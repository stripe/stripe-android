package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentOTPPage(
    @SerialName("title")
    val title: String,
    @SerialName("body")
    val body: String,
    @SerialName("redacted_phone_number")
    val redactedPhoneNumber: String?,
    @SerialName("error_otp_message")
    val errorOtpMessage: String,
    @SerialName("resend_button_text")
    val resendButtonText: String,
    @SerialName("cannot_verify_button_text")
    val cannotVerifyButtonText: String,
) : Parcelable
