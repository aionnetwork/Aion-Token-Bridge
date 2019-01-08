package org.grpc;

/**
 * @implNote Used for testing, to make sure that error conditions get caught correctly
 */
public class BundleValidatorException extends Exception {
    BundleValidator.Error error;
    public BundleValidatorException(BundleValidator.Error error, String message) {
        super(message);
        this.error = error;
    }
}
