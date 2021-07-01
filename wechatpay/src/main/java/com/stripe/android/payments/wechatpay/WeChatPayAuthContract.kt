package com.stripe.android.payments.wechatpay

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.model.WeChat
import com.stripe.android.payments.PaymentFlowResult
import kotlinx.parcelize.Parcelize

internal class WeChatPayAuthContract :
    ActivityResultContract<WeChatPayAuthContract.Args, PaymentFlowResult.Unvalidated>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, WeChatPayAuthActivity::class.java).putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentFlowResult.Unvalidated {
        return PaymentFlowResult.Unvalidated.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val weChat: WeChat,
        val clientSecret: String
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }
}
