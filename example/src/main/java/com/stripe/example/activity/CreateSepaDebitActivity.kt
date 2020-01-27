package com.stripe.example.activity

import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.example.R
import kotlinx.android.synthetic.main.create_sepa_debit_pm_layout.*

/**
 * See [SEPA Direct Debit payments](https://stripe.com/docs/payments/sepa-debit) for more
 * details.
 */
class CreateSepaDebitActivity : AppCompatActivity() {
    private val viewModel: SepaDebitViewModel by lazy {
        ViewModelProviders.of(
            this,
            SepaDebitViewModel.Factory(application)
        )[SepaDebitViewModel::class.java]
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(findViewById(android.R.id.content))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_sepa_debit_pm_layout)
        setTitle(R.string.launch_create_pm_sepa_debit)

        iban_input.setText(TEST_ACCOUNT_NUMBER)

        create_sepa_debit_button.setOnClickListener {
            progress_bar.visibility = View.VISIBLE

            viewModel.createPaymentMethod(iban_input.text.toString())
                .observe(this, Observer {
                    when (it) {
                        is SepaDebitViewModel.Result.Success -> {
                            val paymentMethod = it.paymentMethod
                            progress_bar.visibility = View.INVISIBLE
                            snackbarController.show("Created payment method: ${paymentMethod.id}")
                        }
                        is SepaDebitViewModel.Result.Error -> {
                            val exception = it.exception
                            progress_bar.visibility = View.INVISIBLE
                            snackbarController.show("Created payment method: ${exception.message}")
                        }
                    }
                })
        }
    }

    internal class SepaDebitViewModel(
        application: Application
    ) : AndroidViewModel(application) {
        private val context = application.applicationContext
        private val stripe: Stripe =
            Stripe(context, PaymentConfiguration.getInstance(context).publishableKey)

        fun createPaymentMethod(accountNumber: String): LiveData<Result> {
            val resultData = MutableLiveData<Result>()
            stripe.createPaymentMethod(
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.SepaDebit(
                        iban = accountNumber
                    ),
                    PaymentMethod.BillingDetails(
                        name = CUSTOMER_NAME,
                        email = CUSTOMER_EMAIL
                    )
                ),
                callback = object : ApiResultCallback<PaymentMethod> {
                    override fun onSuccess(result: PaymentMethod) {
                        resultData.value = Result.Success(result)
                    }

                    override fun onError(e: Exception) {
                        resultData.value = Result.Error(e)
                    }
                }
            )

            return resultData
        }

        internal sealed class Result {
            data class Success(val paymentMethod: PaymentMethod) : Result()
            data class Error(val exception: Exception) : Result()
        }

        internal class Factory(
            private val application: Application
        ) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return SepaDebitViewModel(application) as T
            }
        }
    }

    private companion object {
        private const val CUSTOMER_NAME = "Jenny Rosen"
        private const val CUSTOMER_EMAIL = "jrosen@example.com"

        private const val TEST_ACCOUNT_NUMBER = "DE89370400440532013000"
    }
}
