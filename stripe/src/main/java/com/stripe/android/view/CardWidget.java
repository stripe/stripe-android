package com.stripe.android.view;

import android.support.annotation.Nullable;

import com.stripe.android.model.Card;

interface CardWidget {
    @Nullable Card getCard();

    @Nullable Card.Builder getCardBuilder();
}
