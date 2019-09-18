package com.stripe.android.view;

import android.content.Intent;
import android.os.Bundle;

import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodFixtures;
import com.stripe.android.utils.ParcelUtils;

import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AddPaymentMethodActivityStarterTest {

    @Test
    public void testParceling() {
        final AddPaymentMethodActivityStarter.Args args =
                new AddPaymentMethodActivityStarter.Args.Builder()
                        .setPaymentMethodType(PaymentMethod.Type.Fpx)
                        .setIsPaymentSessionActive(true)
                        .setShouldRequirePostalCode(true)
                        .build();

        final AddPaymentMethodActivityStarter.Args createdArgs =
                ParcelUtils.create(args, AddPaymentMethodActivityStarter.Args.CREATOR);
        assertEquals(args, createdArgs);
    }

    @Test
    public void testResultParceling() {
        final Bundle bundle =
                new AddPaymentMethodActivityStarter
                        .Result(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                        .toBundle();
        final Intent intent = new Intent().putExtras(bundle);
        final AddPaymentMethodActivityStarter.Result result =
                Objects.requireNonNull(AddPaymentMethodActivityStarter.Result.fromIntent(intent));
        assertEquals(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                result.paymentMethod
        );
    }
}
