package com.stripe.android.testing

import com.stripe.android.model.AndroidVerificationObject
import com.stripe.android.model.RadarOptions

object RadarOptionsFactory {
    fun create(
        hCaptchaToken: String? = "test_token",
        verificationObject: AndroidVerificationObject? = AndroidVerificationObject(
            androidVerificationToken = "test_verification_token",
            appId = "com.stripe.app"
        )
    ): RadarOptions {
        return RadarOptions(
            hCaptchaToken = hCaptchaToken,
            androidVerificationObject = verificationObject
        )
    }
}
