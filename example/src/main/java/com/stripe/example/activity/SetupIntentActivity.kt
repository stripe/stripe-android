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
import com.stripe.android.model.*
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

    private val mCompositeDisposable = CompositeDisposable()

    private var mProgressDialogController: ProgressDialogController? = null
    private var mErrorDialogHandler: ErrorDialogHandler? = null
    private var mStripe: Stripe? = null
    private var mStripeService: StripeService? = null
    private var mClientSecret: String? = null
    private var mCreatePaymentMethod: Button? = null
    private var mConfirmSetupIntent: Button? = null
    private var mRetrieveSetupIntent: Button? = null
    private var mCardInputWidget: CardInputWidget? = null
    private var mSetupIntentValue: TextView? = null
    private var mPaymentMethod: PaymentMethod? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_intent_demo)
        val createSetupIntent = findViewById<Button>(R.id.btn_create_setup_intent)
        mCreatePaymentMethod = findViewById(R.id.btn_create_payment_method)
        mRetrieveSetupIntent = findViewById(R.id.btn_retrieve_setup_intent)
        mConfirmSetupIntent = findViewById(R.id.btn_confirm_setup_intent)
        mSetupIntentValue = findViewById(R.id.setup_intent_value)
        mCardInputWidget = findViewById(R.id.card_input_widget)

        mProgressDialogController = ProgressDialogController(supportFragmentManager, resources)
        mErrorDialogHandler = ErrorDialogHandler(this)

        mStripe = Stripe(this,
            PaymentConfiguration.getInstance().publishableKey)
        val retrofit = RetrofitFactory.instance
        mStripeService = retrofit.create(StripeService::class.java)

        createSetupIntent.setOnClickListener { createSetupIntent() }
        mCreatePaymentMethod!!.setOnClickListener {
            val card = mCardInputWidget!!.card
            if (card != null) {
                createPaymentMethod(card)
            }
        }
        mRetrieveSetupIntent!!.setOnClickListener { retrieveSetupIntent() }
        mConfirmSetupIntent!!.setOnClickListener { confirmSetupIntent() }
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mStripe!!.onSetupResult(requestCode, data, object : ApiResultCallback<SetupIntentResult> {
            override fun onSuccess(result: SetupIntentResult) {
                mClientSecret = result.intent.clientSecret
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
        val disposable = mStripeService!!.createSetupIntent(HashMap())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { mProgressDialogController!!.show(R.string.creating_setup_intent) }
            .doOnComplete { mProgressDialogController!!.dismiss() }

            // Because we've made the mapping above, we're now subscribing
            // to the result of creating a 3DS Source
            .subscribe(
                { onCreatedSetupIntent(it) },
                { throwable -> mErrorDialogHandler!!.show(throwable.localizedMessage) }
            )
        mCompositeDisposable.add(disposable)
    }

    private fun onCreatedSetupIntent(responseBody: ResponseBody) {
        try {
            val jsonObject = JSONObject(responseBody.string())
            mSetupIntentValue!!.text = jsonObject.toString()
            mClientSecret = jsonObject.getString("secret")
            mCreatePaymentMethod!!.isEnabled = mClientSecret != null
        } catch (exception: IOException) {
            Log.e(TAG, exception.toString())
        } catch (exception: JSONException) {
            Log.e(TAG, exception.toString())
        }
    }

    private fun retrieveSetupIntent() {
        val setupIntentObservable = Observable.fromCallable {
            mStripe!!.retrieveSetupIntentSynchronous(mClientSecret!!)!!
        }

        val disposable = setupIntentObservable
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { mProgressDialogController!!.show(R.string.retrieving_setup_intent) }
            .doOnComplete { mProgressDialogController!!.dismiss() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { displaySetupIntent(it) },
                { throwable -> Log.e(TAG, throwable.toString()) }
            )
        mCompositeDisposable.add(disposable)
    }

    private fun createPaymentMethod(card: Card) {
        val paymentMethodCreateParams = PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null)

        val paymentMethodObservable = Observable.fromCallable { mStripe!!.createPaymentMethodSynchronous(paymentMethodCreateParams) }

        val disposable = paymentMethodObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { mProgressDialogController!!.show(R.string.creating_payment_method) }
            .doOnComplete { mProgressDialogController!!.dismiss() }
            .subscribe(
                { paymentMethod ->
                    if (paymentMethod != null) {
                        mSetupIntentValue!!.text = paymentMethod.id
                        mPaymentMethod = paymentMethod
                        mConfirmSetupIntent!!.isEnabled = true
                        mRetrieveSetupIntent!!.isEnabled = true
                    }
                },
                { throwable -> Log.e(TAG, throwable.toString()) }
            )
        mCompositeDisposable.add(disposable)
    }

    private fun displaySetupIntent(setupIntent: SetupIntent) {
        mSetupIntentValue!!.text = JSONObject(setupIntent.toMap()).toString()
    }

    private fun confirmSetupIntent() {
        mStripe!!.confirmSetupIntent(this,
            ConfirmSetupIntentParams.create(mPaymentMethod!!.id!!, mClientSecret!!, RETURN_URL))
    }

    companion object {
        private val TAG = SetupIntentActivity::class.java.name
        private const val RETURN_URL = "stripe://setup_intent_return"
    }
}
