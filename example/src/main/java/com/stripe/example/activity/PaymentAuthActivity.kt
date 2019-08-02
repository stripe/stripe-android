package com.stripe.example.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.stripe.android.*
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.example.R
import com.stripe.example.Settings
import com.stripe.example.module.RetrofitFactory
import com.stripe.example.service.StripeService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.HashMap

/**
 * An example of creating a PaymentIntent, then confirming it with [Stripe.confirmPayment]
 */
class PaymentAuthActivity : AppCompatActivity() {

    private val mCompositeSubscription = CompositeDisposable()

    private var mStripe: Stripe? = null
    private var mStripeService: StripeService? = null
    private var mStatusTextView: TextView? = null
    private var mBuyButton: Button? = null
    private var mSetupButton: Button? = null
    private var mProgressBar: ProgressBar? = null

    private val stripeAccountId: String? = Settings.STRIPE_ACCOUNT_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_auth)

        val uiCustomization = PaymentAuthConfig.Stripe3ds2UiCustomization.Builder.createWithAppTheme(this)
            .build()
        PaymentAuthConfig.init(PaymentAuthConfig.Builder()
            .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder()
                .setTimeout(6)
                .setUiCustomization(uiCustomization)
                .build())
            .build())

        mStatusTextView = findViewById(R.id.status)
        if (savedInstanceState != null) {
            mStatusTextView!!.text = savedInstanceState.getString(STATE_STATUS)
        }

        mStripeService = RetrofitFactory.instance.create(StripeService::class.java)
        mStripe = Stripe(this, PaymentConfiguration.getInstance().publishableKey)

        // set an optional Stripe Connect account to use for API requests

        if (stripeAccountId != null) {
            mStripe!!.setStripeAccount(stripeAccountId)
        }

        mBuyButton = findViewById(R.id.buy_button)
        mBuyButton!!.setOnClickListener { createPaymentIntent(stripeAccountId) }

        mSetupButton = findViewById(R.id.setup_button)
        mSetupButton!!.setOnClickListener { createSetupIntent() }

        mProgressBar = findViewById(R.id.progress_bar)
    }

    private fun confirmPaymentIntent(paymentIntentClientSecret: String) {
        mStatusTextView!!.append("\n\nStarting payment authentication")
        mStripe!!.confirmPayment(this,
            ConfirmPaymentIntentParams.createWithPaymentMethodId(
                PAYMENT_METHOD_3DS2_REQUIRED,
                paymentIntentClientSecret,
                RETURN_URL))
    }

    private fun confirmSetupIntent(setupIntentClientSecret: String) {
        mStatusTextView!!.append("\n\nStarting setup intent authentication")
        mStripe!!.confirmSetupIntent(this,
            ConfirmSetupIntentParams.create(
                PAYMENT_METHOD_AUTH_REQUIRED_ON_SETUP,
                setupIntentClientSecret,
                RETURN_URL))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mProgressBar!!.visibility = View.VISIBLE
        mStatusTextView!!.append("\n\nPayment authentication completed, getting result")

        val isPaymentResult = mStripe!!.onPaymentResult(requestCode, data, AuthResultListener(this))

        if (!isPaymentResult) {
            val isSetupResult = mStripe!!.onSetupResult(requestCode, data, SetupAuthResultListener(this))
        }
    }

    override fun onPause() {
        mProgressBar!!.visibility = View.INVISIBLE
        super.onPause()
    }

    override fun onDestroy() {
        mCompositeSubscription.dispose()
        super.onDestroy()
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(STATE_STATUS, mStatusTextView!!.text.toString())
    }

    private fun createPaymentIntent(stripeAccountId: String?) {
        mCompositeSubscription.add(
            mStripeService!!.createPaymentIntent(createPaymentIntentParams(stripeAccountId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { d ->
                    mProgressBar!!.visibility = View.VISIBLE
                    mBuyButton!!.isEnabled = false
                    mSetupButton!!.isEnabled = false
                    mStatusTextView!!.setText(R.string.creating_payment_intent)
                }
                .subscribe { handleCreatePaymentIntentResponse(it) })
    }

    private fun createSetupIntent() {
        mCompositeSubscription.add(
            mStripeService!!.createSetupIntent(HashMap(0))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { d ->
                    mProgressBar!!.visibility = View.VISIBLE
                    mBuyButton!!.isEnabled = false
                    mSetupButton!!.isEnabled = false
                    mStatusTextView!!.setText(R.string.creating_setup_intent)
                }
                .subscribe { handleCreateSetupIntentResponse(it) })
    }

    private fun handleCreatePaymentIntentResponse(responseBody: ResponseBody) {
        try {
            val responseData = JSONObject(responseBody.string())
            mStatusTextView!!.append("\n\n" + getString(R.string.payment_intent_status,
                responseData.getString("status")))
            val secret = responseData.getString("secret")
            confirmPaymentIntent(secret)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun handleCreateSetupIntentResponse(responseBody: ResponseBody) {
        try {
            val responseData = JSONObject(responseBody.string())
            mStatusTextView!!.append("\n\n" + getString(R.string.setup_intent_status,
                responseData.getString("status")))
            val secret = responseData.getString("secret")
            confirmSetupIntent(secret)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onAuthComplete() {
        mBuyButton!!.isEnabled = true
        mSetupButton!!.isEnabled = true
        mProgressBar!!.visibility = View.INVISIBLE
    }

    private fun createPaymentIntentParams(stripeAccountId: String?): HashMap<String, Any> {
        val params = HashMap<String, Any>()
        params["payment_method_types[]"] = "card"
        params["amount"] = 1000
        params["currency"] = "usd"
        if (stripeAccountId != null) {
            params["stripe_account"] = stripeAccountId
        }
        return params
    }

    private class AuthResultListener constructor(
        activity: PaymentAuthActivity
    ) : ApiResultCallback<PaymentIntentResult> {
        private val mActivityRef: WeakReference<PaymentAuthActivity> = WeakReference(activity)

        override fun onSuccess(paymentIntentResult: PaymentIntentResult) {
            val activity = mActivityRef.get() ?: return

            val paymentIntent = paymentIntentResult.intent
            activity.mStatusTextView!!.append("\n\n" +
                "Auth status: " + paymentIntentResult.status + "\n\n" +
                activity.getString(R.string.payment_intent_status, paymentIntent.status))
            activity.onAuthComplete()
        }

        override fun onError(e: Exception) {
            val activity = mActivityRef.get() ?: return

            activity.mStatusTextView!!.append("\n\nException: " + e.message)
            activity.onAuthComplete()
        }
    }

    private class SetupAuthResultListener constructor(activity: PaymentAuthActivity) : ApiResultCallback<SetupIntentResult> {
        private val mActivityRef: WeakReference<PaymentAuthActivity> = WeakReference(activity)

        override fun onSuccess(setupIntentResult: SetupIntentResult) {
            val activity = mActivityRef.get() ?: return

            val setupIntent = setupIntentResult.intent
            activity.mStatusTextView!!.append("\n\n" + activity.getString(R.string.setup_intent_status, setupIntent.status))
            activity.onAuthComplete()
        }

        override fun onError(e: Exception) {
            val activity = mActivityRef.get() ?: return

            activity.mStatusTextView!!.append("\n\nException: " + e.message)
            activity.onAuthComplete()
        }
    }

    companion object {

        /**
         * See https://stripe.com/docs/payments/3d-secure#three-ds-cards for more options.
         */
        private const val PAYMENT_METHOD_3DS2_REQUIRED = "pm_card_threeDSecure2Required"
        private const val PAYMENT_METHOD_3DS_REQUIRED = "pm_card_threeDSecureRequired"
        private const val PAYMENT_METHOD_AUTH_REQUIRED_ON_SETUP = "pm_card_authenticationRequiredOnSetup"

        private const val RETURN_URL = "stripe://payment_auth"

        private const val STATE_STATUS = "status"
    }
}
