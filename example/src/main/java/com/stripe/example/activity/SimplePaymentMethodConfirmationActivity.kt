package com.stripe.example.activity

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.databinding.SimplePaymentMethodActivityBinding
import com.stripe.example.module.BackendApiFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.lang.ref.WeakReference

class SimplePaymentMethodConfirmationActivity : AppCompatActivity() {

    private val stripe: Stripe by lazy {
        Stripe(this, PaymentConfiguration.getInstance(this).publishableKey)
    }
    private val viewBinding: SimplePaymentMethodActivityBinding by lazy {
        SimplePaymentMethodActivityBinding.inflate(layoutInflater)
    }
    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }
    private val viewModel: SimplePaymentMethodConfirmationViewModel by lazy {
        ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory(application)
        )[SimplePaymentMethodConfirmationViewModel::class.java]
    }

    private val dropdownItem: DropdownItem
        get() {
            return DropdownItem.valueOf(
                viewBinding.paymentMethod.text.toString()
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, Observer { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this.applicationContext,
            R.layout.dropdown_menu_popup_item,
            DropdownItem.values().map { it.name })
        viewBinding.paymentMethod.setAdapter(adapter)
        viewBinding.paymentMethod.setOnItemClickListener { _, _, _, _ ->
            viewModel.status.value = ""
            onDropdownItemSelected()
        }
        viewBinding.paymentMethod.setText(DropdownItem.P24.name, false)

        viewBinding.payNow.setOnClickListener {
            keyboardController.hide()

            val dropdownItem = this.dropdownItem
            val billingDetails = PaymentMethod.BillingDetails(
                name = viewBinding.name.text.toString().takeIf { dropdownItem.requiresName },
                email = viewBinding.email.text.toString().takeIf { dropdownItem.requiresEmail }
            )

            val country = dropdownItem.country
            val params = dropdownItem.createParams(billingDetails, null)

            createPaymentIntent(country, params)
        }
    }

    private fun onDropdownItemSelected() {
        val dropdownItem = this.dropdownItem
        viewBinding.nameLayout.visibility = if (dropdownItem.requiresName) View.VISIBLE else View.GONE

        viewBinding.emailLayout.visibility = if (dropdownItem.requiresEmail) View.VISIBLE else View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_EMAIL, viewBinding.email.text.toString())
        outState.putString(STATE_NAME, viewBinding.name.text.toString())
        outState.putString(STATE_DROPDOWN_ITEM, viewBinding.paymentMethod.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getString(STATE_EMAIL)?.let(viewBinding.email::setText)
        savedInstanceState.getString(STATE_NAME)?.let(viewBinding.name::setText)
        savedInstanceState.getString(STATE_DROPDOWN_ITEM)?.let {
            viewBinding.paymentMethod.setText(it)
            onDropdownItemSelected()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        stripe.onPaymentResult(requestCode, data, PaymentIntentResultCallback(this))
    }

    private fun createPaymentIntent(country: String, params: PaymentMethodCreateParams) {
        viewModel.createPaymentIntent(country
        ) {
            handleCreatePaymentIntentResponse(it, params)
        }
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.payNow.isEnabled = enabled
        viewBinding.name.isEnabled = enabled
        viewBinding.email.isEnabled = enabled
        viewBinding.progressBar.visibility = if (enabled) View.INVISIBLE else View.VISIBLE
    }

    private fun handleCreatePaymentIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams
    ) {
        val secret = responseData.getString("secret")
        confirmPaymentIntent(secret, params)
    }

    private fun confirmPaymentIntent(
        paymentIntentClientSecret: String,
        params: PaymentMethodCreateParams
    ) {
        viewModel.status.value += "\n\nStarting PaymentIntent confirmation"
        stripe.confirmPayment(
            this,
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = params,
                clientSecret = paymentIntentClientSecret,
                returnUrl = "example://return_url"
            )
        )
    }

    private fun onConfirmSuccess(result: PaymentIntentResult) {
        val paymentIntent = result.intent
        viewModel.status.value += "\n\n" +
            "PaymentIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.payment_intent_status, paymentIntent.status)
        viewModel.inProgress.value = false
    }

    private fun onConfirmError(e: Exception) {
        viewModel.status.value += "\n\nException: " + e.message
        viewModel.inProgress.value = false
    }

    companion object {
        private const val STATE_NAME = "name"
        private const val STATE_EMAIL = "email"
        private const val STATE_DROPDOWN_ITEM = "dropdown_item"

        private enum class DropdownItem(
            val country: String,
            val createParams: (PaymentMethod.BillingDetails, Map<String, String>?) -> PaymentMethodCreateParams,
            val requiresName: Boolean = true,
            val requiresEmail: Boolean = false
        ) {
            P24("pl", PaymentMethodCreateParams.Companion::createP24, requiresName = false, requiresEmail = true),
            Bancontact("be", PaymentMethodCreateParams.Companion::createBancontact),
            EPS("at", PaymentMethodCreateParams.Companion::createEps),
            Giropay("de", PaymentMethodCreateParams.Companion::createGiropay);
        }

        private class PaymentIntentResultCallback(
            activity: SimplePaymentMethodConfirmationActivity
        ) : ApiResultCallback<PaymentIntentResult> {

            private val activityRef = WeakReference(activity)

            override fun onSuccess(result: PaymentIntentResult) {
                activityRef.get()?.onConfirmSuccess(result)
            }

            override fun onError(e: Exception) {
                activityRef.get()?.onConfirmError(e)
            }
        }
    }

    internal class SimplePaymentMethodConfirmationViewModel(
        application: Application
    ) : AndroidViewModel(application) {
        val inProgress = MutableLiveData<Boolean>()
        val status = MutableLiveData<String>()

        private val context = application.applicationContext
        private val backendApi = BackendApiFactory(context).create()
        private val compositeSubscription = CompositeDisposable()

        fun createPaymentIntent(country: String, callback: (JSONObject) -> Unit) {
            compositeSubscription.add(backendApi
                .createPaymentIntent(
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
                        inProgress.postValue(false)
                        status.postValue(status.value + "\n\n" +
                            context.getString(R.string.payment_intent_status,
                            it.getString("status")))
                        callback(it)
                    },
                    {
                        status.postValue(status.value + "\n\n${it.message}")
                    }
                ))
        }

        override fun onCleared() {
            super.onCleared()
            compositeSubscription.dispose()
        }
    }
}
