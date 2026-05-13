package com.stripe.android.payments.samsungpay

import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import com.samsung.android.sdk.samsungpay.v2.SamsungPay
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.StatusListener
import com.stripe.android.core.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

fun interface GetSamsungPayStatus {
    suspend operator fun invoke(): Status

    sealed interface Status {
        data object Ready : Status
        data object Disabled : Status
        data object NeedsUserSetup : Status
        data class Failed(val error: Throwable) : Status
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultGetSamsungPayStatus @Inject constructor(
    private val context: Context,
    private val logger: Logger
) : GetSamsungPayStatus {
    override suspend fun invoke(): GetSamsungPayStatus.Status {
        val partnerInfo = SamsungFactory.buildPartnerInfo()
        val samsungPay = SamsungPay(context, partnerInfo)

        return suspendCancellableCoroutine { continuation ->
            samsungPay.getSamsungPayStatus(object : StatusListener {
                override fun onSuccess(status: Int, bundle: Bundle?) {
                    continuation.resume(handleStatusSuccess(status, bundle))
                }

                override fun onFail(errorCode: Int, bundle: Bundle?) {
                    logger.error("getSamsungPayStatus.onFail: errorCode=$errorCode")
                    continuation.resume(
                        value = GetSamsungPayStatus.Status.Failed(
                            error = Throwable("getSamsungPayStatus.onFail: errorCode=$errorCode")
                        )
                    )
                }
            })
        }
    }

    private fun handleStatusSuccess(status: Int, bundle: Bundle?): GetSamsungPayStatus.Status {
        return when (status) {
            SpaySdk.SPAY_READY -> {
                GetSamsungPayStatus.Status.Ready
            }

            SpaySdk.SPAY_NOT_READY -> {
                logger.warning("handleStatusSuccess: SPAY_NOT_READY — Samsung Pay is supported but not fully ready")

                val errorReason = bundle?.getInt(SpaySdk.EXTRA_ERROR_REASON, -1) ?: -1
                logger.warning("handleStatusSuccess: SPAY_NOT_READY errorReason=$errorReason")

                val message = when (errorReason) {
                    SpaySdk.ERROR_SPAY_APP_NEED_TO_UPDATE ->
                        "Samsung Pay app needs to be updated."

                    SpaySdk.ERROR_SPAY_SETUP_NOT_COMPLETED ->
                        "Samsung Pay setup is not completed. Please set up Samsung Pay."

                    else ->
                        "Samsung Pay is not ready. Error reason: $errorReason"
                }
                logger.warning("handleStatusSuccess: SPAY_NOT_READY resolved message=\"$message\"")

                // If EXTRA_ERROR_REASON is ERROR_SPAY_SETUP_NOT_COMPLETED,
                // Call activateSamsungPay().
                val extraError = bundle?.getInt(SamsungPay.EXTRA_ERROR_REASON) ?: -1
                if (extraError == SamsungPay.ERROR_SPAY_SETUP_NOT_COMPLETED) {
                    GetSamsungPayStatus.Status.NeedsUserSetup
                } else {
                    GetSamsungPayStatus.Status.Disabled
                }
            }

            SpaySdk.SPAY_NOT_ALLOWED_TEMPORALLY -> {
                logger.warning(
                    "handleStatusSuccess: SPAY_NOT_ALLOWED_TEMPORALLY — Samsung Pay temporarily not allowed"
                )

                val errorReason = bundle?.getInt(SpaySdk.EXTRA_ERROR_REASON, -1) ?: -1
                logger.warning(
                    "handleStatusSuccess: SPAY_NOT_ALLOWED_TEMPORALLY errorReason=$errorReason"
                )

                val message = when (errorReason) {
                    SpaySdk.ERROR_SPAY_CONNECTED_WITH_EXTERNAL_DISPLAY ->
                        "Samsung Pay is unavailable while connected to an external display. Please disconnect it."

                    else ->
                        "Samsung Pay is temporarily unavailable. Error reason: $errorReason"
                }
                logger.warning(
                    "handleStatusSuccess: SPAY_NOT_ALLOWED_TEMPORALLY resolved message=\"$message\""
                )
                GetSamsungPayStatus.Status.Disabled
            }

            SpaySdk.SPAY_NOT_SUPPORTED -> {
                logger.error(
                    "handleStatusSuccess: SPAY_NOT_SUPPORTED — Samsung Pay is not supported on this device"
                )
                GetSamsungPayStatus.Status.Disabled
            }

            else -> {
                logger.error("handleStatusSuccess: Unknown status=$status")
                GetSamsungPayStatus.Status.Disabled
            }
        }
    }


}