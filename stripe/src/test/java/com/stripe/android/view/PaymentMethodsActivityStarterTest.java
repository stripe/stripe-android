package com.stripe.android.view;

import com.stripe.android.model.PaymentMethod;
import com.stripe.android.utils.ParcelUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodsActivityStarterTest {

    @Test
    public void testParceling() {
        final Set<PaymentMethod.Type> paymentMethodTypes = new HashSet<>(
                Arrays.asList(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx)
        );
        final PaymentMethodsActivityStarter.Args args =
                new PaymentMethodsActivityStarter.Args.Builder()
                        .setInitialPaymentMethodId("pm_12345")
                        .setIsPaymentSessionActive(true)
                        .setShouldRequirePostalCode(true)
                        .setPaymentMethodTypes(paymentMethodTypes)
                        .build();

        final PaymentMethodsActivityStarter.Args createdArgs =
                ParcelUtils.create(args, PaymentMethodsActivityStarter.Args.CREATOR);
        assertEquals(args, createdArgs);
    }

    @Test
    public void testDefaultPaymentMethodTypes_isCard() {
        assertEquals(
                Collections.singleton(PaymentMethod.Type.Card),
                new PaymentMethodsActivityStarter.Args.Builder()
                        .build()
                        .paymentMethodTypes
        );
    }
}