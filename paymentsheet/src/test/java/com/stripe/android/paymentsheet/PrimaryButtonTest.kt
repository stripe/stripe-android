package com.stripe.android.paymentsheet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.uicore.StripeThemeDefaults
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PrimaryButtonTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val primaryButton: PrimaryButton by lazy {
        createView {
            PrimaryButton(it)
        }
    }

    @get:Rule
    internal val testActivityRule = createTestActivityRule<TestActivity>()

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
                label = "Pay $10.99".resolvableString,
                onClick = {},
                enabled = true,
                lockVisible = true,
            )
        )

        primaryButton.backgroundTintList = ColorStateList.valueOf(Color.BLACK)

        primaryButton.updateState(PrimaryButton.State.StartProcessing)
        assertThat(
            primaryButton.externalLabel?.resolve(context)
        ).isEqualTo(
            "Processing…"
        )

        primaryButton.updateState(
            PrimaryButton.State.Ready
        )

        assertThat(
            primaryButton.externalLabel?.resolve(context)
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
            primaryButton.externalLabel?.resolve(context)
        ).isEqualTo(
            "Processing…"
        )
    }

    @Test
    fun `onStartProcessing() should hide lock`() {
        primaryButton.updateUiState(
            PrimaryButton.UIState(
                label = "Pay $50".resolvableString,
                lockVisible = true,
                enabled = true,
                onClick = {},
            )
        )

        assertThat(
            primaryButton.viewBinding.lockIcon.isVisible
        ).isTrue()

        primaryButton.updateState(
            PrimaryButton.State.StartProcessing
        )

        // Setting lock visible after we start processing should still keep the lock hidden
        primaryButton.updateUiState(
            PrimaryButton.UIState(
                label = "Pay $50".resolvableString,
                lockVisible = true,
                enabled = true,
                onClick = {},
            )
        )

        assertThat(
            primaryButton.viewBinding.lockIcon.isVisible
        ).isFalse()
    }

    @Test
    fun `onFinishProcessing() should hide lock`() {
        primaryButton.updateUiState(
            PrimaryButton.UIState(
                label = "Pay $50".resolvableString,
                lockVisible = true,
                enabled = true,
                onClick = {},
            )
        )

        assertThat(
            primaryButton.viewBinding.lockIcon.isVisible
        ).isTrue()

        primaryButton.updateState(
            PrimaryButton.State.FinishProcessing(onComplete = {})
        )

        // Setting lock visible after we finish processing should still keep the lock hidden
        primaryButton.updateUiState(
            PrimaryButton.UIState(
                label = "Pay $50".resolvableString,
                lockVisible = true,
                enabled = true,
                onClick = {},
            )
        )

        assertThat(
            primaryButton.viewBinding.lockIcon.isVisible
        ).isFalse()
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

    private fun <ViewType : View> createView(
        viewFactory: (Activity) -> ViewType
    ): ViewType {
        var view: ViewType? = null

        ActivityScenario.launch<TestActivity>(
            Intent(context, TestActivity::class.java)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                view = viewFactory(activity)
            }
        }

        return requireNotNull(view)
    }

    internal class TestActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val layout = LinearLayout(this)
            layout.addView(PrimaryButton(this))
            setContentView(layout)
        }
    }
}
