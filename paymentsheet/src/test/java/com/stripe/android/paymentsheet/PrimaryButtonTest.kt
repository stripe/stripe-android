package com.stripe.android.paymentsheet

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View.GONE
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.uicore.StripeThemeDefaults
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
        primaryButton.setAppearanceConfiguration(StripeThemeDefaults.primaryButtonStyle, ColorStateList.valueOf(Color.BLACK))
        primaryButton.updateState(
            PrimaryButton.State.FinishProcessing({})
        )
        assertThat(primaryButton.backgroundTintList).isNull()

        primaryButton.updateState(
            PrimaryButton.State.Ready
        )
        assertThat((primaryButton.background as GradientDrawable).color).isEqualTo(ColorStateList.valueOf(Color.BLACK))
    }

    @Test
    fun `onStartProcessing() and onFinishingState() should make button not clickable`() {
        primaryButton.setAppearanceConfiguration(StripeThemeDefaults.primaryButtonStyle, ColorStateList.valueOf(Color.BLACK))
        primaryButton.updateState(
            PrimaryButton.State.StartProcessing
        )
        assertThat(primaryButton.isClickable).isFalse()

        primaryButton.updateState(
            PrimaryButton.State.Ready
        )
        assertThat(primaryButton.isClickable).isTrue()

        primaryButton.updateState(
            PrimaryButton.State.FinishProcessing({})
        )
        assertThat(primaryButton.isClickable).isFalse()

        primaryButton.updateState(
            PrimaryButton.State.Ready
        )
        assertThat(primaryButton.isClickable).isTrue()
    }

    @Test
    fun `onReadyState() should update label`() {
        primaryButton.updateUiState(
            PrimaryButton.UIState(
                label = "Pay $10.99",
                onClick = {},
                enabled = true,
                lockVisible = true,
            )
        )

        primaryButton.backgroundTintList = ColorStateList.valueOf(Color.BLACK)

        primaryButton.updateState(PrimaryButton.State.StartProcessing)
        assertThat(
            primaryButton.externalLabel
        ).isEqualTo(
            "Processing…"
        )

        primaryButton.updateState(
            PrimaryButton.State.Ready
        )

        assertThat(
            primaryButton.externalLabel
        ).isEqualTo(
            "Pay $10.99"
        )
    }

    @Test
    fun `onStartProcessing() should update label`() {
        primaryButton.updateState(
            PrimaryButton.State.StartProcessing
        )
        assertThat(
            primaryButton.externalLabel
        ).isEqualTo(
            "Processing…"
        )
    }

    @Test
    fun `label alpha is initially 50pct`() {
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
        assertThat(primaryButton.viewBinding.lockIcon.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after viewState ready and disabled, label alpha is 50pct`() {
        primaryButton.updateState(
            PrimaryButton.State.Ready
        )
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
        assertThat(primaryButton.viewBinding.lockIcon.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after viewState ready and enabled, label alpha is 100pct`() {
        primaryButton.updateState(
            PrimaryButton.State.Ready
        )
        primaryButton.isEnabled = true
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(1.0f)
        assertThat(primaryButton.viewBinding.lockIcon.alpha)
            .isEqualTo(1.0f)
    }

    @Test
    fun `when lockVisible set to false, lock is hidden`() {
        primaryButton.lockVisible = false
        primaryButton.isEnabled = true

        primaryButton.updateState(
            PrimaryButton.State.Ready
        )
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(1.0f)
        assertThat(primaryButton.viewBinding.lockIcon.visibility)
            .isEqualTo(GONE)
    }
}
