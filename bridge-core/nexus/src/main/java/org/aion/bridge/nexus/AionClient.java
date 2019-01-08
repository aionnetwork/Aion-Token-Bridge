/*
 * This code is licensed under the MIT License
 *
 * Copyright (c) 2019 Aion Foundation https://aion.network/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.aion.bridge.nexus;

import com.google.protobuf.ByteString;

import org.aion.bridge.chain.aion.rpc.AionRawTransactionCodec;
import org.aion.bridge.chain.aion.rpc.FvmAbiCodec;
import org.aion.bridge.chain.aion.rpc.abi.FvmAddress;
import org.aion.bridge.chain.aion.rpc.abi.FvmBytes32;
import org.aion.bridge.chain.aion.rpc.abi.FvmList;
import org.aion.bridge.chain.aion.rpc.abi.FvmUint128;
import org.aion.bridge.chain.base.api.ConsolidatedChainConnection;
import org.aion.bridge.chain.base.api.QuorumNotAvailableException;
import org.aion.bridge.chain.base.types.ImmutableBytes;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.base.utility.ByteUtils;
import org.aion.bridge.chain.base.utility.CryptoUtils;
import org.aion.bridge.chain.bridge.AionSubmittedTx;
import org.aion.bridge.chain.bridge.StatefulBundle;
import org.aion.bridge.datastore.AionTipState;
import org.aion.bridge.nexus.retry.Predicates;
import org.aion.bridge.nexus.retry.RetryBuilder;
import org.aion.bridge.nexus.retry.RetryExecutor;
import org.aion.bridge.grpc.enclave.SignedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class AionClient {

    private final Logger log = LoggerFactory.getLogger(AionClient.class);
    private ConsolidatedChainConnection aionApi;
    private Relayer relayer;
    private RetryExecutor<AionSubmittedTx> exec;
    private AionTipState aionTipState;
    private final static int RETRY_SLEEP_TIME = 10000;
    private static final Word32 CONTRACT_ADDRESS = new Word32 ("0000000000000000000000000000000000000000000000000000000000000200");

    @SuppressWarnings("unchecked")
    public AionClient(@Nonnull ConsolidatedChainConnection consolidatedChainConnection,
                      @Nonnull AionTipState tipState,
                      @Nonnull Relayer relayer) {

        this.relayer = relayer;
        this.aionTipState = tipState;
        this.aionApi = consolidatedChainConnection;
        exec = RetryBuilder.newBuilder()
                .retryIf(new Predicates.TxInfoFailPredicate())
                .stopAfterAttempt(3)
                .setSleepTime(RETRY_SLEEP_TIME)
                .build();
    }

    public AionSubmittedTx sendAionTransaction(@Nonnull StatefulBundle bundle) {
        if(bundle.getState() != StatefulBundle.State.SIGNED)
            throw new IllegalStateException("Unable to send a tx which is not within the SIGNED state");

        // Set nonce for this tx and update relayer
        final BigInteger nonce = relayer.getNonce();
        relayer.setNonce(nonce.add(BigInteger.ONE));

        Callable<AionSubmittedTx> sendTransaction = () -> {
            long timestamp = System.currentTimeMillis() * 1000;

            BigInteger nrgPrice = BigInteger.valueOf(10_000_000_000L);
            try {
                nrgPrice = aionApi.getGasPrice();
            } catch (QuorumNotAvailableException e) {
                e.printStackTrace();
                log.debug("getGasPrice failed - Unable to reach gas price consensus, proceeding with default nrg price");
            }

            SendRequest r = new SendRequest(bundle);
            if (r.getAddresses() == null) {
                log.error("Bundle did not contain required information.");
                return null;
            }

            ImmutableBytes txnData = new ImmutableBytes(
                    new FvmAbiCodec("submitBundle(bytes32,bytes32[],address[],uint128[],bytes32[],bytes32[],bytes32[])",
                            new FvmBytes32(bundle.getEthBlockHash()),
                            new FvmList(r.getEthTransactions()),
                            new FvmList(r.getAddresses()),
                            new FvmList(r.getAmounts()),
                            new FvmList(r.getSig_pk()),
                            new FvmList(r.getSig_1()),
                            new FvmList(r.getSig_2())).encode());

            byte[] rlpRaw = AionRawTransactionCodec.getRLPRaw(
                    CONTRACT_ADDRESS,
                    BigInteger.ZERO,
                    txnData,
                    nonce,
                    BigInteger.valueOf(timestamp),
                    1_000_000L,
                    nrgPrice.longValueExact());

            SignedResponse signedTxn = relayer.getEnclaveClient().sign(ByteString.copyFrom(CryptoUtils.blake2b256(rlpRaw)));
            if (signedTxn != null && signedTxn.getSignedData().toByteArray().length > 0) {
                log.info("Signature received for TxnData BundleId: {}, BN: {}, Idx: {}", bundle.getBundleId(), bundle.getEthBlockNumber(), bundle.getIndexInEthBlock());

                ImmutableBytes rlpFinal = new ImmutableBytes(AionRawTransactionCodec.getRLPFinal(
                        CONTRACT_ADDRESS,
                        BigInteger.ZERO,
                        txnData,
                        nonce,
                        BigInteger.valueOf(timestamp),
                        1_000_000L,
                        nrgPrice.longValueExact(),
                        ByteUtils.hexToBin(relayer.getPublicKey()),
                        signedTxn.getSignedData().toByteArray()));

                try {
                    // Submit transaction and update bundle state if successful
                    Word32 destinationTxHash = aionApi.sendRawTransaction(rlpFinal);
                    Optional<Long> destinationBlockNumber = aionTipState.getBlockNumber();

                    if (!destinationBlockNumber.isPresent())
                        return null;

                    return new AionSubmittedTx(relayer.getAccountAddress(), nonce.longValue(), destinationBlockNumber.get(), destinationTxHash);
                } catch (QuorumNotAvailableException e) {
                    e.printStackTrace();
                    log.debug("Unable to reach quorum or api call interrupted, retrying");
                }
            }
            return null;
        };

        return exec.execute(sendTransaction);
    }

    static class SendRequest {
        private FvmAddress[] addresses;
        private FvmUint128[] amounts;
        private FvmBytes32[] ethTransactions;
        private FvmBytes32[] sig_1;
        private FvmBytes32[] sig_2;
        private FvmBytes32[] sig_pk;


        FvmAddress[] getAddresses() {
            return addresses;
        }

        FvmUint128[] getAmounts() {
            return amounts;
        }

        FvmBytes32[] getSig_1() {
            return sig_1;
        }

        FvmBytes32[] getSig_2() {
            return sig_2;
        }

        FvmBytes32[] getSig_pk() {
            return sig_pk;
        }

        FvmBytes32[] getEthTransactions() {
            return ethTransactions;
        }

        SendRequest(StatefulBundle bundle) {

            if(bundle.getTransfers().size() > 0 && bundle.getSignatures().size() > 0) {
                this.addresses = ByteUtils.toArray(bundle.getTransfers()
                        .stream()
                        .map(t -> new FvmAddress(t.getAionAddress()))
                        .collect(Collectors.toList()));

                this.amounts = ByteUtils.toArray(bundle.getTransfers()
                        .stream()
                        .map(t -> new FvmUint128(t.getAionTransferAmount()))
                        .collect(Collectors.toList()));

                this.sig_1 = ByteUtils.toArray(bundle.getSignatures()
                        .stream()
                        .map(sig -> new FvmBytes32(new Word32(Arrays.copyOfRange(sig.getSignature().getByteArray(), 0, 32))))
                        .collect(Collectors.toList()));

                this.sig_2 = ByteUtils.toArray(bundle.getSignatures()
                        .stream()
                        .map(sig -> new FvmBytes32(new Word32(Arrays.copyOfRange(sig.getSignature().getByteArray(), 32, 64))))
                        .collect(Collectors.toList()));

                this.sig_pk = ByteUtils.toArray(bundle.getSignatures()
                        .stream()
                        .map(sig -> new FvmBytes32(sig.getPublicKey()))
                        .collect(Collectors.toList()));

                this.ethTransactions = ByteUtils.toArray(bundle.getTransfers()
                        .stream()
                        .map(t -> new FvmBytes32(t.getEthTxHash()))
                        .collect(Collectors.toList()));
            }
        }
    }
}
