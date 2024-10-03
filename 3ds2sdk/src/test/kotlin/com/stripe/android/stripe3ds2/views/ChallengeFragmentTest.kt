package com.stripe.android.stripe3ds2.views

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.R
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.observability.FakeErrorReporter
import com.stripe.android.stripe3ds2.transaction.ChallengeAction
import com.stripe.android.stripe3ds2.transaction.ChallengeActionHandler
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestExecutorFixtures
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestResult
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.FakeTransactionTimer
import com.stripe.android.stripe3ds2.transaction.IntentDataFixtures
import com.stripe.android.stripe3ds2.transaction.TransactionTimer
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.transactions.UiType
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ChallengeFragmentTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val transactionTimer: TransactionTimer = FakeTransactionTimer(false)
    private val errorReporter: ErrorReporter = FakeErrorReporter()
    private val challengeActionHandler = FakeChallengeActionHandler()

    @Test
    fun `fragment should be created successfully`() {
        createFragment { fragment ->
            assertThat(fragment.isResumed)
                .isTrue()

            val challengeZone = fragment.viewBinding.caChallengeZone
            assertThat(challengeZone.challengeEntryView.children.first() is ChallengeZoneTextView)
                .isTrue()
        }
    }

    @Test
    fun `submit button click with incomplete response should show next screen`() {
        createFragment { fragment ->
            val screens = mutableListOf<ChallengeResponseData>()
            fragment.viewModel.nextScreen.observeForever {
                screens.add(it)
            }
            assertThat(screens)
                .isEmpty()

            val nextCres = ChallengeMessageFixtures.CRES.copy(
                shouldShowChallengeInfoTextIndicator = true
            )
            challengeActionHandler.result = ChallengeRequestResult.Success(
                ChallengeMessageFixtures.CREQ,
                nextCres,
                ChallengeRequestExecutorFixtures.CONFIG
            )

            fragment.viewModel.updateChallengeText("123456")
            fragment.clickSubmitButton()

            assertThat(screens)
                .containsExactly(nextCres)
        }
    }

    @Test
    fun `submit button click with valid user entry should submit form`() {
        createFragment {
            it.viewModel.updateChallengeText("123456")
            it.clickSubmitButton()

            assertThat(challengeActionHandler.actions)
                .containsExactly(
                    ChallengeAction.NativeForm("123456", whitelistingValue = false)
                )
        }
    }

    @Test
    fun updateChallengeText_shouldSetTextOnChallengeEditText() {
        createFragment {
            it.viewModel.updateChallengeText("my entry")
            assertThat(it.userEntry)
                .isEqualTo("my entry")
        }
    }

    @Test
    fun configureChallengeZoneView_challengeZoneIsConfigured() {
        createFragment(
            cres = CRES_TEXT_DATA
        ) { fragment ->
            val challengeZoneView = fragment.viewBinding.caChallengeZone
            assertEquals(
                HEADER_TEXT,
                challengeZoneView.infoHeader.text
            )
            assertEquals(
                INFO_TEXT,
                challengeZoneView.infoTextView.text
            )
            assertNotNull(
                challengeZoneView.infoTextView.compoundDrawablesRelative.first()
            )
            assertEquals(
                WHITELIST_TEXT,
                challengeZoneView.whitelistingLabel.text
            )
            assertTrue(challengeZoneView.submitButton.hasOnClickListeners())
            assertTrue(challengeZoneView.resendButton.hasOnClickListeners())
        }
    }

    @Test
    fun setChallengeEntryView_text_challengeZoneTextViewSet() {
        createFragment(
            cres = CRES_TEXT_DATA
        ) { fragment ->
            assertNotNull(fragment.challengeZoneTextView)

            val challengeZoneView = fragment.viewBinding.caChallengeZone
            assertTrue(challengeZoneView.challengeEntryView.children.first() is ChallengeZoneTextView)
            assertEquals(
                "Submit",
                challengeZoneView.submitButton.text
            )
            assertEquals(
                "Resend",
                challengeZoneView.resendButton.text
            )
        }
    }

    @Test
    fun setChallengeEntryView_singleSelect_challengeZoneSingleSelectViewSet() {
        createFragment(
            cres = CRES_SINGLE_SELECT_DATA
        ) { fragment ->
            assertNotNull(fragment.challengeZoneSelectView)
            assertTrue(requireNotNull(fragment.challengeZoneSelectView).isSingleSelectMode)

            val challengeZoneView = fragment.viewBinding.caChallengeZone
            assertTrue(
                challengeZoneView.challengeEntryView.getChildAt(0) is ChallengeZoneSelectView
            )
            assertEquals(
                "Next",
                challengeZoneView.submitButton.text
            )

            fragment.challengeZoneSelectView.selectOption(0)
            assertEquals("phone", fragment.userEntry)
            fragment.challengeZoneSelectView.selectOption(1)
            assertEquals("email", fragment.userEntry)
        }
    }

    @Test
    fun setChallengeEntryView_multiSelect_challengeZoneMultiSelectViewSet() {
        createFragment(
            cres = CRES_MULTI_SELECT_DATA
        ) { fragment ->
            assertNotNull(fragment.challengeZoneSelectView)
            assertFalse(requireNotNull(fragment.challengeZoneSelectView).isSingleSelectMode)

            val challengeZoneView = fragment.viewBinding.caChallengeZone
            assertTrue(
                challengeZoneView.challengeEntryView.getChildAt(0) is ChallengeZoneSelectView
            )
            assertEquals(
                "Next",
                challengeZoneView.submitButton.text
            )

            assertEquals("", fragment.userEntry)
            fragment.challengeZoneSelectView.selectOption(0)
            assertEquals("phone", fragment.userEntry)
            fragment.challengeZoneSelectView.selectOption(1)
            assertEquals("phone,email", fragment.userEntry)
        }
    }

    @Test
    fun setChallengeEntryView_oob_isSetupCorrectly() {
        createFragment(
            cres = CRES_OOB_DATA
        ) { fragment ->
            val challengeZoneView = fragment.viewBinding.caChallengeZone
            assertEquals(
                0,
                challengeZoneView.challengeEntryView.childCount
            )
            assertEquals(
                "Continue OOB",
                challengeZoneView.submitButton.text
            )
        }
    }

    @Test
    fun setChallengeEntryView_html_ChallengeZoneWebViewSet() {
        createFragment(
            cres = CRES_HTML_DATA
        ) { fragment ->
            assertNotNull(fragment.challengeZoneWebView)

            val brandZoneView = fragment.viewBinding.caBrandZone
            assertFalse(brandZoneView.isShown)

            val challengeZoneView = fragment.viewBinding.caChallengeZone
            assertFalse(challengeZoneView.infoHeader.isShown)
            assertFalse(challengeZoneView.infoTextView.isShown)
            assertFalse(challengeZoneView.submitButton.isShown)

            assertNotNull(fragment.challengeZoneWebView?.onClickListener)
        }
    }

    @Test
    fun refreshUi_htmlOOB_acsRefreshHtmlDisplayed() {
        createFragment(cres = CRES_HTML_DATA) { fragment ->
            fragment.refreshUi()
            assertThat(fragment.challengeZoneWebView?.webView)
                .isNotNull()
        }
    }

    @Test
    fun `refreshUi() when native OOB UI Type should update infoText to additionalInfoDisplayed`() {
        createFragment(
            cres = CRES_OOB_DATA
        ) { fragment ->
            val challengeZoneView = fragment.viewBinding.caChallengeZone
            assertThat(challengeZoneView.infoTextView.text)
                .isEqualTo("Original Info Text")
            fragment.refreshUi()
            assertThat(challengeZoneView.infoTextView.text)
                .isEqualTo("Additional Info Text")
        }
    }

    @Test
    fun `start with null args should trigger error and finish`() {
        createFragment(cres = null) { fragment ->
            val runtimeError = assertIs<ChallengeResult.RuntimeError>(
                fragment.viewModel.shouldFinish.value
            )
            assertThat(
                assertIs<IllegalArgumentException>(runtimeError.throwable).message
            ).isEqualTo(
                "Could not start challenge screen. Challenge response data was null."
            )
        }
    }

    private fun createFragment(
        cres: ChallengeResponseData? = ChallengeMessageFixtures.CRES_TEXT_DATA,
        onFragment: (ChallengeFragment) -> Unit
    ): FragmentScenario<ChallengeFragment> {
        return launchFragmentInContainer<ChallengeFragment>(
            fragmentArgs = bundleOf(ChallengeFragment.ARG_CRES to cres),
            themeResId = R.style.Stripe3DS2Theme,
            factory = ChallengeFragmentFactory(
                uiCustomization = UiCustomizationFixtures.DEFAULT,
                transactionTimer = transactionTimer,
                errorRequestExecutor = mock(),
                errorReporter = errorReporter,
                challengeActionHandler = challengeActionHandler,
                intentData = IntentDataFixtures.DEFAULT,
                initialUiType = UiType.Text,
                workContext = testDispatcher
            )
        ).onFragment(onFragment)
    }

    private class FakeChallengeActionHandler : ChallengeActionHandler {
        var result = ChallengeRequestResult.Success(
            ChallengeMessageFixtures.CREQ,
            ChallengeMessageFixtures.CRES,
            ChallengeRequestExecutorFixtures.CONFIG
        )
        val actions = mutableListOf<ChallengeAction>()

        override suspend fun submit(
            action: ChallengeAction
        ): ChallengeRequestResult {
            actions.add(action)
            return result
        }
    }

    private companion object {
        private const val WHY_INFO_LABEL = "Why Info Label"
        private const val WHY_INFO_TEXT = "Why Info Text"
        private const val EXPAND_INFO_LABEL = "Expand Info Label"
        private const val EXPAND_INFO_TEXT = "Expand Info Text"
        private const val HEADER_TEXT = "Header Text"
        private const val INFO_TEXT = "Info Text"
        private const val WHITELIST_TEXT = "Whitelist Text"
        private const val NEXT_BUTTON = "Next"
        private const val SUBMIT_BUTTON = "Submit"
        private const val RESEND_BUTTON = "Resend"
        private const val ACS_HTML = "<h1>TEST</h1>"
        private const val ACS_REFRESH_HTML = "<h1>REFRESH</h1>"

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

        private val CRES_TEXT_DATA = ChallengeMessageFixtures.CRES.copy(
            uiType = UiType.Text,
            shouldShowChallengeInfoTextIndicator = true,
            submitAuthenticationLabel = SUBMIT_BUTTON,
            resendInformationLabel = RESEND_BUTTON,
            issuerImage = ChallengeMessageFixtures.ISSUER_IMAGE,
            paymentSystemImage = ChallengeMessageFixtures.PAYMENT_SYSTEM_IMAGE,
            challengeInfoHeader = HEADER_TEXT,
            challengeInfoText = INFO_TEXT,
            whitelistingInfoText = WHITELIST_TEXT,
            whyInfoLabel = WHY_INFO_LABEL,
            whyInfoText = WHY_INFO_TEXT,
            expandInfoLabel = EXPAND_INFO_LABEL,
            expandInfoText = EXPAND_INFO_TEXT
        )

        private val CRES_SINGLE_SELECT_DATA = ChallengeMessageFixtures.CRES.copy(
            uiType = UiType.SingleSelect,
            submitAuthenticationLabel = NEXT_BUTTON,
            resendInformationLabel = RESEND_BUTTON,
            challengeSelectOptions = OPTIONS,
            whyInfoLabel = WHY_INFO_LABEL,
            whyInfoText = WHY_INFO_TEXT,
            expandInfoLabel = EXPAND_INFO_LABEL,
            expandInfoText = EXPAND_INFO_TEXT
        )

        private val CRES_MULTI_SELECT_DATA = ChallengeMessageFixtures.CRES.copy(
            uiType = UiType.MultiSelect,
            submitAuthenticationLabel = NEXT_BUTTON,
            resendInformationLabel = RESEND_BUTTON,
            challengeSelectOptions = OPTIONS,
            whyInfoLabel = WHY_INFO_LABEL,
            whyInfoText = WHY_INFO_TEXT,
            expandInfoLabel = EXPAND_INFO_LABEL,
            expandInfoText = EXPAND_INFO_TEXT
        )

        private val CRES_OOB_DATA = ChallengeMessageFixtures.CRES.copy(
            uiType = UiType.OutOfBand,
            oobContinueLabel = "Continue OOB",
            challengeInfoText = "Original Info Text",
            challengeAdditionalInfoText = "Additional Info Text"
        )

        private val CRES_HTML_DATA = ChallengeMessageFixtures.CRES.copy(
            uiType = UiType.Html,
            acsHtml = ACS_HTML,
            acsHtmlRefresh = ACS_REFRESH_HTML
        )
    }
}
