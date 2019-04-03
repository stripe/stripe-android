package com.stripe.android.view.i18n;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeError;
import com.stripe.android.StripeErrorFixtures;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TranslatorManagerTest {
    private static final StripeError STRIPE_ERROR = StripeErrorFixtures.INVALID_REQUEST_ERROR;

    @Before
    public void setup() {
        TranslatorManager.setErrorMessageTranslator(null);
    }

    @Test
    public void testDefaultErrorMessageTranslator() {
        assertEquals("error!",
                TranslatorManager.getErrorMessageTranslator()
                        .translate(0, "error!", STRIPE_ERROR));
    }

    @Test
    public void testCustomErrorMessageTranslator() {
        TranslatorManager.setErrorMessageTranslator(new ErrorMessageTranslator() {
            @NonNull
            @Override
            public String translate(int httpCode, @Nullable String errorMessage,
                                    @Nullable StripeError stripeError) {
                return "custom message";
            }
        });
        assertEquals("custom message", TranslatorManager.getErrorMessageTranslator()
                .translate(0, "original message", STRIPE_ERROR));
    }
}
