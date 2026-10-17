package com.stripe.android.common.taptoadd

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import com.stripe.android.core.R as StripeCoreR

@RunWith(RobolectricTestRunner::class)
internal class TapToAddErrorMessageBuilderTest {

    @Test
    fun `build returns card not supported copy for TapToAddCardNotSupportedException`() {
        val error = TapToAddCardNotSupportedException()
        val expected = TapToAddErrorMessage(
            title = R.string.stripe_tap_to_add_card_not_supported_error_title.resolvableString,
            action = R.string.stripe_tap_to_add_unusable_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns network copy for IOException`() {
        val error = IOException("timeout")
        val expected = TapToAddErrorMessage(
            title = StripeCoreR.string.stripe_network_error.resolvableString,
            action = R.string.stripe_tap_to_add_network_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns generic copy for unknown error`() {
        val error = RuntimeException("oops")
        val expected = TapToAddErrorMessage(
            title = StripeCoreR.string.stripe_error.resolvableString,
            action = StripeCoreR.string.stripe_try_again_later.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns declined copy for DECLINED_BY_READER`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.DECLINED_BY_READER,
            errorMessage = "nope",
        )
        val expected = TapToAddErrorMessage(
            title = R.string.stripe_tap_to_add_card_declined_error_title.resolvableString,
            action = R.string.stripe_tap_to_add_unusable_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns declined copy for DECLINED_BY_STRIPE_API`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.DECLINED_BY_STRIPE_API,
            errorMessage = "declined",
        )
        val expected = TapToAddErrorMessage(
            title = R.string.stripe_tap_to_add_card_declined_error_title.resolvableString,
            action = R.string.stripe_tap_to_add_unusable_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns unsupported device copy for TAP_TO_PAY_DEVICE_TAMPERED`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.TAP_TO_PAY_DEVICE_TAMPERED,
            errorMessage = "tampered",
        )
        val expected = TapToAddErrorMessage(
            title = R.string.stripe_tap_to_add_unsupported_device_error_title.resolvableString,
            action = R.string.stripe_tap_to_add_unsupported_device_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns unsupported device copy for TAP_TO_PAY_UNSUPPORTED_DEVICE`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE,
            errorMessage = "bad device",
        )
        val expected = TapToAddErrorMessage(
            title = R.string.stripe_tap_to_add_unsupported_device_error_title.resolvableString,
            action = R.string.stripe_tap_to_add_unsupported_device_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns timed out copy for CARD_READ_TIMED_OUT`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.CARD_READ_TIMED_OUT,
            errorMessage = "slow",
        )
        val expected = TapToAddErrorMessage(
            title = StripeCoreR.string.stripe_timed_out.resolvableString,
            action = R.string.stripe_tap_to_add_timed_out_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns default terminal fallback for other TerminalErrorCode`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.UNEXPECTED_SDK_ERROR,
            errorMessage = "weird",
        )
        val expected = TapToAddErrorMessage(
            title = StripeCoreR.string.stripe_error.resolvableString,
            action = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns expected error for TAP_TO_PAY_INSECURE_ENVIRONMENT when message mentions developer options`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.TAP_TO_PAY_INSECURE_ENVIRONMENT,
            errorMessage = "Insecure: Developer Options enabled on device",
        )
        val expected = TapToAddErrorMessage(
            title = StripeCoreR.string.stripe_error.resolvableString,
            action = R.string.stripe_tap_to_add_developer_options_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }

    @Test
    fun `build returns expected error for TAP_TO_PAY_INSECURE_ENVIRONMENT when message has no developer options`() {
        val error = TerminalException(
            errorCode = TerminalErrorCode.TAP_TO_PAY_INSECURE_ENVIRONMENT,
            errorMessage = "insecure environment",
        )
        val expected = TapToAddErrorMessage(
            title = StripeCoreR.string.stripe_error.resolvableString,
            action = R.string.stripe_tap_to_add_card_default_error_action.resolvableString,
        )
        assertThat(TapToAddErrorMessageBuilder.build(error)).isEqualTo(expected)
    }
}
