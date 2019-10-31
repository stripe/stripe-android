package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSession.Companion.TOKEN_PAYMENT_SESSION
import com.stripe.android.R
import com.stripe.android.StripeError
import com.stripe.android.exception.StripeException
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.i18n.TranslatorManager
import kotlinx.android.synthetic.main.activity_payment_methods.*

/**
 *
 * An activity that allows a customer to select from their attach payment methods,
 * or to add new ones.
 *
 *
 * This Activity is typically started through [com.stripe.android.PaymentSession].
 * To directly start this activity, use [PaymentMethodsActivityStarter.startForResult].
 *
 *
 * Use [PaymentMethodsActivityStarter.Result.fromIntent]
 * to retrieve the result of this activity from an intent in onActivityResult().
 */
class PaymentMethodsActivity : AppCompatActivity() {

    private lateinit var adapter: PaymentMethodsAdapter
    private var startedFromPaymentSession: Boolean = false
    private lateinit var customerSession: CustomerSession
    private lateinit var cardDisplayTextFactory: CardDisplayTextFactory
    private var tappedPaymentMethod: PaymentMethod? = null

    private lateinit var viewModel: PaymentMethodsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_methods)

        viewModel = ViewModelProviders.of(this).get(PaymentMethodsViewModel::class.java)

        val args = PaymentMethodsActivityStarter.Args.create(intent)
        startedFromPaymentSession = args.isPaymentSessionActive
        cardDisplayTextFactory = CardDisplayTextFactory.create(this)
        customerSession = CustomerSession.getInstance()

        val initiallySelectedPaymentMethodId =
            savedInstanceState?.getString(STATE_SELECTED_PAYMENT_METHOD_ID)
                ?: args.initialPaymentMethodId
        adapter = PaymentMethodsAdapter(
            initiallySelectedPaymentMethodId,
            args,
            args.paymentMethodTypes
        )
        adapter.listener = object : PaymentMethodsAdapter.Listener {
            override fun onClick(paymentMethod: PaymentMethod) {
                tappedPaymentMethod = paymentMethod
            }
        }
        setupRecyclerView()

        val itemTouchHelper = ItemTouchHelper(
            PaymentMethodSwipeCallback(this, adapter,
                SwipeToDeleteCallbackListener(this))
        )
        itemTouchHelper.attachToRecyclerView(payment_methods_recycler)

        setSupportActionBar(payment_methods_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        viewModel.paymentMethods.observe(this, Observer {
            when (it.status) {
                PaymentMethodsViewModel.Result.Status.SUCCESS -> {
                    val paymentMethods = it.data as List<PaymentMethod>
                    updatePaymentMethods(paymentMethods)
                }
                PaymentMethodsViewModel.Result.Status.ERROR -> {
                    val exception = it.data as StripeException
                    val displayedError = TranslatorManager.getErrorMessageTranslator()
                        .translate(exception.statusCode, exception.message, exception.stripeError)
                    showError(displayedError)
                }
            }
            setCommunicatingProgress(false)
        })

        fetchCustomerPaymentMethods()

        // This prevents the first click from being eaten by the focus.
        payment_methods_recycler.requestFocusFromTouch()
    }

    private fun setupRecyclerView() {
        payment_methods_recycler.setHasFixedSize(false)
        payment_methods_recycler.layoutManager = LinearLayoutManager(this)
        payment_methods_recycler.adapter = adapter
        payment_methods_recycler.itemAnimator = object : DefaultItemAnimator() {
            override fun onAnimationFinished(viewHolder: RecyclerView.ViewHolder) {
                super.onAnimationFinished(viewHolder)

                // wait until post-tap animations are completed before finishing activity
                tappedPaymentMethod?.let { setSelectionAndFinish(it) }
                tappedPaymentMethod = null
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AddPaymentMethodActivityStarter.REQUEST_CODE &&
            resultCode == Activity.RESULT_OK) {
            onPaymentMethodCreated(data)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        setSelectionAndFinish(adapter.selectedPaymentMethod)
        return true
    }

    private fun onPaymentMethodCreated(data: Intent?) {
        initLoggingTokens()

        data?.let {
            val result = AddPaymentMethodActivityStarter.Result.fromIntent(data)
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

    private fun onDeletedPaymentMethod(paymentMethod: PaymentMethod) {
        adapter.deletePaymentMethod(paymentMethod)

        paymentMethod.id?.let { paymentMethodId ->
            customerSession.detachPaymentMethod(paymentMethodId, PaymentMethodDeleteListener())
        }

        showSnackbar(paymentMethod, R.string.removed)
    }

    private fun showSnackbar(paymentMethod: PaymentMethod, @StringRes stringRes: Int) {
        val snackbarText = paymentMethod.card?.let { paymentMethodId ->
            getString(stringRes, cardDisplayTextFactory.createUnstyled(paymentMethodId))
        }

        if (snackbarText != null) {
            Snackbar.make(
                payment_methods_coordinator,
                snackbarText,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onBackPressed() {
        setSelectionAndFinish(adapter.selectedPaymentMethod)
    }

    private fun fetchCustomerPaymentMethods() {
        setCommunicatingProgress(true)
        viewModel.loadPaymentMethods()
    }

    private fun updatePaymentMethods(paymentMethods: List<PaymentMethod>) {
        adapter.setPaymentMethods(paymentMethods)
    }

    private fun initLoggingTokens() {
        if (startedFromPaymentSession) {
            customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        }
        customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_METHODS_ACTIVITY)
    }

    private fun cancelAndFinish() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun setCommunicatingProgress(communicating: Boolean) {
        payment_methods_progress_bar.visibility = if (communicating) View.VISIBLE else View.GONE
    }

    fun setSelectionAndFinish(paymentMethod: PaymentMethod?) {
        if (paymentMethod?.id == null) {
            cancelAndFinish()
            return
        }

        finishWithPaymentMethod(paymentMethod)
    }

    private fun finishWithPaymentMethod(paymentMethod: PaymentMethod) {
        setResult(Activity.RESULT_OK, Intent()
            .putExtras(PaymentMethodsActivityStarter.Result(paymentMethod).toBundle())
        )
        finish()
    }

    private fun showError(error: String) {
        AlertDialog.Builder(this)
            .setMessage(error)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED_PAYMENT_METHOD_ID, adapter.selectedPaymentMethod?.id)
    }

    private fun confirmDeletePaymentMethod(paymentMethod: PaymentMethod) {
        val message = paymentMethod.card?.let {
            cardDisplayTextFactory.createUnstyled(it)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_payment_method)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                onDeletedPaymentMethod(paymentMethod)
            }
            .setNegativeButton(android.R.string.no) { _, _ ->
                adapter.resetPaymentMethod(paymentMethod)
            }
            .setOnCancelListener {
                adapter.resetPaymentMethod(paymentMethod)
            }
            .create()
            .show()
    }

    private class PaymentMethodDeleteListener : CustomerSession.PaymentMethodRetrievalListener {
        override fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod) {
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
        }
    }

    private class SwipeToDeleteCallbackListener constructor(
        private val activity: PaymentMethodsActivity
    ) : PaymentMethodSwipeCallback.Listener {

        override fun onSwiped(paymentMethod: PaymentMethod) {
            activity.confirmDeletePaymentMethod(paymentMethod)
        }
    }

    companion object {
        private const val STATE_SELECTED_PAYMENT_METHOD_ID = "state_selected_payment_method_id"
        const val TOKEN_PAYMENT_METHODS_ACTIVITY: String = "PaymentMethodsActivity"
    }
}
