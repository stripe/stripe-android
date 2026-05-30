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
        return checkStatus(SamsungFactory.buildPartnerInfo(DEFAULT_SERVICE_ID))
    }

    internal suspend fun invoke(serviceId: String): GetSamsungPayStatus.Status {
        return checkStatus(SamsungFactory.buildPartnerInfo(serviceId))
    }

    private suspend fun checkStatus(
        partnerInfo: com.samsung.android.sdk.samsungpay.v2.PartnerInfo
    ): GetSamsungPayStatus.Status {
        val samsungPay = SamsungPay(context, partnerInfo)

        return suspendCancellableCoroutine { continuation ->
            samsungPay.getSamsungPayStatus(object : StatusListener {
                override fun onSuccess(status: Int, bundle: Bundle?) {
                    continuation.resume(handleStatusSuccess(status, bundle))
                }

                override fun onFail(errorCode: Int, bundle: Bundle?) {
                    logger.error("getSamsungPayStatus.onFail: errorCode=$errorCode, bundle=${bundle?.toString()}")
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
                logger.warning("handleStatusSuccess: SPAY_NOT_READY")

                val extraError = bundle?.getInt(SamsungPay.EXTRA_ERROR_REASON) ?: -1
                if (extraError == SamsungPay.ERROR_SPAY_SETUP_NOT_COMPLETED) {
                    GetSamsungPayStatus.Status.NeedsUserSetup
                } else {
                    GetSamsungPayStatus.Status.Disabled
                }
            }

            SpaySdk.SPAY_NOT_ALLOWED_TEMPORALLY -> {
                logger.warning("handleStatusSuccess: SPAY_NOT_ALLOWED_TEMPORALLY")
                GetSamsungPayStatus.Status.Disabled
            }

            SpaySdk.SPAY_NOT_SUPPORTED -> {
                logger.error("handleStatusSuccess: SPAY_NOT_SUPPORTED")
                GetSamsungPayStatus.Status.Disabled
            }

            else -> {
                logger.error("handleStatusSuccess: Unknown status=$status")
                GetSamsungPayStatus.Status.Disabled
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val DEFAULT_SERVICE_ID = "716e0e5ea6c64b47b467fe"
    }
}
