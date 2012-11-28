package com.stripe;

import android.content.Context;
import android.content.res.Resources;
import com.stripe.util.TextUtils;
import org.json.JSONObject;

public class StripeError {
    /*
     * These keys can be defined in your Android res/values/strings.xml file as per the example application
     */
    public static final String MessageKeyInvalidNumber = "incorrect_number_message";
    public static final String MessageKeyInvalidCVC = "incorrect_cvc_message";
    public static final String MessageKeyInvalidExpMonth = "invalid_expiry_month_message";
    public static final String MessageKeyInvalidExpYear = "invalid_expiry_year_message";
    public static final String MessageKeyExpiredCard = "expired_card_message";
    public static final String MessageKeyDeclined = "card_declined_message";
    public static final String MessageKeyProcessingError = "processing_error_message";
    public static final String MessageKeyUnexpectedError = "unexpected_error_message";
    public static final String MessageKeyInvalidRequest = "invalid_request_error_message";
    public static final String MessageKeyUnauthorized = "unauthorized_request_error_message";
    public static final String MessageKeyMissingNumber = "missing_number_message";
    public static final String MessageKeyMissingCVC = "missing_cvc_message";
    public static final String MessageKeyMissingExpMonth = "missing_expiry_month_message";
    public static final String MessageKeyMissingExpYear = "missing_expiry_year_message";

    private static final String DeveloperMessageInvalidNumber = "Card number must be between 10 and 19 digits long and Luhn valid.";
    private static final String DeveloperMessageInvalidExpYear = "Card expYear must be this year or a year in the future";
    private static final String DeveloperMessageInvalidExpMonth = "Card expMonth must be less than 13";
    private static final String DeveloperMessageInvalidCVC = "Card CVC must be numeric, 3 digits for Visa, Discover, MasterCard, JCB, and Discover cards, and 4 digits for American Express cards.";
    private static final String DeveloperMessageUnknownError = "Could not interpret the response that was returned from Stripe.";
    private static final String DeveloperMessageUnauthorized = "Received 401 Unauthorized from Stripe, please ensure you are using the correct publishable key.";

    protected final static StripeError INVALID_NUMBER = new StripeError(
            StripeErrorCode.CardError,
            MessageKeyInvalidNumber,
            DeveloperMessageInvalidNumber,
            CardErrorCode.InvalidNumber,
            "number");

    protected final static StripeError INVALID_EXP_YEAR = new StripeError(
            StripeErrorCode.CardError,
            MessageKeyInvalidExpYear,
            DeveloperMessageInvalidExpYear,
            CardErrorCode.InvalidExpYear,
            "expYear");

    protected final static StripeError INVALID_EXP_MONTH = new StripeError(
            StripeErrorCode.CardError,
            MessageKeyInvalidExpMonth,
            DeveloperMessageInvalidExpMonth,
            CardErrorCode.InvalidExpMonth,
            "expMonth");

    protected final static StripeError INVALID_CVC = new StripeError(
            StripeErrorCode.CardError,
            MessageKeyInvalidCVC,
            DeveloperMessageInvalidCVC,
            CardErrorCode.InvalidCVC,
            "cvc");

    protected final static StripeError UNAUTHORIZED = new StripeError(
            StripeErrorCode.Unauthorized,
            StripeError.MessageKeyUnauthorized,
            DeveloperMessageUnauthorized,
            null, null);

    protected final static StripeError UNKNOWN_ERROR = new StripeError(
            StripeError.StripeErrorCode.UnexpectedError,
            StripeError.MessageKeyUnexpectedError,
            DeveloperMessageUnknownError,
            null, null);

    public enum CardErrorCode {
        InvalidExpYear,
        InvalidExpMonth,
        InvalidNumber,
        ExpiredCard,
        CardDeclined,
        ProcessingError,
        InvalidCVC,
        UnexpectedError
    }

    public enum StripeErrorCode {
        APIError,
        CardError,
        InvalidRequestError,
        Unauthorized,
        UnexpectedError
    }

    public final StripeErrorCode errorCode;
    protected final String errorMessageKey;
    public final String developerMessage;
    public final CardErrorCode cardErrorCode;
    public final String parameter;

    protected static StripeError fromException(Exception e) {
        return new StripeError(
                StripeError.StripeErrorCode.UnexpectedError,
                StripeError.MessageKeyUnexpectedError,
                e.getMessage() == null ? DeveloperMessageUnknownError : e.getMessage(),
                null, null);
    }

