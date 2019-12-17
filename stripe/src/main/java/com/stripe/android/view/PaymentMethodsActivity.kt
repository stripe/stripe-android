package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession.Companion.TOKEN_PAYMENT_SESSION
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.i18n.TranslatorManager
import kotlinx.android.synthetic.main.activity_payment_methods.*

/**
 * An activity that allows a customer to select from their attached payment methods,
 * or add a new one via [AddPaymentMethodActivity].
 *
 * This Activity is typically started through [com.stripe.android.PaymentSession].
 * To directly start this activity, use [PaymentMethodsActivityStarter.startForResult].
 *
 * Use [PaymentMethodsActivityStarter.Result.fromIntent]
 * to retrieve the result of this activity from an intent in onActivityResult().
 */
class PaymentMethodsActivity : AppCompatActivity() {

    private lateinit var adapter: PaymentMethodsAdapter
    private var startedFromPaymentSession: Boolean = false
    private lateinit var customerSession: CustomerSession
    private lateinit var cardDisplayTextFactory: CardDisplayTextFactory

    private val alertDisplayer: AlertDisplayer by lazy {
        AlertDisplayer.DefaultAlertDisplayer(this)
    }

    private lateinit var viewModel: PaymentMethodsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_methods)

        customerSession = CustomerSession.getInstance()

        val args = PaymentMethodsActivityStarter.Args.create(intent)
        viewModel = ViewModelProviders.of(
            this,
            PaymentMethodsViewModel.Factory(customerSession, args.initialPaymentMethodId)
        )[PaymentMethodsViewModel::class.java]

        args.windowFlags?.let {
            window.addFlags(it)
        }

        startedFromPaymentSession = args.isPaymentSessionActive
        cardDisplayTextFactory = CardDisplayTextFactory.create(this)

        setupRecyclerView(args)

        setSupportActionBar(payment_methods_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        fetchCustomerPaymentMethods()

        // This prevents the first click from being eaten by the focus.
        payment_methods_recycler.requestFocusFromTouch()
    }

    private fun setupRecyclerView(
        args: PaymentMethodsActivityStarter.Args
    ) {
        adapter = PaymentMethodsAdapter(
            args,
            addableTypes = args.paymentMethodTypes,
            initiallySelectedPaymentMethodId = viewModel.selectedPaymentMethodId,
            shouldShowGooglePay = args.shouldShowGooglePay
        )

        adapter.listener = object : PaymentMethodsAdapter.Listener {
            override fun onPaymentMethodClick(paymentMethod: PaymentMethod) {
                payment_methods_recycler.tappedPaymentMethod = paymentMethod
            }

            override fun onGooglePayClick() {
                finishWithGooglePay()
            }
        }

        payment_methods_recycler.adapter = adapter
        payment_methods_recycler.listener = object : PaymentMethodsRecyclerView.Listener {
            override fun onPaymentMethodSelected(paymentMethod: PaymentMethod) {
                finishWithPaymentMethod(paymentMethod)
            }
        }

        payment_methods_recycler.attachItemTouchHelper(
            PaymentMethodSwipeCallback(this, adapter,
                SwipeToDeleteCallbackListener(this, adapter, cardDisplayTextFactory,
                    customerSession))
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AddPaymentMethodActivityStarter.REQUEST_CODE &&
            resultCode == Activity.RESULT_OK) {
            onPaymentMethodCreated(data)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finishWithPaymentMethod(adapter.selectedPaymentMethod, Activity.RESULT_CANCELED)
        return true
    }

    private fun onPaymentMethodCreated(data: Intent?) {
        initLoggingTokens()

        data?.let {
            val result =
                AddPaymentMethodActivityStarter.Result.fromIntent(data)
            result?.paymentMethod?.let { onAddedPaymentMethod(it) }
        } ?: fetchCustomerPaymentMethods()
    }

    private fun onAddedPaymentMethod(paymentMethod: PaymentMethod) {
        val type = PaymentMethod.Type.lookup(paymentMethod.type)
        if (type?.isReusable == true) {
            // Refresh the list of Payment Methods with the new reusable Payment Method.
            fetchCustomerPaymentMethods()
            showSnackbar(paymentMethod, R.string.added)
        } else {
            // If the added Payment Method is not reusable, it also can't be attached to a
            // customer, so immediately return to the launching host with the new
            // Payment Method.
            finishWithPaymentMethod(paymentMethod)
        }
    }

    @JvmSynthetic
    internal fun showSnackbar(paymentMethod: PaymentMethod, @StringRes stringRes: Int) {
        val snackbarText = paymentMethod.card?.let { paymentMethodId ->
            getString(stringRes, cardDisplayTextFactory.createUnstyled(paymentMethodId))
        }

        if (snackbarText != null) {
            Snackbar.make(
                coordinator,
                snackbarText,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onBackPressed() {
        finishWithPaymentMethod(adapter.selectedPaymentMethod, Activity.RESULT_CANCELED)
    }

    private fun fetchCustomerPaymentMethods() {
        setCommunicatingProgress(true)
        viewModel.getPaymentMethods().observe(this, Observer {
            when (it) {
                is PaymentMethodsViewModel.Result.Success -> {
                    adapter.setPaymentMethods(it.paymentMethods)
                }
                is PaymentMethodsViewModel.Result.Error -> {
                    val exception = it.exception
                    val displayedError = TranslatorManager.getErrorMessageTranslator()
                        .translate(exception.statusCode, exception.message, exception.stripeError)
                    alertDisplayer.show(displayedError)
                }
            }
            setCommunicatingProgress(false)
        })
    }

    private fun initLoggingTokens() {
        if (startedFromPaymentSession) {
            customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        }
        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_METHODS_ACTIVITY)
    }

    private fun setCommunicatingProgress(communicating: Boolean) {
        payment_methods_progress_bar.visibility = if (communicating) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun finishWithGooglePay() {
        setResult(Activity.RESULT_OK,
            Intent().putExtras(
                PaymentMethodsActivityStarter.Result(useGooglePay = true).toBundle()
            )
        )

        finish()
    }

    private fun finishWithPaymentMethod(
        paymentMethod: PaymentMethod?,
        resultCode: Int = Activity.RESULT_OK
    ) {
        setResult(
            resultCode,
            Intent().also {
                if (paymentMethod != null) {
                    it.putExtras(PaymentMethodsActivityStarter.Result(paymentMethod).toBundle())
                }
            }
        )

        finish()
    }

    override fun onDestroy() {
        viewModel.selectedPaymentMethodId = adapter.selectedPaymentMethod?.id
        super.onDestroy()
    }

    internal companion object {
        internal const val TOKEN_PAYMENT_METHODS_ACTIVITY: String = "PaymentMethodsActivity"
    }
}
