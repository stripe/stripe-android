package com.stripe.hcaptcha

import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.task.OnFailureListener
import com.stripe.hcaptcha.task.OnLoadedListener
import com.stripe.hcaptcha.task.OnOpenListener
import com.stripe.hcaptcha.task.OnSuccessListener

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface IHCaptchaVerifier : OnLoadedListener, OnOpenListener, OnSuccessListener<String>, OnFailureListener {
    /**
     * Starts the human verification process.
     */
    fun startVerification(activity: FragmentActivity)

    /**
     * Force stop verification and release resources.
     */
    fun reset()
}
