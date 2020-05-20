package com.stripe.example.module

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.example.R
import com.stripe.example.activity.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException

internal class StripeIntentViewModel(
    application: Application
) : BaseViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    val paymentIntentResultLiveData = MutableLiveData<Result<PaymentIntentResult>>()
    val setupIntentResultLiveData = MutableLiveData<Result<SetupIntentResult>>()

    fun createPaymentIntent(
        country: String
    ): LiveData<Result<JSONObject>> {
        return makeBackendRequest(
            R.string.creating_payment_intent,
            R.string.payment_intent_status
        ) {
            backendApi.createPaymentIntent(
                mutableMapOf("country" to country)
            )
        }
    }

    fun createSetupIntent(
        country: String
    ): LiveData<Result<JSONObject>> {
        return makeBackendRequest(
            R.string.creating_setup_intent,
            R.string.setup_intent_status
        ) {
            backendApi.createSetupIntent(
                mutableMapOf("country" to country)
            )
        }
    }

    private fun makeBackendRequest(
        @StringRes creatingStringRes: Int,
        @StringRes resultStringRes: Int,
        apiMethod: suspend () -> ResponseBody
    ): LiveData<Result<JSONObject>> {
        val liveData = MutableLiveData<Result<JSONObject>>()

        inProgress.value = true
        status.value = context.getString(creatingStringRes)

        workScope.launch {
            val result = runCatching {
                JSONObject(apiMethod().string())
            }

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        val intentStatus = context.getString(
                            resultStringRes,
                            it.getString("status")
                        )
                        status.postValue(
                            """
                            ${status.value}
                            
                            
                            $intentStatus
                            """.trimIndent()
                        )
                    },
                    onFailure = {
                        val errorMessage =
                            (it as? HttpException)?.response()?.errorBody()?.string()
                                ?: it.message
                        status.postValue(
                            """
                            ${status.value}
                            
                            
                            $errorMessage
                            """.trimIndent()
                        )
                        inProgress.value = false
                    }
                )

                liveData.value = result
            }
        }

        return liveData
    }
}
