package com.stripe.android;

import android.os.Parcel;

import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodTest;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PaymentSessionDataTest {

    private static final PaymentMethod PAYMENT_METHOD =
            PaymentMethod.fromString(PaymentMethodTest.RAW_CARD_JSON);

    @Test
    public void updateIsPaymentReadyToCharge_noShippingRequired() {
        final PaymentSessionConfig config = new PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .setShippingMethodsRequired(false)
                .build();

        final PaymentSessionData data = new PaymentSessionData();
        assertFalse(data.updateIsPaymentReadyToCharge(config));
        assertFalse(data.isPaymentReadyToCharge());

        data.setPaymentMethod(PAYMENT_METHOD);
        assertTrue(data.updateIsPaymentReadyToCharge(config));
        assertTrue(data.isPaymentReadyToCharge());
    }

    @Test
    public void updateIsPaymentReadyToCharge_shippingRequired() {
        final PaymentSessionConfig config = new PaymentSessionConfig.Builder()
                .setShippingInfoRequired(true)
                .setShippingMethodsRequired(true)
                .build();

        final PaymentSessionData data = new PaymentSessionData();
        assertFalse(data.updateIsPaymentReadyToCharge(config));
        assertFalse(data.isPaymentReadyToCharge());

        data.setPaymentMethod(PAYMENT_METHOD);
        assertFalse(data.updateIsPaymentReadyToCharge(config));
        assertFalse(data.isPaymentReadyToCharge());

        data.setShippingInformation(new ShippingInformation());
        assertFalse(data.updateIsPaymentReadyToCharge(config));
        assertFalse(data.isPaymentReadyToCharge());

        data.setShippingMethod(Mockito.mock(ShippingMethod.class));
        assertTrue(data.updateIsPaymentReadyToCharge(config));
        assertTrue(data.isPaymentReadyToCharge());
    }

    @Test
    public void writeToParcel_withNulls_readsFromParcelCorrectly() {
        final PaymentSessionData data = new PaymentSessionData();

        data.setCartTotal(100L);
        data.setShippingTotal(150L);
        data.setPaymentReadyToCharge(false);

        final Parcel parcel = Parcel.obtain();
        data.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        final PaymentSessionData parceledData = PaymentSessionData.CREATOR.createFromParcel(parcel);
        assertEquals(data, parceledData);
    }

    @Test
    public void writeToParcel_withoutNulls_readsFromParcelCorrectly() {
        final PaymentSessionData data = new PaymentSessionData();

        data.setCartTotal(100L);
        data.setShippingTotal(150L);
        data.setPaymentMethod(PAYMENT_METHOD);
        data.setPaymentReadyToCharge(false);
        data.setShippingInformation(new ShippingInformation());
        data.setShippingMethod(new ShippingMethod("UPS", "SuperFast", 10000L, "usd"));

        final Parcel parcel = Parcel.obtain();
        data.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        final PaymentSessionData parceledData = PaymentSessionData.CREATOR.createFromParcel(parcel);
        assertEquals(data, parceledData);
    }
}
