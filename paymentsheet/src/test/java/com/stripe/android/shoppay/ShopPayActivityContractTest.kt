package com.stripe.android.shoppay

import android.app.Activity
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ShopPayActivityContractTest {

    private val contract = ShopPayActivityContract()

    @Test
    fun `intent is created correctly`() {
        val args = ShopPayActivityContract.Args(
            checkoutUrl = "https://example.com/checkout"
        )

        val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), args)
        val intentArgs = intent.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivity.EXTRA_ARGS, ShopPayArgs::class.java)
        }

        assertThat(intent.component?.className).isEqualTo(ShopPayActivity::class.java.name)
        assertThat(intentArgs?.checkoutUrl).isEqualTo("https://example.com/checkout")
    }

    @Test
    fun `parseResult with completed result`() {
        val result = contract.parseResult(
            Activity.RESULT_OK,
            intent(ShopPayActivityResult.Completed)
        )
        assertThat(result).isEqualTo(ShopPayActivityResult.Completed)
    }

    @Test
    fun `parseResult with canceled result`() {
        val result = contract.parseResult(
            Activity.RESULT_OK,
            intent(ShopPayActivityResult.Canceled)
        )
        assertThat(result).isEqualTo(ShopPayActivityResult.Canceled)
    }

    @Test
    fun `parseResult with failed result`() {
        val throwable = RuntimeException("Failed")
        val result = contract.parseResult(
            Activity.RESULT_OK,
            intent(ShopPayActivityResult.Failed(throwable))
        )
        assertThat(result).isInstanceOf(ShopPayActivityResult.Failed::class.java)
        val failedResult = result as? ShopPayActivityResult.Failed
        assertThat(failedResult?.error?.message).isEqualTo(throwable.message)
    }

    @Test
    fun `parseResult with no result in intent returns failed`() {
        val result = contract.parseResult(
            Activity.RESULT_OK,
            Intent()
        )
        assertThat(result).isInstanceOf(ShopPayActivityResult.Failed::class.java)
        val failedResult = result as? ShopPayActivityResult.Failed
        assertThat(failedResult?.error?.message).isEqualTo("No result")
    }

    @Test
    fun `parseResult with different resultCode still parses result`() {
        val result = contract.parseResult(
            Activity.RESULT_CANCELED,
            intent(ShopPayActivityResult.Completed)
        )
        assertThat(result).isEqualTo(ShopPayActivityResult.Completed)
    }

    private fun intent(result: ShopPayActivityResult): Intent {
        return Intent().putExtras(
            bundleOf(ShopPayActivityContract.EXTRA_RESULT to result)
        )
    }
}
