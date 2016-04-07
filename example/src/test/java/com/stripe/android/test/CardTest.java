package com.stripe.android.test;

import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.stripe.android.model.Card;
import com.stripe.android.time.FrozenClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class CardTest {
    private static final int YEAR_IN_FUTURE = 2000;

    @Before
    public void setup() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1997);
        cal.set(Calendar.MONTH, Calendar.AUGUST);
        cal.set(Calendar.DAY_OF_MONTH, 29);
        FrozenClock.freeze(cal);
    }

    @After
    public void teardown() {
       FrozenClock.unfreeze();
    }

}