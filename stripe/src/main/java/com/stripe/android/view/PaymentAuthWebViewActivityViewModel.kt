package com.stripe.android.view

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiRequest
import com.stripe.android.ApiResultCallback
import com.stripe.android.StripeRepository
import com.stripe.android.model.StripeIntent

internal class PaymentAuthWebViewActivityViewModel internal constructor(
    private val stripeRepository: StripeRepository
) : ViewModel() {

    @JvmSynthetic
    internal val intent: MutableLiveData<Result<*>> = MutableLiveData()

    @JvmSynthetic
    internal fun startCancelIntentSource(
        clientSecret: String,
        sourceId: String,
        options: ApiRequest.Options
    ) {
        stripeRepository.retrieveIntent(clientSecret, options,
            object : ApiResultCallback<StripeIntent> {
                override fun onSuccess(result: StripeIntent) {
                    if (result.requiresAction()) {
                        // only cancel the intent if its status is still `requires_action`
                        cancelIntent(result, sourceId, options)
                    } else {
                        intent.value = Result.create(result)
                    }
                }

                override fun onError(e: Exception) {
                    intent.value = Result.create(e)
                }
            })
    }

    private fun cancelIntent(
        stripeIntent: StripeIntent,
        sourceId: String,
        options: ApiRequest.Options
    ) {
        stripeRepository.cancelIntent(stripeIntent, sourceId, options,
            object : ApiResultCallback<StripeIntent> {
                override fun onSuccess(result: StripeIntent) {
                    intent.value = Result.create(result)
                }

                override fun onError(e: Exception) {
                    intent.value = Result.create(e)
                }
            })
    }

    internal class Factory(
        private val stripeRepository: StripeRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PaymentAuthWebViewActivityViewModel(stripeRepository) as T
        }
    }

    internal data class Result<out T> internal constructor(
        internal val status: Status,
        internal val data: T
    ) {
        enum class Status {
            SUCCESS, ERROR
        }

        internal companion object {
            @JvmSynthetic
            internal fun create(stripeIntent: StripeIntent): Result<StripeIntent> {
                return Result(Status.SUCCESS, stripeIntent)
            }

            @JvmSynthetic
            internal fun create(exception: Exception): Result<Exception> {
                return Result(Status.ERROR, exception)
            }
        }
    }
}
