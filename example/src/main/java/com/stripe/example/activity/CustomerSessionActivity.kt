package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stripe.android.CustomerSession
import com.stripe.android.core.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivityStarter
import com.stripe.example.R
import com.stripe.example.databinding.CustomerSessionActivityBinding
import com.stripe.android.R as StripeR

/**
 * An example activity that handles working with a [CustomerSession], allowing you to
 * add and select sources for the current customer.
 */
class CustomerSessionActivity : AppCompatActivity() {
    private val viewBinding: CustomerSessionActivityBinding by lazy {
        CustomerSessionActivityBinding.inflate(layoutInflater)
    }

    private val viewModel: ActivityViewModel by viewModels()

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        setTitle(R.string.customer_payment_data_example)

        viewBinding.progressBar.visibility = View.VISIBLE
        viewModel.retrieveCustomer().observe(
            this,
            {
                viewBinding.progressBar.visibility = View.INVISIBLE
                viewBinding.selectPaymentMethodButton.isEnabled = it.isSuccess

                it.fold(
                    onSuccess = {},
                    onFailure = { error ->
                        snackbarController.show(error.message.orEmpty())
                    }
                )
            }
        )

        viewBinding.selectPaymentMethodButton.isEnabled = false
        viewBinding.selectPaymentMethodButton.setOnClickListener { launchWithCustomer() }
    }

    private fun launchWithCustomer() {
        PaymentMethodsActivityStarter(this)
            .startForResult(
                PaymentMethodsActivityStarter.Args.Builder()
                    .setCanDeletePaymentMethods(true)
                    .build()
            )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PaymentMethodsActivityStarter.REQUEST_CODE &&
            resultCode == Activity.RESULT_OK && data != null
        ) {
            val paymentMethod =
                PaymentMethodsActivityStarter.Result.fromIntent(data)?.paymentMethod
            paymentMethod?.card?.let { card ->
                viewBinding.paymentMethod.text = buildCardString(card)
            }
        }
    }

    private fun buildCardString(data: PaymentMethod.Card): String {
        return getString(StripeR.string.stripe_card_ending_in, data.brand, data.last4)
    }

    internal class ActivityViewModel : ViewModel() {
        private val customerSession = CustomerSession.getInstance()

        fun retrieveCustomer(): LiveData<Result<Customer>> {
            val liveData = MutableLiveData<Result<Customer>>()
            customerSession.retrieveCurrentCustomer(
                object : CustomerSession.CustomerRetrievalListener {
                    override fun onCustomerRetrieved(customer: Customer) {
                        liveData.value = Result.success(customer)
                    }

                    override fun onError(
                        errorCode: Int,
                        errorMessage: String,
                        stripeError: StripeError?
                    ) {
                        liveData.value = Result.failure(RuntimeException(errorMessage))
                    }
                }
            )
            return liveData
        }
    }
}
