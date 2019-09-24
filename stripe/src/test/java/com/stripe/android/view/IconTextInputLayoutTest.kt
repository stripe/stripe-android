package com.stripe.android.view

import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [IconTextInputLayout] to ensure that the Reflection doesn't break
 * during an upgrade. This class exists only to wrap [TextInputLayout], so there
 * is no need to otherwise test the behavior.
 */
@RunWith(RobolectricTestRunner::class)
class IconTextInputLayoutTest :
    BaseViewTest<CardInputTestActivity>(CardInputTestActivity::class.java) {

    @After
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun init_successfullyFindsFields() {
        val iconTextInputLayout = createActivity()
            .cardMultilineWidget
            .findViewById<IconTextInputLayout>(R.id.tl_add_source_card_number_ml)
        assertTrue(iconTextInputLayout.hasObtainedCollapsingTextHelper())
    }
}