    protected static StripeError fromJSON(JSONObject errorMap) {
        JSONObject errorStruct = errorMap.optJSONObject("error");
        if (errorStruct == null) {
            return UNKNOWN_ERROR;
        }

        String errorType = errorStruct.optString("type", null);
        String devMessage = errorStruct.optString("message", null);
        String cardError = errorStruct.optString("code", null);

        String param = TextUtils.toCamelCase(errorStruct.optString("param", null));

        if (errorType == null || devMessage == null) {
            return UNKNOWN_ERROR;
        }

        StripeErrorCode stripeErrorCode = null;
        CardErrorCode cardErrorCode = null;
        String messageKey = null;
        if ("api_error".equals(errorType)) {
            stripeErrorCode = StripeErrorCode.APIError;
            messageKey = MessageKeyUnexpectedError;
        } else if ("invalid_request_error".equals(errorType)) {
            stripeErrorCode = StripeErrorCode.InvalidRequestError;
            messageKey = MessageKeyInvalidRequest;
        } else if ("card_error".equals(errorType)) {
            stripeErrorCode = StripeErrorCode.CardError;
            if ("incorrect_number".equals(cardError) || "invalid_number".equals(cardError)) {
                cardErrorCode = CardErrorCode.InvalidNumber;
                messageKey = MessageKeyInvalidNumber;
            } else if ("incorrect_cvc".equals(cardError) || "invalid_cvc".equals(cardError)) {
                cardErrorCode = CardErrorCode.InvalidCVC;
                messageKey = MessageKeyInvalidCVC;
            } else if ("invalid_expiry_month".equals(cardError)) {
                cardErrorCode = CardErrorCode.InvalidExpMonth;
                messageKey = MessageKeyInvalidExpMonth;
            } else if ("invalid_expiry_year".equals(cardError)) {
                cardErrorCode = CardErrorCode.InvalidExpYear;
                messageKey = MessageKeyInvalidExpYear;
            } else if ("expired_card".equals(cardError)) {
                cardErrorCode = CardErrorCode.ExpiredCard;
                messageKey = MessageKeyExpiredCard;
            } else if ("card_declined".equals(cardError)) {
                cardErrorCode = CardErrorCode.CardDeclined;
                messageKey = MessageKeyDeclined;
            } else if ("processing_error".equals(cardError)) {
                cardErrorCode = CardErrorCode.ProcessingError;
                messageKey = MessageKeyProcessingError;
            } else if (cardError == null && "number".equals(param)) {
                cardErrorCode = CardErrorCode.InvalidNumber;
                messageKey = MessageKeyMissingNumber;
            } else if (cardError == null && "expYear".equals(param)) {
                cardErrorCode = CardErrorCode.InvalidExpYear;
                messageKey = MessageKeyMissingExpYear;
            } else if (cardError == null && "expMonth".equals(param)) {
                cardErrorCode = CardErrorCode.InvalidExpMonth;
                messageKey = MessageKeyMissingExpMonth;
            } else if (cardError == null && "cvc".equals(param)) {
                cardErrorCode = CardErrorCode.InvalidCVC;
                messageKey = MessageKeyMissingCVC;
            } else {
                cardErrorCode = CardErrorCode.UnexpectedError;
                messageKey = MessageKeyUnexpectedError;
            }
        }
        return new StripeError(stripeErrorCode, messageKey, devMessage, cardErrorCode, param);
    }

    protected StripeError(StripeErrorCode errorCode, String errorMessageKey, String developerMessage, CardErrorCode cardErrorCode, String parameter) {
        this.errorCode = errorCode;
        this.errorMessageKey = errorMessageKey;
        this.developerMessage = developerMessage;
        this.cardErrorCode = cardErrorCode;
        this.parameter = parameter;
    }

    public String getLocalizedString(Context context, Object... formatArgs) throws Resources.NotFoundException {
        Resources resources = context.getResources();
        int id = resources.getIdentifier(errorMessageKey, "string", context.getPackageName());
        return resources.getString(id, formatArgs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StripeError that = (StripeError) o;

        if (cardErrorCode != that.cardErrorCode) return false;
        if (developerMessage != null ? !developerMessage.equals(that.developerMessage) : that.developerMessage != null)
            return false;
        if (errorCode != that.errorCode) return false;
        if (errorMessageKey != null ? !errorMessageKey.equals(that.errorMessageKey) : that.errorMessageKey != null)
            return false;
        if (parameter != null ? !parameter.equals(that.parameter) : that.parameter != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = errorCode != null ? errorCode.hashCode() : 0;
        result = 31 * result + (errorMessageKey != null ? errorMessageKey.hashCode() : 0);
        result = 31 * result + (developerMessage != null ? developerMessage.hashCode() : 0);
        result = 31 * result + (cardErrorCode != null ? cardErrorCode.hashCode() : 0);
        result = 31 * result + (parameter != null ? parameter.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StripeError{" +
                "errorCode=" + errorCode +
                ", errorMessageKey='" + errorMessageKey + '\'' +
                ", developerMessage='" + developerMessage + '\'' +
                ", cardErrorCode=" + cardErrorCode +
                ", parameter='" + parameter + '\'' +
                '}';
    }

}
