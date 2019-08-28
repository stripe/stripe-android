package com.stripe.android.view;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentMethodCreateParams;

interface CardWidget {
    @Nullable Card getCard();

    @Nullable Card.Builder getCardBuilder();

    /**
     * Gets a {@link PaymentMethodCreateParams.Card} object from the user input, if all fields are
     * valid. If not, returns {@code null}.
     *
     * @return a valid {@link PaymentMethodCreateParams.Card} object based on user input, or
     * {@code null} if any field is invalid
     */
    @Nullable PaymentMethodCreateParams.Card getPaymentMethodCard();

    /**
     * Gets a {@link PaymentMethodCreateParams} object from the user input, if all fields are
     * valid. If not, returns {@code null}.
     *
     * @return a valid {@link PaymentMethodCreateParams} object based on user input, or
     * {@code null} if any field is invalid
     */
    @Nullable PaymentMethodCreateParams getPaymentMethodCreateParams();

    void setCardInputListener(@Nullable CardInputListener listener);

    void setCardHint(@NonNull String cardHint);

    void clear();

    void setCardNumber(@Nullable String cardNumber);

    void setExpiryDate(@IntRange(from = 1, to = 12) int month,
                       @IntRange(from = 0, to = 9999) int year);

    void setCvcCode(@Nullable String cvcCode);
}
