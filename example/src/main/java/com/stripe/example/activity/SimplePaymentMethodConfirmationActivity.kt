package com.stripe.example.activity

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import com.stripe.example.databinding.DropdownMenuPopupItemBinding
import com.stripe.example.databinding.SimplePaymentMethodActivityBinding
import com.stripe.example.module.BackendApiFactory
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException
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

    private val paymentMethodCreateParams: PaymentMethodCreateParams
        get() {
            val dropdownItem = this.dropdownItem
            val billingDetails = PaymentMethod.BillingDetails(
                name = viewBinding.name.text.toString().takeIf { dropdownItem.requiresName },
                email = viewBinding.email.text.toString().takeIf { dropdownItem.requiresEmail }
            )
            return dropdownItem.createParams(billingDetails, null)
        }

    private fun onDropdownItemSelected() {
        val dropdownItem = this.dropdownItem
        viewBinding.nameLayout.visibility = viewVisibility(dropdownItem.requiresName)
        viewBinding.emailLayout.visibility = viewVisibility(dropdownItem.requiresEmail)
    }

    private fun viewVisibility(visible: Boolean): Int {
        return if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewModel.inProgress.observe(this, Observer { enableUi(!it) })
        viewModel.status.observe(this, Observer(viewBinding.status::setText))

        val adapter = DropdownItemAdapter(this)
        viewBinding.paymentMethod.setAdapter(adapter)
        viewBinding.paymentMethod.setOnItemClickListener { _, _, _, _ ->
            viewModel.status.value = ""
            onDropdownItemSelected()
        }
        viewBinding.paymentMethod.setText(DropdownItem.P24.name, false)
        onDropdownItemSelected()

        viewBinding.payNow.setOnClickListener {
            keyboardController.hide()

            viewModel.createPaymentIntent(dropdownItem.country) {
                handleCreatePaymentIntentResponse(it, paymentMethodCreateParams)
            }
        }
        viewBinding.saveForLater.setOnClickListener {
            keyboardController.hide()

            viewModel.createSetupIntent(dropdownItem.country) {
                handleCreateSetupIntentResponse(it, paymentMethodCreateParams)
            }
        }
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
        val isPaymentResult =
            stripe.onPaymentResult(requestCode, data, PaymentIntentResultCallback(this))
        if (!isPaymentResult) {
            stripe.onSetupResult(requestCode, data, SetupIntentResultCallback(this))
        }
    }

    private fun enableUi(enabled: Boolean) {
        viewBinding.payNow.isEnabled = enabled
        viewBinding.saveForLater.isEnabled = enabled
        viewBinding.name.isEnabled = enabled
        viewBinding.email.isEnabled = enabled
        viewBinding.progressBar.visibility = viewVisibility(!enabled)
    }

    private fun handleCreatePaymentIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams
    ) {
        val secret = responseData.getString("secret")
        viewModel.status.value += "\n\nStarting PaymentIntent confirmation"
        stripe.confirmPayment(
            this,
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = params,
                clientSecret = secret,
                returnUrl = "example://return_url"
            )
        )
    }

    private fun handleCreateSetupIntentResponse(
        responseData: JSONObject,
        params: PaymentMethodCreateParams
    ) {
        val secret = responseData.getString("secret")
        viewModel.status.value += "\n\nStarting SetupIntent confirmation"
        stripe.confirmSetupIntent(
            this,
            ConfirmSetupIntentParams.create(
                paymentMethodCreateParams = params,
                clientSecret = secret,
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

    private fun onConfirmSuccess(result: SetupIntentResult) {
        val setupIntent = result.intent
        viewModel.status.value += "\n\n" +
            "SetupIntent confirmation outcome: ${result.outcome}\n\n" +
            getString(R.string.setup_intent_status, setupIntent.status)
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
            @DrawableRes val icon: Int,
            val createParams: (PaymentMethod.BillingDetails, Map<String, String>?) -> PaymentMethodCreateParams,
            val requiresName: Boolean = true,
            val requiresEmail: Boolean = false
        ) {
            P24("pl",
                R.drawable.ic_brandicon__p24,
                PaymentMethodCreateParams.Companion::createP24,
                requiresName = false, requiresEmail = true),
            Bancontact("be", R.drawable.ic_brandicon__bancontact,
                PaymentMethodCreateParams.Companion::createBancontact),
            EPS("at", R.drawable.ic_brandicon__eps,
                PaymentMethodCreateParams.Companion::createEps),
            Giropay("de", R.drawable.ic_brandicon__giropay,
                PaymentMethodCreateParams.Companion::createGiropay);
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

        private class SetupIntentResultCallback(
            activity: SimplePaymentMethodConfirmationActivity
        ) : ApiResultCallback<SetupIntentResult> {

            private val activityRef = WeakReference(activity)

            override fun onSuccess(result: SetupIntentResult) {
                activityRef.get()?.onConfirmSuccess(result)
            }

            override fun onError(e: Exception) {
                activityRef.get()?.onConfirmError(e)
            }
        }

        private class DropdownItemAdapter(
            context: Context
        ) : ArrayAdapter<DropdownItem>(
            context,
            0
        ) {
            private val layoutInflater = LayoutInflater.from(context)

            init {
                addAll(DropdownItem.values().toList())
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val viewBinding = convertView?.let {
                    DropdownMenuPopupItemBinding.bind(convertView)
                } ?: DropdownMenuPopupItemBinding.inflate(layoutInflater, parent, false)

                val dropdownItem = requireNotNull(getItem(position))
                viewBinding.image.also {
                    val drawable = requireNotNull(
                        ContextCompat.getDrawable(context, dropdownItem.icon)
                    )
                    it.setImageDrawable(drawable)
                    it.contentDescription = dropdownItem.name
                }
                viewBinding.name.text = dropdownItem.name

                return viewBinding.root
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
                callApi(country, R.string.creating_payment_intent, R.string.payment_intent_status,
                    backendApi::createPaymentIntent, callback)
            }

            fun createSetupIntent(country: String, callback: (JSONObject) -> Unit) {
                callApi(country, R.string.creating_setup_intent, R.string.setup_intent_status,
                    backendApi::createSetupIntent, callback)
            }

            private fun callApi(
                country: String,
                @StringRes startString: Int,
                @StringRes statusString: Int,
                apiMethod: (MutableMap<String, Any>) -> Observable<ResponseBody>,
                callback: (JSONObject) -> Unit
            ) {
                compositeSubscription.add(
                    apiMethod(
                        mutableMapOf(
                            "country" to country
                        )
                    )
                        .doOnSubscribe {
                            inProgress.postValue(true)
                            status.postValue(context.getString(startString))
                        }
                        .map {
                            JSONObject(it.string())
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            {
                                status.postValue(status.value + "\n\n" +
                                    context.getString(statusString,
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
    }
}
