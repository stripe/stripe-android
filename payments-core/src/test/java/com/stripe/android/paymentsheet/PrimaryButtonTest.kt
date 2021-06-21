package com.stripe.android.paymentsheet

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.view.ActivityScenarioFactory
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PrimaryButtonTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private val primaryButton: PrimaryButton by lazy {
        activityScenarioFactory.createView {
            PrimaryButton(it)
        }
    }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `onFinishingState() should clear any tint and restore onReadyState()`() {
        primaryButton.backgroundTintList = ColorStateList.valueOf(Color.BLACK)

        primaryButton.updateState(
            PrimaryButton.State.FinishProcessing({})
        )
        assertThat(primaryButton.backgroundTintList).isNull()

        primaryButton.updateState(
            PrimaryButton.State.Ready
        )
        assertThat(primaryButton.backgroundTintList).isEqualTo(ColorStateList.valueOf(Color.BLACK))
    }

    @Test
    fun `onReadyState() should update label`() {
        primaryButton.setLabel("Pay $10.99")
        primaryButton.backgroundTintList = ColorStateList.valueOf(Color.BLACK)

        primaryButton.updateState(
            PrimaryButton.State.StartProcessing
        )
        assertThat(
            primaryButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Processing…"
        )

        primaryButton.updateState(
            PrimaryButton.State.Ready
        )

        assertThat(
            primaryButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Pay $10.99"
        )
    }

    @Test
    fun `onStartProcessing() should update label`() {
        primaryButton.updateState(
            PrimaryButton.State.StartProcessing,
        )
        assertThat(
            primaryButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Processing…"
        )
    }

    @Test
    fun `label alpha is initially 50%`() {
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after viewState ready and disabled, label alpha is 50%`() {
        primaryButton.updateState(
            PrimaryButton.State.Ready
        )
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after viewState ready and enabled, label alpha is 100%`() {
        primaryButton.updateState(
            PrimaryButton.State.Ready
        )
        primaryButton.isEnabled = true
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(1.0f)
    }
}
