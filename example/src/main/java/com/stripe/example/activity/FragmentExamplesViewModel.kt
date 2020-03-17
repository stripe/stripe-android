package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.example.module.BackendApiFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.util.HashMap

class FragmentExamplesViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val compositeDisposable = CompositeDisposable()
    private val backendApi = BackendApiFactory(application.applicationContext).create()

    fun createPaymentIntent(): LiveData<JSONObject> {
        val result = MutableLiveData<JSONObject>()
        compositeDisposable.add(
            backendApi.createPaymentIntent(createPaymentIntentParams())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    result.value = JSONObject(it.string())
                }
        )
        return result
    }

    fun createSetupIntent(): LiveData<JSONObject> {
        val result = MutableLiveData<JSONObject>()
        compositeDisposable.add(
            backendApi.createSetupIntent(hashMapOf("country" to "us"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    result.value = JSONObject(it.string())
                }
        )
        return result
    }

    fun dispose() {
        compositeDisposable.dispose()
    }

    private fun createPaymentIntentParams(): HashMap<String, Any> {
        return hashMapOf(
            "payment_method_types[]" to "card",
            "amount" to 1000,
            "country" to "us"
        )
    }
}
