package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.Card
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardInputWidget
import com.stripe.example.R
import com.stripe.example.controller.ErrorDialogHandler
import com.stripe.example.controller.ProgressDialogController
import com.stripe.example.module.RetrofitFactory
import com.stripe.example.service.BackendApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.HashMap

class PaymentIntentActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()

    private lateinit var stripe: Stripe
    private lateinit var backendApi: BackendApi
    private lateinit var progressDialogController: ProgressDialogController
    private lateinit var errorDialogHandler: ErrorDialogHandler
    private lateinit var confirmPaymentIntent: Button
    private lateinit var retrievePaymentIntent: Button
    private lateinit var cardInputWidget: CardInputWidget
    private lateinit var paymentIntentValue: TextView

    private var clientSecret: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_intent_demo)
        val createPaymentIntent = findViewById<Button>(R.id.btn_create_payment_intent)
        retrievePaymentIntent = findViewById(R.id.btn_retrieve_payment_intent)
        confirmPaymentIntent = findViewById(R.id.btn_confirm_payment_intent)
        paymentIntentValue = findViewById(R.id.payment_intent_value)
        cardInputWidget = findViewById(R.id.card_input_widget)

        progressDialogController = ProgressDialogController(supportFragmentManager, resources)
        errorDialogHandler = ErrorDialogHandler(this)
        stripe = Stripe(applicationContext, PaymentConfiguration.getInstance().publishableKey)
        val retrofit = RetrofitFactory.instance
        backendApi = retrofit.create(BackendApi::class.java)

        createPaymentIntent.setOnClickListener { createPaymentIntent() }

        retrievePaymentIntent.setOnClickListener { retrievePaymentIntent() }
        confirmPaymentIntent.setOnClickListener {
            cardInputWidget.card?.let {
                confirmPaymentIntent(it)
            }
        }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        stripe.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {
                clientSecret = result.intent.clientSecret
                displayPaymentIntent(result.intent)
            }

            override fun onError(e: Exception) {
                Toast.makeText(this@PaymentIntentActivity,
                    "Error: " + e.localizedMessage, Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun createPaymentIntentParams(): HashMap<String, Any> {
        return hashMapOf(
            "payment_method_types[]" to "card",
            "amount" to 1000,
            "currency" to "usd"
        )
    }

    private fun createPaymentIntent() {
        val params = createPaymentIntentParams()
        val disposable = backendApi.createPaymentIntent(params)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { progressDialogController.show(R.string.creating_payment_intent) }
            .doOnComplete { progressDialogController.dismiss() }

            // Because we've made the mapping above, we're now subscribing
            // to the result of creating a 3DS Source
            .subscribe(
                { onCreatedPaymentIntent(it) },
                { throwable -> errorDialogHandler.show(throwable.localizedMessage) }
            )
        compositeDisposable.add(disposable)
    }

    private fun onCreatedPaymentIntent(responseBody: ResponseBody) {
        try {
            val jsonObject = JSONObject(responseBody.string())
            paymentIntentValue.text = jsonObject.toString()
            clientSecret = jsonObject.getString("secret")
            confirmPaymentIntent.isEnabled = clientSecret != null
            retrievePaymentIntent.isEnabled = clientSecret != null
        } catch (exception: IOException) {
            Log.e(TAG, exception.toString())
        } catch (exception: JSONException) {
            Log.e(TAG, exception.toString())
        }
    }

    private fun retrievePaymentIntent() {
        val paymentIntentObservable = Observable.fromCallable {
            stripe.retrievePaymentIntentSynchronous(clientSecret!!)!!
        }
        val disposable = paymentIntentObservable
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { progressDialogController.show(R.string.retrieving_payment_intent) }
            .doOnComplete { progressDialogController.dismiss() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { displayPaymentIntent(it) },
                { throwable -> Log.e(TAG, throwable.toString()) }
            )
        compositeDisposable.add(disposable)
    }

    private fun displayPaymentIntent(paymentIntent: PaymentIntent) {
        val displayText = "Payment Intent status: ${paymentIntent.status?.code}"
        paymentIntentValue.text = displayText
    }

    private fun confirmPaymentIntent(card: Card) {
        stripe.confirmPayment(this,
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null),
                clientSecret!!, RETURN_URL))
    }

    companion object {
        private val TAG = PaymentIntentActivity::class.java.name

        private const val RETURN_URL = "stripe://payment_intent_return"
    }
}
