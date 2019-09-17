package com.stripe.android.view;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodFixtures;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class CardDisplayTextFactoryTest {

    @Test
    public void createUnstyled_withVisa() {
        final Context context = ApplicationProvider.getApplicationContext();
        final CardDisplayTextFactory cardDisplayTextFactory = new CardDisplayTextFactory(
                context.getResources(),
                new ThemeConfig(context)
        );
        assertEquals(
                "Visa ending in 4242",
                cardDisplayTextFactory.createUnstyled(PaymentMethodFixtures.CARD)
        );
    }

    @Test
    public void createUnstyled_withUnknown() {
        final Context context = ApplicationProvider.getApplicationContext();
        final CardDisplayTextFactory cardDisplayTextFactory = new CardDisplayTextFactory(
                context.getResources(),
                new ThemeConfig(context)
        );
        assertEquals(
                "Unknown ending in 4242",
                cardDisplayTextFactory.createUnstyled(
                        new PaymentMethod.Card.Builder()
                                .setLast4("4242")
                                .build()
                )
        );
    }

    @Test
    public void createStyled_withVisaWithoutLast4() {
        final Context context = ApplicationProvider.getApplicationContext();
        final CardDisplayTextFactory cardDisplayTextFactory = new CardDisplayTextFactory(
                context.getResources(),
                new ThemeConfig(context)
        );
        assertEquals(
                "Visa",
                cardDisplayTextFactory.createStyled(
                        PaymentMethod.Card.Brand.VISA,
                        null,
                        false
                ).toString()
        );
    }
}
