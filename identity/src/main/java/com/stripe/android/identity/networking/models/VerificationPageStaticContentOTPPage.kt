package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
// TODO(ccen) WIP - to read from server
internal data class VerificationPageStaticContentOTPPage(
    @SerialName("title")
    val title: String = "Enter verification code",
    @SerialName("body")
    val body: String = "Enter the code sent to you phone %redacted_phone_number% to continue.",
    @SerialName("redacted_phone_number")
    val redactedPhoneNumber: String? = "(***)*****35",
    @SerialName("error_otp_message")
    val errorOtpMessage: String = "Error confirming verification code",
    @SerialName("resend_button_text")
    val resendButtonText: String = "Resend code",
    @SerialName("cannot_verify_button_text")
    val cannotVerifyButtonText: String = "I cannot verify this phone number",
) : Parcelable
