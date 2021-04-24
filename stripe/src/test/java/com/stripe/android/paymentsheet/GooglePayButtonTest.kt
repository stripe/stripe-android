package com.stripe.android.paymentsheet

import android.content.Context
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
            PrimaryButton.State.StartProcessing,
        )
        googlePayButton.updateState(
            PrimaryButton.State.FinishProcessing {},
        )
        assertThat(primaryButton.isVisible).isTrue()
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.isVisible).isFalse()
        assertThat(primaryButton.backgroundTintList).isNull()

        googlePayButton.updateState(
            PrimaryButton.State.Ready
        )
        assertThat(primaryButton.isVisible).isFalse()
        assertThat(primaryButton.backgroundTintList).isNotNull()
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.isVisible).isTrue()
    }

    @Test
    fun `onStartProcessing() should update label`() {
        googlePayButton.updateState(
            PrimaryButton.State.StartProcessing,
        )
        assertThat(
            primaryButton.viewBinding.label.text.toString()
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
            },
        )

        idleLooper()

        assertThat(finishedProcessing).isTrue()
        assertThat(primaryButton.isVisible).isTrue()
        assertThat(googlePayButton.viewBinding.googlePayButtonIcon.isVisible).isFalse()
    }

    @Test
    fun `label alpha is initially 50%`() {
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after viewState ready and disabled, label alpha is 50%`() {
        googlePayButton.updateState(PrimaryButton.State.Ready)
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after viewState ready and enabled, label alpha is 100%`() {
        googlePayButton.updateState(PrimaryButton.State.Ready)
        googlePayButton.isEnabled = true
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(1.0f)
    }
}
