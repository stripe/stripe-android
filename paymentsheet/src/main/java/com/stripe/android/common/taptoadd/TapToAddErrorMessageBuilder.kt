package com.stripe.android.common.taptoadd

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import java.io.IOException
import com.stripe.android.core.R as StripeCoreR

internal object TapToAddErrorMessageBuilder {
    fun build(error: Throwable): TapToAddErrorMessage {
        return when (error) {
            is TerminalException -> buildFromTerminalError(error)
            is TapToAddCardNotSupportedException -> TapToAddErrorMessage(
                title = R.string.stripe_tap_to_add_card_not_supported_error_title.resolvableString,
                action = R.string.stripe_tap_to_add_unusable_error_action.resolvableString,
            )
            is IOException -> TapToAddErrorMessage(
                title = StripeCoreR.string.stripe_network_error.resolvableString,
                action = R.string.stripe_tap_to_add_network_error_action.resolvableString,
            )
            else -> TapToAddErrorMessage(
                title = StripeCoreR.string.stripe_error.resolvableString,
                action = StripeCoreR.string.stripe_try_again_later.resolvableString,
            )
        }
    }

    private fun buildFromTerminalError(error: TerminalException): TapToAddErrorMessage {
        return when (error.errorCode) {
            TerminalErrorCode.DECLINED_BY_READER,
            TerminalErrorCode.DECLINED_BY_STRIPE_API -> TapToAddErrorMessage(
                title = R.string.stripe_tap_to_add_card_declined_error_title.resolvableString,
                action = R.string.stripe_tap_to_add_unusable_error_action.resolvableString,
            )
            TerminalErrorCode.TAP_TO_PAY_DEVICE_TAMPERED,
            TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE -> TapToAddErrorMessage(
                title = R.string.stripe_tap_to_add_unsupported_device_error_title.resolvableString,
                action = R.string.stripe_tap_to_add_unsupported_device_error_action.resolvableString,
            )
            TerminalErrorCode.CARD_READ_TIMED_OUT -> TapToAddErrorMessage(
                title = StripeCoreR.string.stripe_timed_out.resolvableString,
                action = R.string.stripe_tap_to_add_timed_out_error_action.resolvableString,
            )
            /*
             * Unfortunately there is no specific error for developer options and insecure environment can be map to
             * another error type during reader connection. We can check the message returned by the error which is
             * untranslated here:
             *
             * https://stripe.sourcegraphcloud.com/r/stripe-internal/android/-/blob/terminalsdk/cots/modules/common/src/main/kotlin/com/stripe/cots/common/CotsError.kt?L13-16
             */
            TerminalErrorCode.TAP_TO_PAY_INSECURE_ENVIRONMENT
                if error.errorMessage.contains(DEVELOPER_OPTIONS_SUBSTRING) -> {
                TapToAddErrorMessage(
                    title = StripeCoreR.string.stripe_error.resolvableString,
                    action = R.string.stripe_tap_to_add_developer_options_error_action.resolvableString,
                )
            }
            else -> TapToAddErrorMessage(
                title = StripeCoreR.string.stripe_error.resolvableString,
                action = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
            )
        }
    }

    private const val DEVELOPER_OPTIONS_SUBSTRING = "Developer Options"
}
