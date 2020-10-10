package com.xobotun.vtb;

public class VtbException extends RuntimeException {
    public VtbException() {
    }

    public VtbException(final String message) {
        super(message);
    }

    public VtbException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public VtbException(final Throwable cause) {
        super(cause);
    }

    public VtbException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
