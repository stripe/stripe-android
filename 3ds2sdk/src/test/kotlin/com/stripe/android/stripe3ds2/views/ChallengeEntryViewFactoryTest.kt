package com.stripe.android.stripe3ds2.views

import androidx.test.core.app.ApplicationProvider
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.transactions.UiType
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ChallengeEntryViewFactoryTest {
    private lateinit var factory: ChallengeEntryViewFactory

    @BeforeTest
    fun before() {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
            .create()
            .use {
                it.onActivity { activity ->
                    factory = ChallengeEntryViewFactory(activity)
                }
            }
    }

    @Test
    fun makeChallengeEntryTextView_textEntryViewCreated() {
        val data = ChallengeMessageFixtures.CRES.copy(
            uiType = UiType.Text,
            challengeInfoLabel = "challenge info"
        )
        val view = factory.createChallengeEntryTextView(data, UI_CUSTOMIZATION)
        assertEquals("challenge info", view.infoLabel.hint).toString()
    }

    @Test
    fun makeChallengeEntrySelectView_singleSelectEntryViewCreated() {
        val data = ChallengeMessageFixtures.CRES.copy(
            uiType = UiType.SingleSelect,
            challengeInfoLabel = "challenge info",
            challengeSelectOptions = OPTIONS
        )

        val view = factory.createChallengeEntrySelectView(data, UI_CUSTOMIZATION)
        assertTrue(view.isSingleSelectMode)
        assertEquals("challenge info", view.infoLabel.text.toString())
        assertEquals(2, view.selectGroup.childCount)
    }

    @Test
    fun makeChallengeEntrySelectView_multiSelectEntryViewCreated() {
        val data = ChallengeMessageFixtures.CRES.copy(
            uiType = UiType.MultiSelect,
            challengeInfoLabel = "challenge info",
            challengeSelectOptions = OPTIONS
        )
        val view = factory.createChallengeEntrySelectView(data, UI_CUSTOMIZATION)
        assertFalse(view.isSingleSelectMode)
        assertEquals("challenge info", view.infoLabel.text.toString())
        assertEquals(2, view.checkBoxes?.size)
    }

    @Test
    fun makeChallengeEntryWebView_webViewCreated() {
        val data = ChallengeMessageFixtures.CRES.copy(
            uiType = UiType.Html,
            acsHtml = "<html/>"
        )

        val view = factory.createChallengeEntryWebView(data)
        assertNotNull(view)
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

        private val UI_CUSTOMIZATION = StripeUiCustomization()
    }
}
