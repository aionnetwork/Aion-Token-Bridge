package org.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.aion.bridge.chain.base.api.StatelessChainConnection;
import org.aion.bridge.chain.base.types.Word32;
import org.aion.bridge.chain.eth.types.EthAddress;
import org.aion.bridge.chain.eth.types.EthBlock;
import org.aion.bridge.chain.eth.types.EthLog;
import org.aion.bridge.chain.eth.types.EthReceipt;
import org.aion.bridge.chain.log.LoggingSetup;
import org.aion.bridge.grpc.enclave.SignedResponse;
import org.aion.bridge.grpc.signatory.SignatoryServiceGrpc;
import org.aion.bridge.grpc.signatory.SignedBundle;
import org.aion.bridge.grpc.signatory.ValidateAndSignRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Controller<T extends StatelessChainConnection<EthBlock, EthReceipt, EthLog, EthAddress>> extends SignatoryServiceGrpc.SignatoryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    private Enclave enclave;
    private BundleValidator<T> bundleValidator;

    public Controller(Enclave enclave, BundleValidator<T> bundleValidator) {
        this.enclave = enclave;
        this.bundleValidator = bundleValidator;
        LoggingSetup.setupLogging();
    }

    /**
     * This is the whole point of this program: function is called to handle a gRPC request
     * for validation & signature of a bundle.
     * <p>
     * This program:
     * <p>
     * 1. Goes to an Ethereum client to retrieve the specified block.
     * 2. Reconstructs the bundle requested by the sent by the server
     * 3. If the bundle hash matches the one provided, it signs the bundle and replies with the signature
     */
    @Override
    public void validateAndSign(ValidateAndSignRequest request, StreamObserver<SignedBundle> responseObserver) {
        try {
            // @implNote: casting the 32-byte types as Word32 guarantees length = 32 bytes (or throws)
            Word32 blockHash = new Word32(request.getSourceChainBlockHash().toByteArray());
            Word32 bundleHash = new Word32(request.getBundleHash().toByteArray());
            long blockNumber = request.getSourceChainBlockNumber();
            int bundleIndex = request.getIndexInSourceChainBlock();

            log.debug("Controller received bundleHash: {}", bundleHash.toString());

            boolean isValid = bundleValidator.validate(blockNumber, blockHash, bundleIndex, bundleHash);

            if (!isValid)
                throw new Exception("Bundle requested not valid");

            // it's finally OK to sign the message
            SignedResponse response;
            try {
                response = enclave.sign(ByteString.copyFrom(bundleHash.payload()));
            } catch (StatusRuntimeException e) {
                LoggingSetup.setupLogging();
                log.error(LoggingSetup.SMTP_MARKER, "Signature from enclave failed: " + e.getMessage());
                throw new Exception("Signature from enclave failed: " + e.getMessage());
            }

            if (response == null)
                throw new IllegalStateException("Response from enclave should not be null");

            log.debug("Enclave successfully signed bundleHash: {}", bundleHash.toString());

            // send the bundle signature back to the client
            SignedBundle signed = SignedBundle.newBuilder().setSignature(response.getSignedData()).build();
            responseObserver.onNext(signed);
            responseObserver.onCompleted();

            // all done.
        } catch (Exception e) {
            LoggingSetup.setupLogging();
            log.debug("Controller caught exception: ", e);
            // catch any runtime exceptions here and translate them off for the client as best as possible.
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
            // gRPC's opinion is that it's better for RPC-type interfaces to deal in Runtime exceptions
        }
    }
}

