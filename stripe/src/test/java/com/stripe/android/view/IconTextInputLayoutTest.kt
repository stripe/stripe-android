package com.stripe.android.view

import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.R
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [IconTextInputLayout] to ensure that the Reflection doesn't break
 * during an upgrade. This class exists only to wrap [TextInputLayout], so there
 * is no need to otherwise test the behavior.
 */
@RunWith(RobolectricTestRunner::class)
internal class IconTextInputLayoutTest :
    BaseViewTest<CardInputTestActivity>(CardInputTestActivity::class.java) {

    @AfterTest
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun init_successfullyFindsFields() {
        val iconTextInputLayout = createActivity()
            .cardMultilineWidget
            .findViewById<IconTextInputLayout>(R.id.tl_card_number)
        assertTrue(iconTextInputLayout.hasObtainedCollapsingTextHelper())
    }
}
