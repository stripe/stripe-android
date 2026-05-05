package com.stripe.android.payments.samsungpay

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SamsungPay
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.StatusListener
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Checks Samsung Pay availability on the device.
 */
internal fun interface SamsungPayRepository {
    suspend fun checkAvailability(): SamsungPayLauncher.AvailabilityResult
}

internal class DefaultSamsungPayRepository(
    private val context: Context,
    private val partnerInfo: PartnerInfo,
) : SamsungPayRepository {

    override suspend fun checkAvailability(): SamsungPayLauncher.AvailabilityResult =
        suspendCancellableCoroutine { cont ->
            Log.d(TAG, "Checking Samsung Pay availability...")
            val samsungPay = SamsungPay(context, partnerInfo)
            samsungPay.getSamsungPayStatus(object : StatusListener {
                override fun onSuccess(status: Int, bundle: Bundle) {
                    Log.d(TAG, "getSamsungPayStatus onSuccess: status=$status, bundle=$bundle")
                    val errorReason = bundle.getInt(SpaySdk.EXTRA_ERROR_REASON, -1)
                    Log.d(TAG, "  errorReason=$errorReason")

                    val result = when (status) {
                        SpaySdk.SPAY_READY -> {
                            Log.d(TAG, "  -> Ready")
                            SamsungPayLauncher.AvailabilityResult.Ready
                        }

                        SpaySdk.SPAY_NOT_SUPPORTED -> {
                            Log.d(TAG, "  -> NotSupported")
                            SamsungPayLauncher.AvailabilityResult.NotSupported
                        }

                        SpaySdk.SPAY_NOT_READY -> {
                            when (errorReason) {
                                SpaySdk.ERROR_SPAY_SETUP_NOT_COMPLETED -> {
                                    Log.d(TAG, "  -> SetupRequired")
                                    SamsungPayLauncher.AvailabilityResult.SetupRequired(
                                        launchSetup = { samsungPay.activateSamsungPay() }
                                    )
                                }
                                SpaySdk.ERROR_SPAY_APP_NEED_TO_UPDATE -> {
                                    Log.d(TAG, "  -> UpdateRequired")
                                    SamsungPayLauncher.AvailabilityResult.UpdateRequired(
                                        launchUpdate = { samsungPay.goToUpdatePage() }
                                    )
                                }
                                else -> {
                                    Log.d(TAG, "  -> NotSupported (not ready, reason=$errorReason)")
                                    SamsungPayLauncher.AvailabilityResult.NotSupported
                                }
                            }
                        }
                        else -> {
                            Log.d(TAG, "  -> NotSupported (unknown status=$status)")
                            SamsungPayLauncher.AvailabilityResult.NotSupported
                        }
                    }
                    cont.resume(result)
                }

                override fun onFail(errorCode: Int, bundle: Bundle) {
                    Log.e(TAG, "getSamsungPayStatus onFail: errorCode=$errorCode, bundle=$bundle")
                    cont.resume(SamsungPayLauncher.AvailabilityResult.NotSupported)
                }
            })
        }

    companion object {
        private const val TAG = "SamsungPayRepository"
    }
}
