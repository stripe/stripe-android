package com.stripe.android.view;

import com.stripe.android.model.PaymentMethod;
import com.stripe.android.utils.ParcelUtils;

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
}
