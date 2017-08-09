package com.stripe.android.view;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents a listener for card input events. Note that events are
 * not one-time events. For instance, a user can "complete" the CVC many times
 * by deleting and re-entering the value.
 */
public interface CardInputListener {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            FocusField.FOCUS_CARD,
            FocusField.FOCUS_EXPIRY,
            FocusField.FOCUS_CVC,
            FocusField.FOCUS_POSTAL
    })
    @interface FocusField {
        String FOCUS_CARD = "focus_card";
        String FOCUS_EXPIRY = "focus_expiry";
        String FOCUS_CVC = "focus_cvc";
        String FOCUS_POSTAL = "focus_postal";
    }

    /**
     * Called whenever the field of focus within the widget changes.
     *
     * @param focusField a {@link FocusField} to which the focus has just changed.
     */
    void onFocusChange(@FocusField String focusField);

    /**
     * Called when a potentially valid card number has been completed in the
     * {@link CardNumberEditText}. May be called multiple times if the user edits
     * the field.
     */
    void onCardComplete();

    /**
     * Called when a expiration date (one that has not yet passed) has been entered.
     * May be called multiple times, if the user edits the date.
     */
    void onExpirationComplete();

    /**
     * Called when a potentially valid CVC has been entered. The only verification performed
     * on the number is that it is the correct length. May be called multiple times, if
     * the user edits the CVC.
     */
    void onCvcComplete();

    /**
     * Called when a potentially valid postal code or zip code has been entered.
     * May be called multiple times.
     */
    void onPostalCodeComplete();
}
