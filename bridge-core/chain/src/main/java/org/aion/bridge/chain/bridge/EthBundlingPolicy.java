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

package org.aion.bridge.chain.bridge;

import org.aion.bridge.chain.aion.types.AionAddress;
import org.aion.bridge.chain.base.BlockProcessor;
import org.aion.bridge.chain.base.BlockProcessorMissingReceiptsException;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.eth.types.*;

import java.math.BigInteger;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Defines the static policy to convert an EthBlock into bundles
 */
public class EthBundlingPolicy {

    // hard coded policy-level constants
    private BigInteger TOKEN_TO_COIN_DECIMAL_SHIFT = BigInteger.TEN.pow(10);
    @SuppressWarnings("FieldCanBeLocal")
    private Integer TRANSFERS_PER_BUNDLE = 20;

    private EthEventFilter ethEventFilter;

    public EthBundlingPolicy(String ethTriggerEvent, String ethContractAddress) {
        ethEventFilter =  new EthEventFilter(new EthAddress(ethContractAddress), ethTriggerEvent, new KeccakBloom());
    }

    public List<EthBundle> fromFilteredBlock(EthBlock block, List<EthReceipt> filtered) {
        List<EthTransfer> transfers = new ArrayList<>();
        for (EthReceipt receipt : filtered) {
            fromReceipt(receipt).ifPresent(transfers::add);
        }

        if (transfers.size() == 0)
            return Collections.emptyList();

        Collections.sort(transfers, new EthTransferSortByIndex());

        // Adding TRANSFERS_PER_BUNDLE-1 to transfers.size() allows rounding up using integer division
        int bundleCount = (transfers.size() + TRANSFERS_PER_BUNDLE - 1) / TRANSFERS_PER_BUNDLE;

        List<EthBundle> bundles = new ArrayList<>();
        for (int i = 0; i < bundleCount; i++) {

            List<EthTransfer> subList;
            int startIndex = i * TRANSFERS_PER_BUNDLE;
            if ((transfers.size() - startIndex) < TRANSFERS_PER_BUNDLE) {
                // if this is the last set of transfers
                subList = transfers.subList(startIndex, transfers.size());
            } else {
                subList = transfers.subList(startIndex, startIndex + TRANSFERS_PER_BUNDLE);
            }

            Map<Word32, EthAddress> txHashToEthAddressMap = subList.stream()
                    .collect(toMap(t -> t.getTransfer().getEthTxHash(), EthTransfer::getEthAddress));

            List<Transfer> transferList = subList.stream().map(EthTransfer::getTransfer).collect(toList());
            Bundle b = new Bundle(block.getNumber(), block.getHash(), i, transferList);

            bundles.add(new EthBundle(b, txHashToEthAddressMap));
        }

        return bundles;
    }

    public List<EthBundle> fromUnfilteredBlock(EthBlock block, List<EthReceipt> receipts)
            throws BlockProcessorMissingReceiptsException {

        // first filter at the block-level
        if (!BlockProcessor.filterBlock(block, ethEventFilter))
            return Collections.emptyList();

        // then filter at the receipt-level
        List<EthReceipt> filtered = BlockProcessor.filterReceipts(block, receipts, ethEventFilter);

        return fromFilteredBlock(block, filtered);
    }

    @SuppressWarnings("WeakerAccess")
    public Optional<EthTransfer> fromReceipt(EthReceipt receipt) {
        for (EthLog l : receipt.getEventLogs()) {
            if (l.getTopics().get(0).equals(ethEventFilter.getEventHash())) {
                // extract amount
                byte[] data = l.getData().getByteArray();

                if (!new BigInteger(data).equals(BigInteger.ZERO)) {

                    byte[] zeros = new byte[16];
                    System.arraycopy(data, 0, zeros, 0, 16);

                    if(!BigInteger.ZERO.equals(new BigInteger(1, zeros)))
                        throw new IllegalStateException("Top 16 bytes of a transfer non-zero value. TxHash: " + receipt.getTransactionHash() + "BlockHash: " + receipt.getBlockHash());

                    byte[] amountEth = new byte[16];
                    System.arraycopy(data, 16, amountEth, 0, 16);
                    BigInteger amountAion = new BigInteger(1, amountEth).multiply(TOKEN_TO_COIN_DECIMAL_SHIFT);

                    // extract info
                    AionAddress aionAddress = new AionAddress(l.getTopics().get(2));
                    Word32 ethTxHash = receipt.getTransactionHash();
                    EthAddress ethAddress = receipt.getFrom();

                    return Optional.of(new EthTransfer(new Transfer(ethTxHash, aionAddress, amountAion), ethAddress, receipt.getTransactionIndex()));
                }
            }
        }
        return Optional.empty();
    }
}
