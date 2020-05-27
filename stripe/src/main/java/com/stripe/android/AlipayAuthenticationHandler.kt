package com.stripe.android

import androidx.annotation.WorkerThread
import com.stripe.android.model.PaymentIntent

/**
 * Interface for handling [PaymentIntent] authentication with the Alipay SDK
 * @see <a href="https://intl.alipay.com/docs/ac/app/sdk_integration">Alipay Documentation</a>
 *
 * To authenticate using the Alipay SDK, pass the data String to PayTask#payV2
 * <pre>
 * new AlipayAuthenticationHandler() {
 *  @NotNull
 *  @Override
 *  Map<String, String> authenticate(@NotNull String data) {
 *      return new PayTask(this).payV2(data, true)
 *  }
 * }
 * </pre>
 */
interface AlipayAuthenticationHandler {
    @WorkerThread
    fun authenticate(data: String): Map<String, String>
}
