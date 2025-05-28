package com.stripe.android.shoppay

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

    @Test
    fun `intent is created correctly`() {
        val contract = ShopPayActivityContract()

        val intent = contract.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            input = ShopPayActivityContract.Args("https://example.com")
        )

        assertThat(intent.component?.className).isEqualTo(ShopPayActivity::class.java.name)

        val actualArg = intent.extras?.let {
            BundleCompat.getParcelable(it, ShopPayActivity.EXTRA_ARGS, ShopPayArgs::class.java)
        }
        assertThat(actualArg).isEqualTo(
            ShopPayArgs("https://example.com")
        )
    }

    @Test
    fun `canceled result is parsed correctly`() = testActivityResult(
        expectedResult = ShopPayActivityResult.Canceled
    )

    @Test
    fun `failed result is parsed correctly`() = testActivityResult(
        expectedResult = ShopPayActivityResult.Failed(
            error = Throwable("Something went wrong")
        )
    )

    @Test
    fun `completed result is parsed correctly`() = testActivityResult(
        expectedResult = ShopPayActivityResult.Completed("spm_1234")
    )

    private fun testActivityResult(expectedResult: ShopPayActivityResult) {
        val contract = ShopPayActivityContract()

        val result = contract.parseResult(
            resultCode = ShopPayActivity.RESULT_COMPLETE,
            intent = intent(expectedResult)
        )

        assertThat(result).isEqualTo(expectedResult)
    }

    private fun intent(result: ShopPayActivityResult): Intent {
        val bundle = bundleOf(
            ShopPayActivityContract.EXTRA_RESULT to result
        )
        return Intent().apply {
            putExtras(bundle)
        }
    }
}
