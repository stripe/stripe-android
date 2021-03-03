package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.runBlocking
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
    fun `onReadyState() should update label`() {
        runBlocking{
            primaryButton.updateState(
                PrimaryButton.State.Ready("Pay $10.99")
            )
        }

        // THe logic tested here is in the PaymentActivity now and needs to be tested around that observable
        assertThat(
            primaryButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Pay $10.99"
        )
    }

    @Test
    fun `onConfirmingState() should update label`() {
        runBlocking {
            primaryButton.updateState(PrimaryButton.State.Confirming)
            assertThat(
                primaryButton.viewBinding.label.text.toString()
            ).isEqualTo(
                "Processing…"
            )
        }
    }

    @Test
    fun `label alpha is initially 50%`() {
        assertThat(primaryButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after viewState ready and disabled, label alpha is 50%`() {
        runBlocking {
            primaryButton.updateState(
                PrimaryButton.State.Ready("$10.99")
            )
            assertThat(primaryButton.viewBinding.label.alpha)
                .isEqualTo(0.5f)
        }
    }

    @Test
    fun `after viewState ready and enabled, label alpha is 100%`() {
        runBlocking {
            primaryButton.updateState(
                PrimaryButton.State.Ready("$10.99")
            )
            primaryButton.isEnabled = true
            assertThat(primaryButton.viewBinding.label.alpha)
                .isEqualTo(1.0f)
        }
    }
}
