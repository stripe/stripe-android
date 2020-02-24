package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
    private val startedFromPaymentSession: Boolean by lazy {
        args.isPaymentSessionActive
    }
    private val customerSession: CustomerSession by lazy {
        CustomerSession.getInstance()
    }
    private val cardDisplayTextFactory: CardDisplayTextFactory by lazy {
        CardDisplayTextFactory(this)
    }

    private val alertDisplayer: AlertDisplayer by lazy {
        AlertDisplayer.DefaultAlertDisplayer(this)
    }

    private val args: PaymentMethodsActivityStarter.Args by lazy {
        PaymentMethodsActivityStarter.Args.create(intent)
    }

    private val viewModel: PaymentMethodsViewModel by lazy {
        ViewModelProvider(
            this,
            PaymentMethodsViewModel.Factory(
                application,
                customerSession,
                args.initialPaymentMethodId
            )
        )[PaymentMethodsViewModel::class.java]
    }

    private val adapter: PaymentMethodsAdapter by lazy {
        PaymentMethodsAdapter(
            args,
            addableTypes = args.paymentMethodTypes,
            initiallySelectedPaymentMethodId = viewModel.selectedPaymentMethodId,
            shouldShowGooglePay = args.shouldShowGooglePay,
            useGooglePay = args.useGooglePay
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_methods)

        args.windowFlags?.let {
            window.addFlags(it)
        }

        viewModel.snackbarData.observe(this, Observer { snackbarText ->
            snackbarText?.let {
                Snackbar.make(coordinator, it, Snackbar.LENGTH_SHORT).show()
            }
        })
        viewModel.progressData.observe(this, Observer {
            payment_methods_progress_bar.visibility = if (it) {
                View.VISIBLE
            } else {
                View.GONE
            }
        })

        setupRecyclerView()

        setSupportActionBar(payment_methods_toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        fetchCustomerPaymentMethods()

        // This prevents the first click from being eaten by the focus.
        payment_methods_recycler.requestFocusFromTouch()
    }

    private fun setupRecyclerView() {
        val deletePaymentMethodDialogFactory = DeletePaymentMethodDialogFactory(
            this,
            adapter,
            cardDisplayTextFactory,
            customerSession
        ) { viewModel.onPaymentMethodRemoved(it) }

        adapter.listener = object : PaymentMethodsAdapter.Listener {
            override fun onPaymentMethodClick(paymentMethod: PaymentMethod) {
                payment_methods_recycler.tappedPaymentMethod = paymentMethod
            }

            override fun onGooglePayClick() {
                finishWithGooglePay()
            }

            override fun onDeletePaymentMethodAction(paymentMethod: PaymentMethod) {
                deletePaymentMethodDialogFactory.create(paymentMethod).show()
            }
        }

        payment_methods_recycler.adapter = adapter
        payment_methods_recycler.paymentMethodSelectedCallback = { finishWithResult(it) }

        payment_methods_recycler.attachItemTouchHelper(
            PaymentMethodSwipeCallback(
                this, adapter,
                SwipeToDeleteCallbackListener(deletePaymentMethodDialogFactory)
            )
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
        finishWithResult(adapter.selectedPaymentMethod, Activity.RESULT_CANCELED)
        return true
    }

    private fun onPaymentMethodCreated(data: Intent?) {
        addLoggingTokens()

        data?.let {
            val result =
                AddPaymentMethodActivityStarter.Result.fromIntent(data)
            result?.paymentMethod?.let { onAddedPaymentMethod(it) }
        } ?: fetchCustomerPaymentMethods()
    }

    private fun onAddedPaymentMethod(paymentMethod: PaymentMethod) {
        if (paymentMethod.type?.isReusable == true) {
            // Refresh the list of Payment Methods with the new reusable Payment Method.
            fetchCustomerPaymentMethods()
            viewModel.onPaymentMethodAdded(paymentMethod)
        } else {
            // If the added Payment Method is not reusable, it also can't be attached to a
            // customer, so immediately return to the launching host with the new
            // Payment Method.
            finishWithResult(paymentMethod)
        }
    }

    override fun onBackPressed() {
        finishWithResult(adapter.selectedPaymentMethod, Activity.RESULT_CANCELED)
    }

    private fun fetchCustomerPaymentMethods() {
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
        })
    }

    private fun addLoggingTokens() {
        listOfNotNull(
            TOKEN_PAYMENT_SESSION.takeIf { startedFromPaymentSession },
            TOKEN_PAYMENT_METHODS_ACTIVITY
        ).forEach { customerSession.addProductUsageTokenIfValid(it) }
    }

    private fun finishWithGooglePay() {
        setResult(Activity.RESULT_OK,
            Intent().putExtras(
                PaymentMethodsActivityStarter.Result(useGooglePay = true).toBundle()
            )
        )

        finish()
    }

    private fun finishWithResult(
        paymentMethod: PaymentMethod?,
        resultCode: Int = Activity.RESULT_OK
    ) {
        setResult(
            resultCode,
            Intent().also {
                it.putExtras(PaymentMethodsActivityStarter.Result(
                    paymentMethod = paymentMethod,
                    useGooglePay = args.useGooglePay && paymentMethod == null
                ).toBundle())
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
