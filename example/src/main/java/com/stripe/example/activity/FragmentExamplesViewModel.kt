package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.example.module.BackendApiFactory
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONObject

class FragmentExamplesViewModel(
    application: Application
) : AndroidViewModel(application) {
    val paymentIntentResultLiveData = MutableLiveData<Result<PaymentIntentResult>>()
    val setupIntentResultLiveData = MutableLiveData<Result<SetupIntentResult>>()

    private val compositeDisposable = CompositeDisposable()
    private val backendApi = BackendApiFactory(application.applicationContext).create()

    fun createPaymentIntent(): LiveData<JSONObject> {
        return createIntent(
            backendApi.createPaymentIntent(PARAMS)
        )
    }

    fun createSetupIntent(): LiveData<JSONObject> {
        return createIntent(
            backendApi.createSetupIntent(PARAMS)
        )
    }

    private fun createIntent(intentSingle: Single<ResponseBody>): LiveData<JSONObject> {
        val result = MutableLiveData<JSONObject>()
        compositeDisposable.add(
            intentSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { responseBody, _ ->
                    result.value = JSONObject(responseBody.string())
                }
        )
        return result
    }

    fun dispose() {
        compositeDisposable.dispose()
    }

    private companion object {
        private val PARAMS = mutableMapOf("country" to "us")
    }
}
