package com.stripe.android.stripe3ds2.views

import android.app.Activity
import android.graphics.Color
import android.view.View.NO_ID
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.stripe3ds2.init.ui.ButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ChallengeZoneSelectViewTest {
    private val activityScenarioFactory = ActivityScenarioFactory(
        ApplicationProvider.getApplicationContext()
    )

    private val selectButtonCustomization: ButtonCustomization =
        StripeButtonCustomization().apply {
            setBackgroundColor("#FF0000")
            setTextColor("#0000FF")
        }

    @Test
    fun constructor_singleSelectModeSet() {
        createSelectViews { singleSelect, multiSelect ->
            assertTrue(singleSelect.isSingleSelectMode)
            assertFalse(multiSelect.isSingleSelectMode)
            assertTrue(singleSelect.selectGroup is RadioGroup)
        }
    }

    @Test
    fun setTextEntryLabel_labelHasBeenSet() {
        createSelectViews { singleSelect, _ ->
            singleSelect.setTextEntryLabel("LABEL", null)
            assertEquals("LABEL", singleSelect.infoLabel.text)
        }
    }

    @Test
    fun setChallengeSelectOptions_optionsAreSet() {
        createSelectViews { singleSelect, multiSelect ->
            singleSelect.setChallengeSelectOptions(OPTIONS, null)
            multiSelect.setChallengeSelectOptions(OPTIONS, null)

            assertEquals(2, singleSelect.selectGroup.childCount)
            assertEquals(2, multiSelect.selectGroup.childCount)
        }
    }

    @Test
    fun buildButton_singleSelect_correctButtonTypeBuiltWithAttributes() {
        createSelectViews { singleSelect, _ ->
            val option = OPTIONS[0]

            val button = singleSelect
                .buildButton(option, selectButtonCustomization, false)

            assertTrue(button is RadioButton)
            assertEquals(option, button.tag)
            assertEquals(option.text, button.text)
            assertNotEquals(NO_ID, button.id)
            assertEquals(
                Color.parseColor("#0000FF"),
                button.textColors.defaultColor
            )
            assertEquals(
                Color.parseColor("#FF0000"),
                button.buttonTintList?.defaultColor
            )
        }
    }

    @Test
    fun buildButton_correctBottomMargin() {
        createSelectViews { singleSelect, _ ->
            val option = OPTIONS[0]

            val firstButton = singleSelect.buildButton(
                option, null, false
            )
            val notLastLayoutParams =
                firstButton.layoutParams as LinearLayout.LayoutParams
            assertTrue(notLastLayoutParams.bottomMargin > 0)

            val lastButton = singleSelect
                .buildButton(option, null, true)
            val lastLayoutParams =
                lastButton.layoutParams as LinearLayout.LayoutParams
            assertEquals(0, lastLayoutParams.bottomMargin)
        }
    }

    @Test
    fun buildButton_multiSelect_correctButtonTypeBuiltWithAttributes() {
        createSelectViews { _, multiSelect ->
            val option = OPTIONS[0]

            val button = multiSelect
                .buildButton(option, selectButtonCustomization, false)

            assertTrue(button is CheckBox)
            assertEquals(option, button.tag)
            assertEquals(option.text, button.text)
            assertNotEquals(NO_ID, button.id)
            assertEquals(
                Color.parseColor("#0000FF"),
                button.textColors.defaultColor
            )
            assertEquals(
                Color.parseColor("#FF0000"),
                button.buttonTintList?.defaultColor
            )
        }
    }

    @Test
    fun selectOption_singleSelect_optionIsSelected() {
        createSelectViews { singleSelect, _ ->
            singleSelect.setChallengeSelectOptions(OPTIONS, null)

            assertEquals(0, singleSelect.selectedOptions.size)
            assertEquals(0, singleSelect.selectedIndexes.size)

            singleSelect.selectOption(0)
            assertEquals(1, singleSelect.selectedOptions.size)
            assertEquals(1, singleSelect.selectedIndexes.size)
            assertEquals(OPTIONS[0], singleSelect.selectedOptions[0])
            assertEquals(0, singleSelect.selectedIndexes[0])

            singleSelect.selectOption(1)
            assertEquals(1, singleSelect.selectedOptions.size)
            assertEquals(1, singleSelect.selectedIndexes.size)
            assertEquals(OPTIONS[1], singleSelect.selectedOptions[0])
            assertEquals(1, singleSelect.selectedIndexes[0])
        }
    }

    @Test
    fun selectOption_multiSelect_optionIsSelected() {
        createSelectViews { _, multiSelect ->
            multiSelect.setChallengeSelectOptions(OPTIONS, null)

            assertEquals(0, multiSelect.selectedOptions.size)
            assertEquals(0, multiSelect.selectedIndexes.size)

            multiSelect.selectOption(0)
            assertEquals(1, multiSelect.selectedOptions.size)
            assertEquals(1, multiSelect.selectedIndexes.size)
            assertEquals(OPTIONS[0], multiSelect.selectedOptions[0])
            assertEquals(0, multiSelect.selectedIndexes[0])

            multiSelect.selectOption(1)
            assertEquals(2, multiSelect.selectedOptions.size)
            assertEquals(2, multiSelect.selectedIndexes.size)
            assertEquals(OPTIONS[0], multiSelect.selectedOptions[0])
            assertEquals(0, multiSelect.selectedIndexes[0])
            assertEquals(OPTIONS[1], multiSelect.selectedOptions[1])
            assertEquals(1, multiSelect.selectedIndexes[1])
        }
    }

    @Test
    fun getCheckBoxes_checkBoxesAreReturnedForMultiSelect() {
        createSelectViews { singleSelect, multiSelect ->
            singleSelect.setChallengeSelectOptions(OPTIONS, null)
            multiSelect.setChallengeSelectOptions(OPTIONS, null)

            assertNull(singleSelect.checkBoxes)

            assertEquals(2, multiSelect.checkBoxes?.size)
        }
    }

    private fun createSelectViews(
        callback: (
            singleSelect: ChallengeZoneSelectView,
            multiSelect: ChallengeZoneSelectView
        ) -> Unit
    ) {
        createActivity {
            callback(
                ChallengeZoneSelectView(it, isSingleSelectMode = true),
                ChallengeZoneSelectView(it, isSingleSelectMode = false)
            )
        }
    }

    private fun createActivity(callback: (Activity) -> Unit) {
        activityScenarioFactory.create().use {
            it.onActivity(callback)
        }
    }

    private companion object {
        private val OPTIONS = listOf(
            ChallengeResponseData.ChallengeSelectOption(
                "phone",
                "Mobile **** **** 321"
            ),
            ChallengeResponseData.ChallengeSelectOption(
                "email",
                "Email a*******g**@g***.com"
            )
        )
    }
}
