package com.stripe.hcaptcha.task

/**
 * A success listener class
 *
 * @param <TResult> expected result type
</TResult> */
interface OnSuccessListener<TResult> {
    /**
     * Called when the challenge is successfully completed
     *
     * @param result the hCaptcha token result
     */
    fun onSuccess(result: TResult)
}
