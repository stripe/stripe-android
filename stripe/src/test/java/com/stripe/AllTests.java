package com.stripe;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CardTest.class,
        StripeErrorTest.class,
        StripeTest.class
        })
public class AllTests {}