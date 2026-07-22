package com.stripe.android.crypto.onramp.samsungpay

import android.content.Context
import androidx.annotation.MainThread
import com.stripe.android.crypto.onramp.exception.SamsungPayException.Reason
import com.stripe.android.crypto.onramp.model.OnrampConfiguration

internal interface SamsungPayLauncher {
    @MainThread
    fun getStatus(callback: (SamsungPayStatus) -> Unit)

    @MainThread
    fun present(
        presentation: SamsungPayPresentation,
        callback: (SamsungPayResult) -> Unit,
    )

    @MainThread
    fun destroy()

    fun interface Factory {
        fun create(
            context: Context,
            configuration: OnrampConfiguration.SamsungPayConfig,
            merchantDisplayName: String,
        ): SamsungPayLauncher
    }
}

internal fun interface SamsungPayClassProvider {
    fun loadClass(name: String): Class<*>
}

internal data class SamsungPayPresentation(
    val currencyCode: String,
    val amount: Long,
    val orderNumber: String,
)

internal sealed interface SamsungPayStatus {
    data object Ready : SamsungPayStatus

    data object NotSupported : SamsungPayStatus

    data object TemporarilyUnavailable : SamsungPayStatus

    data class NotReady(
        val reason: Reason,
    ) : SamsungPayStatus {
        internal sealed interface Reason {
            data object NeedsUserSetup : Reason

            data object NeedsAppUpdate : Reason

            data class Other(val code: Int?) : Reason
        }
    }

    data class Failed(val error: Throwable) : SamsungPayStatus
}

internal sealed interface SamsungPayResult {
    data class Completed(val paymentCredential: String) : SamsungPayResult

    data object Canceled : SamsungPayResult

    data class Failed(val error: Throwable) : SamsungPayResult
}

internal class SamsungPayException(
    message: String,
    cause: Throwable?,
    val errorCode: Int?,
    val reason: Reason,
) : IllegalStateException(message, cause)
