package com.stripe.hcaptcha

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.hcaptcha.task.OnFailureListener
import com.stripe.hcaptcha.task.OnLoadedListener
import com.stripe.hcaptcha.task.OnOpenListener
import com.stripe.hcaptcha.task.OnSuccessListener

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface IHCaptchaVerifier : OnLoadedListener, OnOpenListener, OnSuccessListener<String>, OnFailureListener {
    /**
     * Starts the human verification process.
     */
    fun startVerification(context: Context)

    /**
     * Force stop verification and release resources.
     */
    fun reset()
}
