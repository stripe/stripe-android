package com.stripe.android.exception;

/**
 * A base class for Stripe-related {@link Exception Exceptions}.
 */
public abstract class StripeException extends Exception {

    protected static final long serialVersionUID = 1L;

    private String requestId;
    private Integer statusCode;

    public StripeException(String message, String requestId, Integer statusCode) {
        super(message, null);
        this.requestId = requestId;
        this.statusCode = statusCode;
    }

    public StripeException(String message, String requestId, Integer statusCode, Throwable e) {
        super(message, e);
        this.statusCode = statusCode;
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String toString() {
        String reqIdStr = "";
        if (requestId != null) {
            reqIdStr = "; request-id: " + requestId;
        }
        return super.toString() + reqIdStr;
    }
}

