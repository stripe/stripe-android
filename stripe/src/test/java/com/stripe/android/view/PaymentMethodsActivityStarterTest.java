package com.stripe.android.view;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.ApiKeyFixtures;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.utils.ParcelUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodsActivityStarterTest {

    @Test
    public void testParceling() {
        final Context context = ApplicationProvider.getApplicationContext();
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);

        final Set<PaymentMethod.Type> paymentMethodTypes = new HashSet<>(
                Arrays.asList(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx)
        );
        final PaymentMethodsActivityStarter.Args args =
                new PaymentMethodsActivityStarter.Args.Builder()
                        .setInitialPaymentMethodId("pm_12345")
                        .setIsPaymentSessionActive(true)
                        .setShouldRequirePostalCode(true)
                        .setPaymentMethodTypes(paymentMethodTypes)
                        .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
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