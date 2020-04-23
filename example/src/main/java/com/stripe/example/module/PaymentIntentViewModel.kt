package com.stripe.example.module

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.stripe.example.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.HttpException

internal class PaymentIntentViewModel(
    application: Application
) : AndroidViewModel(application) {
    val inProgress = MutableLiveData<Boolean>()
    val status = MutableLiveData<String>()

    private val context = application.applicationContext
    private val backendApi = BackendApiFactory(context).create()
    private val compositeSubscription = CompositeDisposable()

    fun createPaymentIntent(
        country: String,
        callback: (JSONObject) -> Unit
    ) {
        compositeSubscription.add(
            backendApi.createPaymentIntent(
                mutableMapOf(
                    "country" to country
                )
            )
                .doOnSubscribe {
                    inProgress.postValue(true)
                    status.postValue(context.getString(R.string.creating_payment_intent))
                }
                .map {
                    JSONObject(it.string())
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        status.postValue(status.value + "\n\n" +
                            context.getString(R.string.payment_intent_status,
                                it.getString("status")))
                        callback(it)
                    },
                    {
                        val errorMessage =
                            (it as? HttpException)?.response()?.errorBody()?.string()
                                ?: it.message
                        status.postValue(status.value + "\n\n$errorMessage")
                        inProgress.postValue(false)
                    }
                ))
    }

    override fun onCleared() {
        super.onCleared()
        compositeSubscription.dispose()
    }
}
