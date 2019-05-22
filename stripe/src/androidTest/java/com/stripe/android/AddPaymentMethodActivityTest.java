package com.stripe.android;

import android.support.test.runner.AndroidJUnit4;

import com.stripe.android.view.AddPaymentMethodActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class AddPaymentMethodActivityTest {

    @Rule
    public ActivityTestRule<AddPaymentMethodActivity> mActivityRule =
            new ActivityTestRule(AddPaymentMethodActivity.class);

    @Test
    public void titleRenders() {
        onView(withText(R.string.title_add_a_card)).check(matches(isDisplayed()));
    }

}
