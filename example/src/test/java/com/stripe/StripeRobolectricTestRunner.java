package com.stripe;

import java.io.File;

import org.junit.runners.model.InitializationError;

import com.xtremelabs.robolectric.RobolectricConfig;
import com.xtremelabs.robolectric.RobolectricTestRunner;

public class StripeRobolectricTestRunner extends RobolectricTestRunner {

    public StripeRobolectricTestRunner(@SuppressWarnings("rawtypes") Class testClass) throws InitializationError {
        super(testClass, new RobolectricConfig(new File("example")));
    }

}
