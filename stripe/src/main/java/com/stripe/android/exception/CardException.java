package com.stripe.android.exception;

/**
 * An {@link Exception} indicating that there is a problem with a Card used for a request.
 * Card errors are the most common type of error you should expect to handle.
 * They result when the user enters a card that can't be charged for some reason.
 */
public class CardException extends StripeException {

    private String code;
    private String param;
    private String declineCode;
    private String charge;

    public CardException(String message, String requestId, String code, String param,
                         String declineCode, String charge, Integer statusCode, Throwable e) {
        super(message, requestId, statusCode, e);
        this.code = code;
        this.param = param;
        this.declineCode = declineCode;
        this.charge = charge;
    }

    public String getCode() {
        return code;
    }

    public String getParam() {
        return param;
    }

    public String getDeclineCode() {
        return declineCode;
    }

    public String getCharge() {
        return charge;
    }
}
