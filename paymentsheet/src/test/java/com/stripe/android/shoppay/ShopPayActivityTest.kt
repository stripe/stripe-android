package com.stripe.android.shoppay

import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.assertNoUnverifiedIntents
import androidx.test.espresso.intent.rule.IntentsRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ShopPayActivityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val intentsTestRule = IntentsRule()

    @Test
    fun `finishes with failed result when no args are passed`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ShopPayActivity::class.java)

        val scenario = ActivityScenario.launchActivityForResult<ShopPayActivity>(intent)

        assertThat(scenario.result.resultCode).isEqualTo(ShopPayActivity.RESULT_COMPLETE)

        val resultIntent = scenario.result.resultData
        val result = resultIntent.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivityContract.EXTRA_RESULT, ShopPayActivityResult::class.java)
        }

        assertThat(result).isInstanceOf(ShopPayActivityResult.Failed::class.java)
        val failedResult = result as ShopPayActivityResult.Failed
        assertThat(failedResult.error.message).isEqualTo("No args")
        assertNoUnverifiedIntents()
    }

    @Test
    fun `createIntent creates correct intent with args`() {
        val shopPayConfiguration = mock<PaymentSheet.ShopPayConfiguration>()
        val args = ShopPayArgs(shopPayConfiguration)

        val intent = ShopPayActivity.createIntent(context, args)

        assertThat(intent.component?.className).isEqualTo(ShopPayActivity::class.java.name)
        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivity.EXTRA_ARGS, ShopPayArgs::class.java)
        }
        assertThat(intentArgs?.shopPayConfiguration).isEqualTo(shopPayConfiguration)
    }
}
