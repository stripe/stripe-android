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

class PaymentIntentActivity : AppCompatActivity() {

    private val mCompositeDisposable = CompositeDisposable()

    private var mProgressDialogController: ProgressDialogController? = null
    private var mErrorDialogHandler: ErrorDialogHandler? = null
    private var mStripe: Stripe? = null
    private var mStripeService: StripeService? = null
    private var mClientSecret: String? = null
    private var mConfirmPaymentIntent: Button? = null
    private var mRetrievePaymentIntent: Button? = null
    private var mCardInputWidget: CardInputWidget? = null
    private var mPaymentIntentValue: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_intent_demo)
        val createPaymentIntent = findViewById<Button>(R.id.btn_create_payment_intent)
        mRetrievePaymentIntent = findViewById(R.id.btn_retrieve_payment_intent)
        mConfirmPaymentIntent = findViewById(R.id.btn_confirm_payment_intent)
        mPaymentIntentValue = findViewById(R.id.payment_intent_value)
        mCardInputWidget = findViewById(R.id.card_input_widget)

        mProgressDialogController = ProgressDialogController(supportFragmentManager,
            resources)
        mErrorDialogHandler = ErrorDialogHandler(this)
        mStripe = Stripe(applicationContext,
            PaymentConfiguration.getInstance().publishableKey)
        val retrofit = RetrofitFactory.instance
        mStripeService = retrofit.create(StripeService::class.java)

        createPaymentIntent.setOnClickListener { createPaymentIntent() }

        mRetrievePaymentIntent!!.setOnClickListener { retrievePaymentIntent() }
        mConfirmPaymentIntent!!.setOnClickListener {
            val card = mCardInputWidget!!.card
            if (card != null) {
                confirmPaymentIntent(card)
            }
        }
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mStripe!!.onPaymentResult(requestCode, data, object : ApiResultCallback<PaymentIntentResult> {
            override fun onSuccess(result: PaymentIntentResult) {
                mClientSecret = result.intent.clientSecret
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
        val disposable = mStripeService!!.createPaymentIntent(params)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { mProgressDialogController!!.show(R.string.creating_payment_intent) }
            .doOnComplete { mProgressDialogController!!.dismiss() }

            // Because we've made the mapping above, we're now subscribing
            // to the result of creating a 3DS Source
            .subscribe(
                { onCreatedPaymentIntent(it) },
                { throwable -> mErrorDialogHandler!!.show(throwable.localizedMessage) }
            )
        mCompositeDisposable.add(disposable)
    }

    private fun onCreatedPaymentIntent(responseBody: ResponseBody) {
        try {
            val jsonObject = JSONObject(responseBody.string())
            mPaymentIntentValue!!.text = jsonObject.toString()
            mClientSecret = jsonObject.getString("secret")
            mConfirmPaymentIntent!!.isEnabled = mClientSecret != null
            mRetrievePaymentIntent!!.isEnabled = mClientSecret != null
        } catch (exception: IOException) {
            Log.e(TAG, exception.toString())
        } catch (exception: JSONException) {
            Log.e(TAG, exception.toString())
        }
    }

    private fun retrievePaymentIntent() {
        val paymentIntentObservable = Observable.fromCallable {
            mStripe!!.retrievePaymentIntentSynchronous(mClientSecret!!)!!
        }
        val disposable = paymentIntentObservable
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { mProgressDialogController!!.show(R.string.retrieving_payment_intent) }
            .doOnComplete { mProgressDialogController!!.dismiss() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { displayPaymentIntent(it) },
                { throwable -> Log.e(TAG, throwable.toString()) }
            )
        mCompositeDisposable.add(disposable)
    }

    private fun displayPaymentIntent(paymentIntent: PaymentIntent) {
        mPaymentIntentValue!!.text = JSONObject(paymentIntent.toMap()).toString()
    }

    private fun confirmPaymentIntent(card: Card) {
        mStripe!!.confirmPayment(this,
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null),
                mClientSecret!!, RETURN_URL))
    }

    companion object {
        private val TAG = PaymentIntentActivity::class.java.name

        private const val RETURN_URL = "stripe://payment_intent_return"
    }
}
