package com.stripe.android.stripe3ds2.views

import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.stripe3ds2.R
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ChallengeZoneViewTest {
    private lateinit var challengeZoneView: ChallengeZoneView

    private val onClickListener: View.OnClickListener = mock()

    @BeforeTest
    fun before() {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
            .create()
            .use {
                it.onActivity { activity ->
                    challengeZoneView = ChallengeZoneView(activity)
                }
            }
    }

    @Test
    fun setInfoHeader_noCustomization_infoHeaderTextViewIsSet() {
        challengeZoneView.setInfoHeaderText("HEADER")
        assertEquals("HEADER", challengeZoneView.infoHeader.text)
    }

    @Test
    fun setInfoText_noCustomization_infoTextViewIsSet() {
        challengeZoneView.setInfoText("TEXT")
        assertEquals("TEXT", challengeZoneView.infoTextView.text)
    }

    @Test
    fun setInfoHeader_emptyLabels_infoHeaderTextViewIsHidden() {
        challengeZoneView.setInfoHeaderText(null)
        assertEquals(View.GONE, challengeZoneView.infoHeader.visibility)
    }

    @Test
    fun setInfoText_emptyLabels_infoTextViewIsHidden() {
        challengeZoneView.setInfoText("")
        assertEquals(View.GONE, challengeZoneView.infoTextView.visibility)
    }

    @Test
    fun setInfoTextIndicator_infoTextIndicatorIsSet() {
        challengeZoneView.setInfoTextIndicator(R.drawable.stripe_3ds2_ic_visa)
        val drawables = challengeZoneView.infoTextView.compoundDrawablesRelative
        assertNotNull(drawables[0])
        assertNull(drawables[1])
        assertNull(drawables[2])
        assertNull(drawables[3])
    }

    @Test
    fun setSubmitButton_noCustomization_submitButtonLabelIsSet() {
        challengeZoneView.setSubmitButton("SUBMIT")
        assertEquals("SUBMIT", challengeZoneView.submitButton.text)
    }

    @Test
    fun setSubmitButtonClickListener_verifyResendButtonListenerClicked() {
        challengeZoneView.setSubmitButtonClickListener(onClickListener)
        challengeZoneView.submitButton.performClick()
        verify(onClickListener).onClick(challengeZoneView.submitButton)
    }

    @Test
    fun setSubmitButtonLabel_emptyLabel_submitButtonIsGone() {
        challengeZoneView.setSubmitButton("")
        assertEquals(View.GONE, challengeZoneView.submitButton.visibility)
    }

    @Test
    fun setResendButtonLabel_noCustomization_resendButtonLabelIsSet() {
        challengeZoneView.setResendButtonLabel("RESEND")
        assertEquals("RESEND", challengeZoneView.resendButton.text)
    }

    @Test
    fun setResendButtonLabel_emptyLabel_resendButtonLabelIsGone() {
        challengeZoneView.setResendButtonLabel("")
        assertEquals(View.GONE, challengeZoneView.resendButton.visibility)
    }

    @Test
    fun setResendButtonClickListener_verifyResendButtonListenerClicked() {
        challengeZoneView.setResendButtonClickListener(onClickListener)
        challengeZoneView.resendButton.performClick()
        verify(onClickListener).onClick(challengeZoneView.resendButton)
    }

    @Test
    fun setWhitelistingLabel_whitelistLabelIsSetAndEntryShown() {
        challengeZoneView.setWhitelistingLabel("WHITELIST")
        assertEquals("WHITELIST", challengeZoneView.whitelistingLabel.text)
        assertEquals(View.VISIBLE, challengeZoneView.whitelistingLabel.visibility)
        assertEquals(View.VISIBLE, challengeZoneView.whitelistRadioGroup.visibility)
    }

    @Test
    fun getWhitelistingSelection_yesClicked_returnsTrue() {
        challengeZoneView.whitelistYesRadioButton.performClick()
        assertTrue(challengeZoneView.whitelistingSelection)
    }

    @Test
    fun getWhitelistingSelection_noClicked_returnsFalse() {
        challengeZoneView.whitelistNoRadioButton.performClick()
        assertFalse(challengeZoneView.whitelistingSelection)
    }

    @Test
    fun getWhitelistingSelection_noButtonsClicked_returnsFalse() {
        assertFalse(challengeZoneView.whitelistingSelection)
    }

    @Test
    fun setChallengeEntryView_entryViewIsSet() {
        assertEquals(0, challengeZoneView.challengeEntryView.childCount)
        challengeZoneView
            .setChallengeEntryView(View(ApplicationProvider.getApplicationContext()))
        assertEquals(1, challengeZoneView.challengeEntryView.childCount)
    }
}
