package com.stripe.android.paymentsheet

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.view.ActivityScenarioFactory
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class GooglePayButtonTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private val googlePayButton: GooglePayButton by lazy {
        activityScenarioFactory.createView {
            GooglePayButton(it)
        }
    }

    private val primaryButton: PrimaryButton by lazy {
        googlePayButton.viewBinding.primaryButton
    }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `onReadyState() should update the backgroundTint`() {
        googlePayButton.updateState(
            PrimaryButton.State.StartProcessing
        )
        googlePayButton.updateState(
            PrimaryButton.State.FinishProcessing {}
        )
        assertThat(primaryButton.isVisible).isTrue()
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.isVisible).isFalse()

        googlePayButton.updateState(
            PrimaryButton.State.Ready
        )
        assertThat(primaryButton.isVisible).isFalse()
        assertThat((primaryButton.background as GradientDrawable).color).isNotNull()
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.isVisible).isTrue()
    }

    @Test
    fun `onStartProcessing() should update label`() {
        googlePayButton.updateState(
            PrimaryButton.State.StartProcessing
        )
        assertThat(
            primaryButton.externalLabel
        ).isEqualTo(
            "Processing…"
        )
        assertThat(primaryButton.isVisible).isTrue()
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.isVisible).isFalse()
    }

    @Test
    fun `onFinishProcessing() should set the background resource`() {
        var finishedProcessing = false
        googlePayButton.updateState(
            PrimaryButton.State.FinishProcessing {
                finishedProcessing = true
            }
        )

        idleLooper()

        assertThat(finishedProcessing).isTrue()
        assertThat(primaryButton.isVisible).isTrue()
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.isVisible).isFalse()
    }

    @Test
    fun `not setting view state and not enabled should be 50% alpha`() {
        googlePayButton.isEnabled = false
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `ready view state and not enabled should be 50% alpha`() {
        googlePayButton.updateState(PrimaryButton.State.Ready)
        googlePayButton.isEnabled = false
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `ready view state and enabled should be 100% alpha`() {
        googlePayButton.updateState(PrimaryButton.State.Ready)
        googlePayButton.isEnabled = true
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.alpha)
            .isEqualTo(1.0f)
    }
}
