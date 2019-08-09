package com.stripe.android.view;

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

    void setCardInputListener(@Nullable CardInputListener listener);

    void clear();
}
