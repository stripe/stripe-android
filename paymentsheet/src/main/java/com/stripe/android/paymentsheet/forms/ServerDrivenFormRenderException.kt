package com.stripe.android.paymentsheet.forms

internal class ServerDrivenFormRenderException(
    val errorCode: ErrorCode,
    cause: Throwable? = null,
) : IllegalArgumentException("Unable to render Mobile Session form: ${errorCode.analyticsValue}", cause) {
    internal enum class ErrorCode(val analyticsValue: String) {
        MissingFormSpec("missing_form_spec"),
        UnsupportedNativeComponent("unsupported_native_component"),
        NativeComponentFailure("native_component_failure"),
        UnsupportedMandateText("unsupported_mandate_text"),
        FormRenderFailure("form_render_failure"),
    }
}
