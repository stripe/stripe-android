package com.stripe.android.crypto.onramp.samsungpay

import com.stripe.android.crypto.onramp.exception.SamsungPayException.Reason
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class SamsungPayReflection(
    private val classProvider: SamsungPayClassProvider,
) {
    fun loadClass(name: String): Class<*> = classProvider.loadClass(name)

    fun newInstance(
        className: String,
        vararg arguments: Pair<Class<*>, Any?>,
    ): Any {
        return loadClass(className)
            .getConstructor(*arguments.parameterTypes())
            .newInstance(*arguments.values())
    }

    fun invoke(
        target: Any,
        methodName: String,
        vararg arguments: Pair<Class<*>, Any?>,
    ): Any? {
        return target.javaClass
            .getMethod(methodName, *arguments.parameterTypes())
            .invoke(target, *arguments.values())
    }

    fun createProxy(
        interfaceClass: Class<*>,
        handler: (proxy: Any, method: Method, arguments: Array<out Any?>?) -> Any?,
    ): Any {
        return Proxy.newProxyInstance(
            interfaceClass.classLoader,
            arrayOf(interfaceClass),
        ) { proxy, method, arguments ->
            handler(proxy, method, arguments)
        }
    }

    fun enumConstant(className: String, constantName: String): Any {
        return loadClass(className).enumConstants
            ?.firstOrNull { (it as Enum<*>).name == constantName }
            ?: throw NoSuchFieldException("$className.$constantName")
    }

    fun staticInt(className: String, fieldName: String): Int {
        return loadClass(className).getField(fieldName).getInt(null)
    }

    fun staticString(className: String, fieldName: String): String {
        return loadClass(className).getField(fieldName).get(null) as String
    }

    fun handleProxyObjectMethod(
        proxy: Any,
        method: Method,
        arguments: Array<out Any?>?,
    ): Any? {
        return when (method.name) {
            "equals" -> proxy === arguments?.getOrNull(0)
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "SamsungPayReflectionProxy(${proxy.javaClass.interfaces.joinToString { it.name }})"
            else -> null
        }
    }

    fun <T> runOperation(
        operation: String,
        block: () -> T,
    ): Result<T> {
        return runCatching(block).recoverCatching { throwable ->
            val cause = throwable.unwrapInvocationTarget()
            throw when (cause) {
                is SamsungPayException -> cause
                is ClassNotFoundException,
                is NoClassDefFoundError -> SamsungPayException(
                    message = "Samsung Pay SDK 2.22.00 is not available. " +
                        "The client app must include the Samsung Pay SDK JAR.",
                    cause = cause,
                    errorCode = null,
                    reason = Reason.SdkUnavailable,
                )
                is NoSuchMethodException,
                is NoSuchFieldException,
                is LinkageError -> SamsungPayException(
                    message =
                        "The installed Samsung Pay SDK is incompatible with the required 2.22.00 API while $operation.",
                    cause = cause,
                    errorCode = null,
                    reason = Reason.SdkIncompatible,
                )
                else -> SamsungPayException(
                    message = buildString {
                        append("Samsung Pay failed while $operation.")
                        cause.message?.takeIf(String::isNotBlank)?.let { append(" $it") }
                    },
                    cause = cause,
                    errorCode = null,
                    reason = Reason.OperationFailed,
                )
            }
        }
    }

    private fun Array<out Pair<Class<*>, Any?>>.parameterTypes(): Array<Class<*>> {
        return map { it.first }.toTypedArray()
    }

    private fun Array<out Pair<Class<*>, Any?>>.values(): Array<Any?> {
        return map { it.second }.toTypedArray()
    }

    private fun Throwable.unwrapInvocationTarget(): Throwable {
        var current = this
        while (current is InvocationTargetException && current.targetException != null) {
            current = current.targetException
        }
        return current
    }
}

internal object SamsungPaySdkClassNames {
    const val PARTNER_INFO = "com.samsung.android.sdk.samsungpay.v2.PartnerInfo"
    const val SPAY_SDK = "com.samsung.android.sdk.samsungpay.v2.SpaySdk"
    const val SERVICE_TYPE = "com.samsung.android.sdk.samsungpay.v2.SpaySdk\$ServiceType"
    const val SDK_API_LEVEL = "com.samsung.android.sdk.samsungpay.v2.SpaySdk\$SdkApiLevel"
    const val BRAND = "com.samsung.android.sdk.samsungpay.v2.SpaySdk\$Brand"
    const val SAMSUNG_PAY = "com.samsung.android.sdk.samsungpay.v2.SamsungPay"
    const val STATUS_LISTENER = "com.samsung.android.sdk.samsungpay.v2.StatusListener"
    const val PAYMENT_MANAGER = "com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager"
    const val CUSTOM_SHEET_LISTENER =
        "com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager\$CustomSheetTransactionInfoListener"
    const val CUSTOM_SHEET_PAYMENT_INFO =
        "com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo"
    const val CUSTOM_SHEET_PAYMENT_INFO_BUILDER =
        "com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo\$Builder"
    const val ADDRESS_IN_PAYMENT_SHEET =
        "com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo\$AddressInPaymentSheet"
    const val CUSTOM_SHEET = "com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet"
    const val SHEET_CONTROL = "com.samsung.android.sdk.samsungpay.v2.payment.sheet.SheetControl"
    const val AMOUNT_BOX_CONTROL = "com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountBoxControl"
    const val AMOUNT_CONSTANTS = "com.samsung.android.sdk.samsungpay.v2.payment.sheet.AmountConstants"
}
