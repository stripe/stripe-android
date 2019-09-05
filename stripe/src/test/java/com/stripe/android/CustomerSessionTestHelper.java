package com.stripe.android;

import androidx.annotation.NonNull;

public class CustomerSessionTestHelper {
    private CustomerSessionTestHelper() {
    }

    public static void setInstance(@NonNull CustomerSession customerSession) {
        CustomerSession.setInstance(customerSession);
    }
}
