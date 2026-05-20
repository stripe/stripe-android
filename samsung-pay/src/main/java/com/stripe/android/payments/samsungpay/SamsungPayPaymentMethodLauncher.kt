package com.stripe.android.payments.samsungpay

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.lifecycle.lifecycleScope
import com.stripe.android.model.PaymentMethod
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class SamsungPayPaymentMethodLauncher internal constructor(
    lifecycleScope: CoroutineScope,
    private val config: Config,
    private val readyCallback: ReadyCallback,
    private val activityResultLauncher: ActivityResultLauncher<SamsungPayPaymentMethodLauncherContract.Args>,
    private val getSamsungPayStatus: GetSamsungPayStatus,
) {
    private var isReady = false

    constructor(
        activity: ComponentActivity,
        config: Config,
        readyCallback: ReadyCallback,
        resultCallback: ResultCallback,
    ) : this(
        lifecycleScope = activity.lifecycleScope,
        config = config,
        readyCallback = readyCallback,
        activityResultLauncher = activity.registerForActivityResult(
            SamsungPayPaymentMethodLauncherContract()
        ) {
            resultCallback.onResult(it)
        },
        getSamsungPayStatus = DefaultGetSamsungPayStatus(
            context = activity.applicationContext,
            logger = com.stripe.android.core.Logger.getInstance(true),
        ),
    )

    init {
        lifecycleScope.launch {
            val status = getSamsungPayStatus()
            val ready = status is GetSamsungPayStatus.Status.Ready
            isReady = ready
            readyCallback.onReady(ready)
        }
    }

    fun present(
        currencyCode: String,
        amount: Long,
        orderNumber: String,
    ) {
        check(isReady) {
            "present() may only be called when Samsung Pay is available on this device."
        }

        activityResultLauncher.launch(
            SamsungPayPaymentMethodLauncherContract.Args(
                config = config,
                currencyCode = currencyCode,
                amount = amount,
                orderNumber = orderNumber,
            )
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun present(
        currencyCode: String,
        amount: Long,
        orderNumber: String,
        skipReadyCheck: Boolean = false,
    ) {
        check(skipReadyCheck || isReady) {
            "present() may only be called when Samsung Pay is available on this device."
        }

        activityResultLauncher.launch(
            SamsungPayPaymentMethodLauncherContract.Args(
                config = config,
                currencyCode = currencyCode,
                amount = amount,
                orderNumber = orderNumber,
            )
        )
    }

    sealed class Result : Parcelable {
        @Parcelize
        @Poko
        class Completed(
            val paymentMethod: PaymentMethod,
        ) : Result()

        @Parcelize
        @Poko
        class Failed(
            val error: Throwable,
            @ErrorCode val errorCode: Int,
        ) : Result()

        @Parcelize
        data object Canceled : Result()
    }

    fun interface ReadyCallback {
        fun onReady(isReady: Boolean)
    }

    fun interface ResultCallback {
        fun onResult(result: Result)
    }

    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
    @IntDef(INTERNAL_ERROR, DEVELOPER_ERROR, NETWORK_ERROR)
    annotation class ErrorCode

    companion object {
        const val INTERNAL_ERROR = 1
        const val DEVELOPER_ERROR = 2
        const val NETWORK_ERROR = 3
    }
}
