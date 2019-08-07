package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.Card
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.view.CardInputWidget
import com.stripe.example.R
import com.stripe.example.controller.ErrorDialogHandler
import com.stripe.example.controller.ProgressDialogController
import com.stripe.example.module.RetrofitFactory
import com.stripe.example.service.StripeService
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.HashMap

class SetupIntentActivity : AppCompatActivity() {
    private val compositeDisposable = CompositeDisposable()

    private lateinit var stripe: Stripe
    private lateinit var stripeService: StripeService
    private lateinit var progressDialogController: ProgressDialogController
    private lateinit var errorDialogHandler: ErrorDialogHandler
    private lateinit var createPaymentMethod: Button
    private lateinit var confirmSetupIntent: Button
    private lateinit var retrieveSetupIntent: Button
    private lateinit var cardInputWidget: CardInputWidget
    private lateinit var setupIntentValue: TextView

    private var clientSecret: String? = null
    private var paymentMethod: PaymentMethod? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_intent_demo)
        val createSetupIntent = findViewById<Button>(R.id.btn_create_setup_intent)
        createPaymentMethod = findViewById(R.id.btn_create_payment_method)
        retrieveSetupIntent = findViewById(R.id.btn_retrieve_setup_intent)
        confirmSetupIntent = findViewById(R.id.btn_confirm_setup_intent)
        setupIntentValue = findViewById(R.id.setup_intent_value)
        cardInputWidget = findViewById(R.id.card_input_widget)

        progressDialogController = ProgressDialogController(supportFragmentManager, resources)
        errorDialogHandler = ErrorDialogHandler(this)

        stripe = Stripe(this,
            PaymentConfiguration.getInstance().publishableKey)
        val retrofit = RetrofitFactory.instance
        stripeService = retrofit.create(StripeService::class.java)

        createSetupIntent.setOnClickListener { createSetupIntent() }
        createPaymentMethod.setOnClickListener {
            val card = cardInputWidget.card
            if (card != null) {
                createPaymentMethod(card)
            }
        }
        retrieveSetupIntent.setOnClickListener { retrieveSetupIntent() }
        confirmSetupIntent.setOnClickListener { confirmSetupIntent() }
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        stripe.onSetupResult(requestCode, data, object : ApiResultCallback<SetupIntentResult> {
            override fun onSuccess(result: SetupIntentResult) {
                clientSecret = result.intent.clientSecret
                displaySetupIntent(result.intent)
            }

            override fun onError(e: Exception) {
                Toast.makeText(this@SetupIntentActivity,
                    "Error: " + e.localizedMessage, Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun createSetupIntent() {
        val disposable = stripeService.createSetupIntent(HashMap())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { progressDialogController.show(R.string.creating_setup_intent) }
            .doOnComplete { progressDialogController.dismiss() }

            // Because we've made the mapping above, we're now subscribing
            // to the result of creating a 3DS Source
            .subscribe(
                { onCreatedSetupIntent(it) },
                { throwable -> errorDialogHandler.show(throwable.localizedMessage) }
            )
        compositeDisposable.add(disposable)
    }

    private fun onCreatedSetupIntent(responseBody: ResponseBody) {
        try {
            val jsonObject = JSONObject(responseBody.string())
            setupIntentValue.text = jsonObject.toString()
            clientSecret = jsonObject.getString("secret")
            createPaymentMethod.isEnabled = clientSecret != null
        } catch (exception: IOException) {
            Log.e(TAG, exception.toString())
        } catch (exception: JSONException) {
            Log.e(TAG, exception.toString())
        }
    }

    private fun retrieveSetupIntent() {
        val setupIntentObservable = Observable.fromCallable {
            stripe.retrieveSetupIntentSynchronous(clientSecret!!)!!
        }

        val disposable = setupIntentObservable
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { progressDialogController.show(R.string.retrieving_setup_intent) }
            .doOnComplete { progressDialogController.dismiss() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { displaySetupIntent(it) },
                { throwable -> Log.e(TAG, throwable.toString()) }
            )
        compositeDisposable.add(disposable)
    }

    private fun createPaymentMethod(card: Card) {
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null)

        val paymentMethodObservable = Observable.fromCallable {
            stripe.createPaymentMethodSynchronous(paymentMethodCreateParams)
        }

        val disposable = paymentMethodObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { progressDialogController.show(R.string.creating_payment_method) }
            .doOnComplete { progressDialogController.dismiss() }
            .subscribe(
                { paymentMethod ->
                    if (paymentMethod != null) {
                        setupIntentValue.text = paymentMethod.id
                        this.paymentMethod = paymentMethod
                        confirmSetupIntent.isEnabled = true
                        retrieveSetupIntent.isEnabled = true
                    }
                },
                { throwable -> Log.e(TAG, throwable.toString()) }
            )
        compositeDisposable.add(disposable)
    }

    private fun displaySetupIntent(setupIntent: SetupIntent) {
        val displayText = "Setup Intent status: ${setupIntent.status?.code}"
        setupIntentValue.text = displayText
    }

    private fun confirmSetupIntent() {
        stripe.confirmSetupIntent(this,
            ConfirmSetupIntentParams.create(paymentMethod!!.id!!, clientSecret!!, RETURN_URL))
    }

    companion object {
        private val TAG = SetupIntentActivity::class.java.name
        private const val RETURN_URL = "stripe://setup_intent_return"
    }
}
