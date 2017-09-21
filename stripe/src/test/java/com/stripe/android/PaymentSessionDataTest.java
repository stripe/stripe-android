package com.stripe.android;

import android.os.Parcel;

import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PaymentSessionDataTest {

    @Test
    public void writeToParcel_withNulls_readsFromParcelCorrectly() {
        PaymentSessionData data = new PaymentSessionData();

        data.setCartTotal(100L);
        data.setShippingTotal(150L);
        data.setSelectedPaymentMethodId("abc123");
        data.setPaymentReadyToCharge(false);

        Parcel parcel = Parcel.obtain();
        data.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PaymentSessionData parceledData =
                PaymentSessionData.CREATOR.createFromParcel(parcel);
        assertEquals(data, parceledData);
    }

    @Test
    public void writeToParcel_withoutNulls_readsFromParcelCorrectly() {
        PaymentSessionData data = new PaymentSessionData();

        data.setCartTotal(100L);
        data.setShippingTotal(150L);
        data.setSelectedPaymentMethodId("abc123");
        data.setPaymentReadyToCharge(false);
        data.setShippingInformation(new ShippingInformation());
        data.setShippingMethod(new ShippingMethod("UPS", "SuperFast", 10000L, "usd"));

        Parcel parcel = Parcel.obtain();
        data.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PaymentSessionData parceledData =
                PaymentSessionData.CREATOR.createFromParcel(parcel);
        assertEquals(data, parceledData);
    }
}
