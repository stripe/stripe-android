package com.stripe.android.common.taptoadd

import android.app.Activity
import android.content.Intent
import androidx.core.os.bundleOf
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.view.ActivityStarter
import org.junit.Test

class TapToAddContractTest {
    @Test
    fun `parseResult returns parsed completed result`() = resultTest(result = TapToAddResult.Complete)

    @Test
    fun `parseResult returns parsed canceled result`() = resultTest(
        result = TapToAddResult.Canceled(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFactory.card(),
            ),
        ),
    )

    @Test
    fun `parseResult returns parsed continue result`() = resultTest(
        result = TapToAddResult.Continue(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFactory.card(),
            ),
        ),
    )

    @Test
    fun `parseResult returns canceled by default`() = resultTest(
        result = null,
        expectedResult = TapToAddResult.Canceled(
            paymentSelection = null,
        ),
    )
    private fun resultTest(
        resultCode: Int = Activity.RESULT_OK,
        result: TapToAddResult?,
        expectedResult: TapToAddResult? = result
    ) {
        val result = TapToAddContract.parseResult(
            resultCode,
            intent(result)
        )
        assertThat(result).isEqualTo(expectedResult)
    }

    private fun intent(result: TapToAddResult?): Intent {
        return Intent().putExtras(
            bundleOf(ActivityStarter.Result.EXTRA to result)
        )
    }
}
