package com.stripe.android.model

import com.stripe.android.StripeIntentResult
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AlipayAuthResult(
    @StripeIntentResult.Outcome val outcome: Int
) : StripeModel {
    internal companion object {
        // https://intl.alipay.com/docs/ac/3rdpartryqrcode/standard_4
        const val RESULT_CODE_SUCCESS = "9000"
        const val RESULT_CODE_CANCELLED = "6001"
        const val RESULT_CODE_FAILED = "4000"
    }
}
