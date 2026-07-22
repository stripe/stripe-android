package com.stripe.android.crypto.onramp.samsungpay

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent
import com.stripe.android.crypto.onramp.exception.SamsungPayException.Reason
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import java.lang.reflect.Method

internal class DefaultSamsungPayLauncherFactory(
    private val trackAnalyticsEvent: (OnrampAnalyticsEvent) -> Unit,
) : SamsungPayLauncher.Factory {
    override fun create(
        context: Context,
        configuration: OnrampConfiguration.SamsungPayConfig,
        merchantDisplayName: String,
    ): SamsungPayLauncher {
        return DefaultSamsungPayLauncher(
            context = context.applicationContext,
            configuration = configuration,
            merchantDisplayName = merchantDisplayName,
            trackAnalyticsEvent = trackAnalyticsEvent,
            classProvider = SamsungPayClassProvider { className ->
                Class.forName(className, true, context.classLoader)
            },
        )
    }
}

internal class DefaultSamsungPayLauncher(
    private val context: Context,
    configuration: OnrampConfiguration.SamsungPayConfig,
    merchantDisplayName: String,
    private val trackAnalyticsEvent: (OnrampAnalyticsEvent) -> Unit,
    classProvider: SamsungPayClassProvider,
) : SamsungPayLauncher {
    private val statusCallbacks = mutableListOf<(SamsungPayStatus) -> Unit>()
    private val reflection = SamsungPayReflection(classProvider)
    private val sheetFactory = SamsungPaySheetFactory(reflection, configuration, merchantDisplayName)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var status: SamsungPayStatus? = null
    private var statusRequestInFlight = false
    private var statusClient: Any? = null
    private var statusListener: Any? = null
    private var activePresentation: ActivePresentation? = null
    private var destroyed = false

    init {
        trackAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayInitialized)
    }

    @MainThread
    override fun getStatus(callback: (SamsungPayStatus) -> Unit) {
        status?.let {
            callback(it)
            return
        }

        if (destroyed) {
            callback(
                SamsungPayStatus.Failed(
                    samsungPayError(
                        message = "Samsung Pay launcher has been destroyed.",
                        reason = Reason.OperationFailed,
                        errorCode = null,
                    ),
                ),
            )
            return
        }

        statusCallbacks += callback
        if (statusRequestInFlight) {
            return
        }
        statusRequestInFlight = true

        reflection.runOperation("checking Samsung Pay availability") {
            val partnerInfo = sheetFactory.buildPartnerInfo()
            val statusListenerClass = reflection.loadClass(SamsungPaySdkClassNames.STATUS_LISTENER)
            val samsungPay = reflection.newInstance(
                SamsungPaySdkClassNames.SAMSUNG_PAY,
                Context::class.java to context,
                reflection.loadClass(SamsungPaySdkClassNames.PARTNER_INFO) to partnerInfo,
            )
            val listener = reflection.createProxy(statusListenerClass, ::handleStatusCallback)

            statusClient = samsungPay
            statusListener = listener
            reflection.invoke(
                samsungPay,
                "getSamsungPayStatus",
                statusListenerClass to listener,
            )
        }.onFailure { error ->
            completeStatus(SamsungPayStatus.Failed(error))
        }
    }

    @MainThread
    override fun present(
        presentation: SamsungPayPresentation,
        callback: (SamsungPayResult) -> Unit,
    ) {
        if (destroyed) {
            callback(
                SamsungPayResult.Failed(
                    samsungPayError(
                        message = "Samsung Pay launcher has been destroyed.",
                        reason = Reason.PresentationFailed,
                        errorCode = null,
                    ),
                ),
            )
            return
        }
        if (status !is SamsungPayStatus.Ready) {
            callback(SamsungPayResult.Failed(unavailableError(status)))
            return
        }
        if (activePresentation != null) {
            callback(
                SamsungPayResult.Failed(
                    samsungPayError(
                        message = "A Samsung Pay presentation is already in progress.",
                        reason = Reason.PresentationFailed,
                        errorCode = null,
                    ),
                ),
            )
            return
        }
        val active = ActivePresentation(callback).also { activePresentation = it }

        reflection.runOperation("presenting Samsung Pay") {
            sheetFactory.validateConfiguration()
            val paymentManager = reflection.newInstance(
                SamsungPaySdkClassNames.PAYMENT_MANAGER,
                Context::class.java to context,
                reflection.loadClass(SamsungPaySdkClassNames.PARTNER_INFO) to sheetFactory.buildPartnerInfo(),
            )
            val listenerClass = reflection.loadClass(SamsungPaySdkClassNames.CUSTOM_SHEET_LISTENER)
            val listener = reflection.createProxy(listenerClass) { proxy, method, arguments ->
                handlePaymentCallback(active, paymentManager, proxy, method, arguments)
            }

            active.paymentManager = paymentManager
            active.listener = listener

            reflection.invoke(
                paymentManager,
                "startInAppPayWithCustomSheet",
                reflection.loadClass(SamsungPaySdkClassNames.CUSTOM_SHEET_PAYMENT_INFO) to
                    sheetFactory.buildPaymentInfo(presentation),
                listenerClass to listener,
            )
            trackAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayPresented)
        }.onFailure { error ->
            completePresentation(active, SamsungPayResult.Failed(error))
        }
    }

    @MainThread
    override fun destroy() {
        destroyed = true
        statusCallbacks.clear()
        statusClient = null
        statusListener = null
        activePresentation = null
    }

    private fun handleStatusCallback(
        proxy: Any,
        method: Method,
        arguments: Array<out Any?>?,
    ): Any? {
        if (method.declaringClass == Any::class.java) {
            return reflection.handleProxyObjectMethod(proxy, method, arguments)
        }

        runOnMain {
            reflection.runOperation("handling Samsung Pay availability") {
                when (method.name) {
                    "onSuccess" -> {
                        val statusCode = arguments?.getOrNull(0) as? Int
                            ?: throw samsungPayError(
                                message = "Samsung Pay returned an invalid status callback.",
                                reason = Reason.OperationFailed,
                                errorCode = null,
                            )
                        val data = arguments.getOrNull(1) as? Bundle
                        val mappedStatus = mapStatus(statusCode, data)
                        trackAnalyticsEvent(
                            OnrampAnalyticsEvent.SamsungPayAvailable(
                                available = mappedStatus is SamsungPayStatus.Ready,
                                status = statusCode,
                            ),
                        )
                        completeStatus(mappedStatus)
                    }
                    "onFail" -> {
                        val errorCode = arguments?.getOrNull(0) as? Int
                            ?: throw samsungPayError(
                                message = "Samsung Pay returned an invalid status failure callback.",
                                reason = Reason.OperationFailed,
                                errorCode = null,
                            )
                        trackAnalyticsEvent(
                            OnrampAnalyticsEvent.SamsungPayAvailable(
                                available = false,
                                status = errorCode,
                            ),
                        )
                        completeStatus(
                            SamsungPayStatus.Failed(
                                samsungPayError(
                                    message = "Samsung Pay availability check failed with error code $errorCode.",
                                    reason = Reason.NotReady,
                                    errorCode = errorCode,
                                ),
                            ),
                        )
                    }
                }
            }.onFailure { error ->
                completeStatus(SamsungPayStatus.Failed(error))
            }
        }
        return null
    }

    private fun handlePaymentCallback(
        active: ActivePresentation,
        paymentManager: Any,
        proxy: Any,
        method: Method,
        arguments: Array<out Any?>?,
    ): Any? {
        if (method.declaringClass == Any::class.java) {
            return reflection.handleProxyObjectMethod(proxy, method, arguments)
        }

        runOnMain {
            reflection.runOperation("handling Samsung Pay result") {
                when (method.name) {
                    "onCardInfoUpdated" -> handleCardInfoUpdated(paymentManager, arguments)
                    "onSuccess" -> handlePaymentSuccess(active, arguments)
                    "onFailure" -> handlePaymentFailure(active, arguments)
                }
            }.onFailure { error ->
                completePresentation(active, SamsungPayResult.Failed(error))
            }
        }
        return null
    }

    private fun handleCardInfoUpdated(
        paymentManager: Any,
        arguments: Array<out Any?>?,
    ) {
        val customSheet = arguments?.getOrNull(1)
            ?: throw samsungPayError(
                message = "Samsung Pay did not provide the updated sheet.",
                reason = Reason.PresentationFailed,
                errorCode = null,
            )
        reflection.invoke(
            paymentManager,
            "updateSheet",
            reflection.loadClass(SamsungPaySdkClassNames.CUSTOM_SHEET) to customSheet,
        )
    }

    private fun handlePaymentSuccess(
        active: ActivePresentation,
        arguments: Array<out Any?>?,
    ) {
        val credential = arguments?.getOrNull(1) as? String
        if (credential.isNullOrBlank()) {
            throw samsungPayError(
                message = "Samsung Pay returned an empty payment credential.",
                reason = Reason.CredentialsFailed,
                errorCode = null,
            )
        }
        trackAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayObtainCredentialsSuccess)
        completePresentation(active, SamsungPayResult.Completed(credential))
    }

    private fun handlePaymentFailure(
        active: ActivePresentation,
        arguments: Array<out Any?>?,
    ) {
        val errorCode = arguments?.getOrNull(0) as? Int
        if (errorCode == reflection.staticInt(SamsungPaySdkClassNames.SPAY_SDK, "ERROR_USER_CANCELED")) {
            trackAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayCanceled)
            completePresentation(active, SamsungPayResult.Canceled)
        } else {
            errorCode?.let {
                trackAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayObtainCredentialsFailed(errorCode = it))
            }
            completePresentation(
                active,
                SamsungPayResult.Failed(
                    samsungPayError(
                        message = "Samsung Pay failed with error code $errorCode.",
                        reason = Reason.CredentialsFailed,
                        errorCode = errorCode,
                    ),
                ),
            )
        }
    }

    private fun mapStatus(statusCode: Int, data: Bundle?): SamsungPayStatus {
        return when (statusCode) {
            reflection.staticInt(SamsungPaySdkClassNames.SPAY_SDK, "SPAY_READY") -> SamsungPayStatus.Ready
            reflection.staticInt(SamsungPaySdkClassNames.SPAY_SDK, "SPAY_NOT_SUPPORTED") -> {
                SamsungPayStatus.NotSupported
            }
            reflection.staticInt(SamsungPaySdkClassNames.SPAY_SDK, "SPAY_NOT_ALLOWED_TEMPORALLY") -> {
                SamsungPayStatus.TemporarilyUnavailable
            }
            reflection.staticInt(SamsungPaySdkClassNames.SPAY_SDK, "SPAY_NOT_READY") -> {
                val errorReasonKey = reflection.staticString(
                    SamsungPaySdkClassNames.SAMSUNG_PAY,
                    "EXTRA_ERROR_REASON",
                )
                val reasonCode = data?.takeIf { it.containsKey(errorReasonKey) }?.getInt(errorReasonKey)
                val reason = when (reasonCode) {
                    reflection.staticInt(SamsungPaySdkClassNames.SPAY_SDK, "ERROR_SPAY_SETUP_NOT_COMPLETED") -> {
                        SamsungPayStatus.NotReady.Reason.NeedsUserSetup
                    }
                    reflection.staticInt(SamsungPaySdkClassNames.SPAY_SDK, "ERROR_SPAY_APP_NEED_TO_UPDATE") -> {
                        SamsungPayStatus.NotReady.Reason.NeedsAppUpdate
                    }
                    else -> SamsungPayStatus.NotReady.Reason.Other(reasonCode)
                }
                SamsungPayStatus.NotReady(reason)
            }
            else -> SamsungPayStatus.Failed(
                samsungPayError(
                    message = "Samsung Pay returned unknown status code $statusCode.",
                    reason = Reason.NotReady,
                    errorCode = statusCode,
                ),
            )
        }
    }

    @MainThread
    private fun completeStatus(completedStatus: SamsungPayStatus) {
        if (destroyed || status != null) {
            return
        }
        status = completedStatus
        statusRequestInFlight = false
        statusClient = null
        statusListener = null
        val callbacks = statusCallbacks.toList().also { statusCallbacks.clear() }
        callbacks.forEach { it(completedStatus) }
    }

    @MainThread
    private fun completePresentation(active: ActivePresentation, result: SamsungPayResult) {
        if (destroyed || activePresentation !== active) {
            return
        }
        activePresentation = null
        active.callback(result)
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post { block() }
        }
    }

    private fun unavailableError(currentStatus: SamsungPayStatus?): Throwable {
        return when (currentStatus) {
            is SamsungPayStatus.Failed -> currentStatus.error
            null -> samsungPayError(
                message = "Samsung Pay readiness has not completed.",
                reason = Reason.NotReady,
                errorCode = null,
            )
            else -> samsungPayError(
                message = "Samsung Pay is not available: $currentStatus",
                reason = Reason.NotReady,
                errorCode = null,
            )
        }
    }

    private fun samsungPayError(
        message: String,
        reason: Reason,
        errorCode: Int?,
    ): SamsungPayException {
        return SamsungPayException(
            message = message,
            cause = null,
            errorCode = errorCode,
            reason = reason,
        )
    }

    private class ActivePresentation(
        val callback: (SamsungPayResult) -> Unit,
    ) {
        var paymentManager: Any? = null
        var listener: Any? = null
    }
}
