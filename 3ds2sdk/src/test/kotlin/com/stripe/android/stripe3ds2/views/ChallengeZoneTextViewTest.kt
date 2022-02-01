package com.stripe.android.stripe3ds2.views

import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ChallengeZoneTextViewTest {
    private lateinit var challengeZoneTextView: ChallengeZoneTextView

    @BeforeTest
    fun before() {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
            .create()
            .use {
                it.onActivity { activity ->
                    challengeZoneTextView = ChallengeZoneTextView(activity)
                }
            }
    }

    @Test
    fun setTextEntryLabel_labelHasBeenSet() {
        challengeZoneTextView.setTextEntryLabel("LABEL")
        assertEquals("LABEL", challengeZoneTextView.infoLabel.hint)
    }

    @Test
    fun setAndGetTextEntry_entryTextIsSet() {
        challengeZoneTextView.setText("ENTRY")
        assertEquals("ENTRY", challengeZoneTextView.textEntryView.text.toString())
        assertEquals("ENTRY", challengeZoneTextView.userEntry)
    }
}
