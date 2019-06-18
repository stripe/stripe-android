package com.stripe.android;

import android.os.Parcel;

import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingInformation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PaymentSessionConfigTest {

    @Test
    public void testParcel() {
        final PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setHiddenShippingInfoFields("field1", "field2")
                .setOptionalShippingInfoFields("field3", "field4")
                .setPrepopulatedShippingInfo(new ShippingInformation(
                        new Address.Builder().build(), null, null))
                .setShippingInfoRequired(true)
                .setShippingMethodsRequired(true)
                .build();

        final Parcel parcel = Parcel.obtain();
        paymentSessionConfig.writeToParcel(parcel, paymentSessionConfig.describeContents());
        parcel.setDataPosition(0);

        assertEquals(paymentSessionConfig, PaymentSessionConfig.CREATOR.createFromParcel(parcel));
    }
}