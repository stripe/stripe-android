package com.stripe.hcaptcha.task

import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import com.stripe.hcaptcha.HCaptchaError
import com.stripe.hcaptcha.HCaptchaException
import kotlin.time.Duration

/**
 * Generic task definition which allows registration of listeners for the result/error of the task.
 *
 * @param TResult The result type of the task.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class Task<TResult> protected constructor() {
    /**
     * @return if current task is complete or not
     */
    private var isComplete = false

    /**
     * @return if current task is successful or not
     */
    private var isSuccessful = false

    /**
     * @return the result object
     */
    private var result: TResult? = null

    /**
     * @return the exception if any
     */
    private var exception: HCaptchaException? = null
    private val onSuccessListeners: MutableList<OnSuccessListener<TResult>>
    private val onFailureListeners: MutableList<OnFailureListener>
    private val onOpenListeners: MutableList<OnOpenListener>
    protected val handler = Handler(Looper.getMainLooper())

    /**
     * Creates a new Task object
     */
    init {
        onSuccessListeners = ArrayList()
        onFailureListeners = ArrayList()
        onOpenListeners = ArrayList()
        reset()
    }

    private fun reset() {
        isComplete = false
        isSuccessful = false
        result = null
        exception = null
    }

    /**
     * Sets the task result and marks it as successfully completed
     *
     * @param result the result object
     */
    protected fun setResult(result: TResult) {
        this.result = result
        isSuccessful = true
        isComplete = true
        tryCallbacks()
    }

    /**
     * Sets the task exception and marks it as not successfully completed
     *
     * @param exception the hCaptcha exception
     */
    protected fun setException(exception: HCaptchaException) {
        this.exception = exception
        isSuccessful = false
        isComplete = true
        tryCallbacks()
    }

    /**
     * Internal callback which called once 'open-callback' fired in js SDK
     */
    protected fun captchaOpened() {
        onOpenListeners.forEach {
            it.onOpen()
        }
    }

    /**
     * Schedule timer to expire the token.
     * @param tokenExpiration - token expiration timeout
     */
    protected fun scheduleCaptchaExpired(tokenExpiration: Duration) {
        handler.postDelayed({
            for (listener in onFailureListeners) {
                listener.onFailure(HCaptchaException(HCaptchaError.TOKEN_TIMEOUT))
            }
        }, tokenExpiration.inWholeMilliseconds)
    }

    /**
     * Add a success listener triggered when the task finishes successfully
     *
     * @param onSuccessListener the success listener to be triggered
     * @return current object
     */
    fun addOnSuccessListener(onSuccessListener: OnSuccessListener<TResult>): Task<TResult> {
        onSuccessListeners.add(onSuccessListener)
        tryCallbacks()
        return this
    }

    /**
     * Remove a success listener
     * @param onSuccessListener the success listener to be removed
     * @return current object
     */
    fun removeOnSuccessListener(onSuccessListener: OnSuccessListener<TResult>): Task<TResult> {
        onSuccessListeners.remove(onSuccessListener)
        return this
    }

    /**
     * Add a failure listener triggered when the task finishes with an exception
     *
     * @param onFailureListener the failure listener to be triggered
     * @return current object
     */
    fun addOnFailureListener(onFailureListener: OnFailureListener): Task<TResult> {
        onFailureListeners.add(onFailureListener)
        tryCallbacks()
        return this
    }

    /**
     * Remove a failure listener
     * @param onFailureListener to be removed
     * @return current object
     */
    fun removeOnFailureListener(onFailureListener: OnFailureListener): Task<TResult> {
        onFailureListeners.remove(onFailureListener)
        return this
    }

    /**
     * Add a hCaptcha open listener triggered when the hCaptcha View is displayed
     *
     * @param onOpenListener the open listener to be triggered
     * @return current object
     */
    fun addOnOpenListener(onOpenListener: OnOpenListener): Task<TResult> {
        onOpenListeners.add(onOpenListener)
        tryCallbacks()
        return this
    }

    /**
     * Remove a open listener
     * @param onOpenListener to be removed
     * @return current object
     */
    fun removeOnOpenListener(onOpenListener: OnOpenListener): Task<TResult> {
        onOpenListeners.remove(onOpenListener)
        return this
    }

    /**
     * Remove all listeners: success, failure and open listeners
     * @return current object
     */
    fun removeAllListeners(): Task<TResult> {
        onSuccessListeners.clear()
        onFailureListeners.clear()
        onOpenListeners.clear()
        return this
    }

    private fun tryCallbacks() {
        var shouldReset = false

        result?.let { e ->
            onSuccessListeners.forEach {
                it.onSuccess(e)
                shouldReset = true
            }
        }

        exception?.let { e ->
            onFailureListeners.forEach {
                it.onFailure(e)
                shouldReset = true
            }
        }

        if (shouldReset) {
            reset()
        }
    }
}
