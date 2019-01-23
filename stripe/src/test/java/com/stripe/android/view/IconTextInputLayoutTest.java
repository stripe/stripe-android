package com.stripe.android.view;

import com.google.android.material.textfield.TextInputLayout;
import com.stripe.android.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link IconTextInputLayout} to ensure that the Reflection doesn't break
 * during an upgrade. This class exists only to wrap {@link TextInputLayout}, so there
 * is no need to otherwise test the behavior.
 */
@RunWith(RobolectricTestRunner.class)
public class IconTextInputLayoutTest {

    @Test
    public void init_successfullyFindsFields() {
        ActivityController<CardInputTestActivity> activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start();

        IconTextInputLayout iconTextInputLayout =
                activityController.get().getCardMultilineWidget()
                        .findViewById(R.id.tl_add_source_card_number_ml);

        assertTrue(iconTextInputLayout.hasObtainedCollapsingTextHelper());
    }
}
