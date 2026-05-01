package com.stripe.android.payments.samsungpay

import android.content.Context
import com.samsung.android.sdk.samsungpay.v2.PartnerInfo
import com.samsung.android.sdk.samsungpay.v2.SamsungPay
import com.samsung.android.sdk.samsungpay.v2.SpaySdk
import com.samsung.android.sdk.samsungpay.v2.StatusListener
import android.os.Bundle
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
            val samsungPay = SamsungPay(context, partnerInfo)
            samsungPay.getSamsungPayStatus(object : StatusListener {
                override fun onSuccess(status: Int, bundle: Bundle) {
                    val result = when (status) {
                        SpaySdk.SPAY_READY -> SamsungPayLauncher.AvailabilityResult.Ready

                        SpaySdk.SPAY_NOT_SUPPORTED -> SamsungPayLauncher.AvailabilityResult.NotSupported

                        SpaySdk.SPAY_NOT_READY -> {
                            when (bundle.getInt(SpaySdk.EXTRA_ERROR_REASON)) {
                                SpaySdk.ERROR_SPAY_SETUP_NOT_COMPLETED ->
                                    SamsungPayLauncher.AvailabilityResult.SetupRequired(
                                        launchSetup = { samsungPay.activateSamsungPay() }
                                    )
                                SpaySdk.ERROR_SPAY_APP_NEED_TO_UPDATE ->
                                    SamsungPayLauncher.AvailabilityResult.UpdateRequired(
                                        launchUpdate = { samsungPay.goToUpdatePage() }
                                    )
                                else -> SamsungPayLauncher.AvailabilityResult.NotSupported
                            }
                        }
                        else -> SamsungPayLauncher.AvailabilityResult.NotSupported
                    }
                    cont.resume(result)
                }

                override fun onFail(errorCode: Int, bundle: Bundle) {
                    cont.resume(SamsungPayLauncher.AvailabilityResult.NotSupported)
                }
            })
        }
}
