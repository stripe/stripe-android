package com.stripe.hcaptcha.task

import androidx.annotation.RestrictTo

/**
 * A success listener class
 *
 * @param <TResult> expected result type
</TResult> */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface OnSuccessListener<TResult> {
    /**
     * Called when the challenge is successfully completed
     *
     * @param result the hCaptcha token result
     */
    fun onSuccess(result: TResult)
}
