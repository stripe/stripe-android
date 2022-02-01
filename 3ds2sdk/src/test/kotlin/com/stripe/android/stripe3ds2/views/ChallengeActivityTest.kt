package com.stripe.android.stripe3ds2.views

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.stripe3ds2.ChallengeMessageFixtures
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.IntentDataFixtures
import com.stripe.android.stripe3ds2.transactions.UiType
import com.stripe.android.stripe3ds2.utils.ImageCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ChallengeActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Test
    fun refreshUi_afterRecreate_shouldBeTrue() {
        createActivityScenario().use { activityScenario ->
            activityScenario.onActivity { activity ->
                assertFalse(activity.viewModel.shouldRefreshUi)
            }
            activityScenario.recreate().onActivity { activity ->
                assertTrue(activity.viewModel.shouldRefreshUi)
            }
        }
    }

    @Test
    fun refreshUi_onPause_shouldBeTrue() {
        createActivityScenario().use { activityScenario ->
            activityScenario.onActivity { activity ->
                activityScenario.moveToState(Lifecycle.State.RESUMED)
                assertFalse(activity.viewModel.shouldRefreshUi)
                activityScenario.moveToState(Lifecycle.State.STARTED)
                assertTrue(activity.viewModel.shouldRefreshUi)
                activityScenario.moveToState(Lifecycle.State.RESUMED)
                assertTrue(activity.viewModel.shouldRefreshUi)
            }
        }
    }

    @Test
    fun `typeTextChallengeValue should update challengeZoneTextView`() {
        createActivity { activity ->
            activity.viewModel.updateChallengeText("Hello, world!")
            val challengeZone = activity.fragmentViewBinding.caChallengeZone
            val challengeZoneTextView =
                challengeZone.challengeEntryView.children.first() as ChallengeZoneTextView
            assertThat(challengeZoneTextView.userEntry)
                .isEqualTo("Hello, world!")
        }
    }

    @Test
    fun `viewModel onFinish() should trigger activity finish`() {
        createActivity { activity ->
            assertThat(activity.isFinishing)
                .isFalse()
            activity.viewModel.onFinish(
                ChallengeResult.Canceled(
                    UiType.Text.code,
                    initialUiType = UiType.Text,
                    IntentDataFixtures.DEFAULT
                )
            )
            assertThat(activity.isFinishing)
                .isTrue()
        }
    }

    @Test
    fun `onLowMemory() should trigger clearing ImageCache`() {
        createActivity { activity ->
            ImageCache.Default["image"] = BITMAP

            assertThat(ImageCache.Default["image"])
                .isNotNull()
            activity.onLowMemory()
            assertThat(ImageCache.Default["image"])
                .isNull()
        }
    }

    @Test
    fun `informationZoneView should be configured based on CRes`() {
        createActivity { activity ->
            val informationZoneView = activity.fragmentViewBinding.caInformationZone

            assertThat(informationZoneView.expandLabel.text)
                .isEqualTo(EXPAND_INFO_LABEL)
            assertThat(informationZoneView.expandText.text)
                .isEqualTo(EXPAND_INFO_TEXT)
            assertThat(informationZoneView.whyLabel.text)
                .isEqualTo(WHY_INFO_LABEL)
            assertThat(informationZoneView.whyText.text)
                .isEqualTo(WHY_INFO_TEXT)
        }
    }

    @Test
    fun `when cancel button is clicked, disable cancel button`() {
        createActivity { activity ->
            val cancelButton = requireNotNull(activity.supportActionBar?.customView)
            cancelButton.performClick()

            assertThat(cancelButton.isClickable)
                .isFalse()
        }
    }

    @Test
    fun customizeHeaderZoneView_customToolbarCustomization_shouldCustomizeHeaderZoneView() {
        createActivity { activity ->
            val actionBar = requireNotNull(activity.supportActionBar)
            val cancelButton = actionBar.customView as ThreeDS2Button
            assertEquals(
                "CANCEL",
                cancelButton.text
            )

            assertEquals(
                "AUTHORIZE PAYMENT",
                actionBar.title.toString()
            )
        }
    }

    @Test
    fun init_withNullToolbarCustomization_shouldUseDefaults() {
        createActivity(
            uiCustomization = StripeUiCustomization()
        ) { activity ->
            val actionBar = requireNotNull(activity.supportActionBar)
            val cancelButton = actionBar.customView as ThreeDS2Button
            assertThat(cancelButton.text)
                .isEqualTo("Cancel")

            assertThat(actionBar.title)
                .isEqualTo("Secure Checkout")
        }
    }

    private fun createActivity(
        uiCustomization: StripeUiCustomization = UiCustomizationFixtures.DEFAULT,
        onActivity: (ChallengeActivity) -> Unit
    ) {
        createActivityScenario(
            uiCustomization = uiCustomization
        ).use {
            it.onActivity { activity ->
                onActivity(activity)
            }
        }
    }

    private fun createActivityScenario(
        uiCustomization: StripeUiCustomization = UiCustomizationFixtures.DEFAULT
    ): ActivityScenario<ChallengeActivity> {
        return ActivityScenarioFactory(
            ApplicationProvider.getApplicationContext(),
            cres = CRES_TEXT_DATA,
            uiCustomization = uiCustomization
        ).create()
    }

    private companion object {
        private val BITMAP = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)

        private const val WHY_INFO_LABEL = "Why Info Label"
        private const val WHY_INFO_TEXT = "Why Info Text"
        private const val EXPAND_INFO_LABEL = "Expand Info Label"
        private const val EXPAND_INFO_TEXT = "Expand Info Text"
        private const val HEADER_TEXT = "Header Text"
        private const val INFO_TEXT = "Info Text"
        private const val WHITELIST_TEXT = "Whitelist Text"
        private const val SUBMIT_BUTTON = "Submit"
        private const val RESEND_BUTTON = "Resend"

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
    }
}
