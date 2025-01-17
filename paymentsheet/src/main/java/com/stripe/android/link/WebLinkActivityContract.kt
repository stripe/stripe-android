package com.stripe.android.link

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.serialization.PopupPayload
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.networking.StripeRepository
import org.json.JSONObject
import javax.inject.Inject

internal class WebLinkActivityContract @Inject internal constructor(
    private val stripeRepository: StripeRepository,
) : ActivityResultContract<LinkActivityContract.Args, LinkActivityResult>() {

    override fun createIntent(context: Context, input: LinkActivityContract.Args): Intent {
        val paymentConfiguration = PaymentConfiguration.getInstance(context)
        val payload = PopupPayload.create(
            configuration = input.configuration,
            context = context,
            publishableKey = paymentConfiguration.publishableKey,
            stripeAccount = paymentConfiguration.stripeAccountId,
            paymentUserAgent = stripeRepository.buildPaymentUserAgent(),
        )
        return LinkForegroundActivity.createIntent(context, payload.toUrl())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): LinkActivityResult {
        return when (resultCode) {
            LinkForegroundActivity.RESULT_FAILURE -> {
                val exception = intent?.extras?.let {
                    BundleCompat.getSerializable(
                        it,
                        LinkForegroundActivity.EXTRA_FAILURE,
                        Exception::class.java
                    )
                }
                if (exception != null) {
                    LinkActivityResult.Failed(exception)
                } else {
                    LinkActivityResult.Canceled()
                }
            }

            LinkForegroundActivity.RESULT_COMPLETE -> {
                val redirectUri = intent?.data ?: return LinkActivityResult.Canceled()
                when (redirectUri.getQueryParameter("link_status")) {
                    "complete" -> {
                        val paymentMethod = redirectUri.getQueryParameter("pm")
                            ?.parsePaymentMethod()
                        if (paymentMethod == null) {
                            LinkActivityResult.Canceled()
                        } else {
                            LinkActivityResult.PaymentMethodObtained(paymentMethod)
                        }
                    }

                    "logout" -> {
                        LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.LoggedOut)
                    }

                    else -> {
                        LinkActivityResult.Canceled()
                    }
                }
            }

            Activity.RESULT_CANCELED -> {
                LinkActivityResult.Canceled()
            }
            else -> {
                LinkActivityResult.Canceled()
            }
        }
    }

    @SuppressWarnings("TooGenericExceptionCaught", "SwallowedException")
    private fun String.parsePaymentMethod(): PaymentMethod? = try {
        val decodedPaymentMethod = String(Base64.decode(this, 0), Charsets.UTF_8)
        val paymentMethod = PaymentMethodJsonParser()
            .parse(JSONObject(decodedPaymentMethod))
        paymentMethod
    } catch (e: Exception) {
        null
    }
}
