package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.BASIC_INTEGRATION_DEPRECATION_WARNING
import com.stripe.android.CustomerSession
import com.stripe.android.R
import com.stripe.android.core.exception.StripeException
import com.stripe.android.databinding.StripePaymentMethodsActivityBinding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.utils.argsAreInvalid
import com.stripe.android.view.i18n.TranslatorManager
import kotlinx.coroutines.launch

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
@Deprecated(BASIC_INTEGRATION_DEPRECATION_WARNING)
class PaymentMethodsActivity : AppCompatActivity() {
    internal val viewBinding: StripePaymentMethodsActivityBinding by lazy {
        StripePaymentMethodsActivityBinding.inflate(layoutInflater)
    }

    private val startedFromPaymentSession: Boolean by lazy {
        args.isPaymentSessionActive
    }

    private val customerSession: Result<CustomerSession> by lazy {
        runCatching { CustomerSession.getInstance() }
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

    private val viewModel: PaymentMethodsViewModel by viewModels {
        PaymentMethodsViewModel.Factory(
            application,
            customerSession,
            args.initialPaymentMethodId,
            startedFromPaymentSession
        )
    }

    private val adapter: PaymentMethodsAdapter by lazy {
        PaymentMethodsAdapter(
            args,
            addableTypes = args.paymentMethodTypes,
            initiallySelectedPaymentMethodId = viewModel.selectedPaymentMethodId,
            shouldShowGooglePay = args.shouldShowGooglePay,
            useGooglePay = args.useGooglePay,
            canDeletePaymentMethods = args.canDeletePaymentMethods
        )
    }

    private var earlyExitDueToIllegalState: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (customerSession.isFailure) {
            finishWithResult(
                null,
                Activity.RESULT_CANCELED
            )
            return
        }
        if (argsAreInvalid { args }) {
            earlyExitDueToIllegalState = true
            return
        }

        setContentView(viewBinding.root)

        args.windowFlags?.let {
            window.addFlags(it)
        }

        onBackPressedDispatcher.addCallback {
            finishWithResult(adapter.selectedPaymentMethod, Activity.RESULT_CANCELED)
        }

        lifecycleScope.launch {
            viewModel.snackbarData.collect { snackbarText ->
                snackbarText?.let {
                    Snackbar.make(viewBinding.coordinator, it, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.progressData.collect {
                viewBinding.progressBar.isVisible = it
            }
        }

        val addPaymentMethodLauncher = registerForActivityResult(
            AddPaymentMethodContract(),
            ::onAddPaymentMethodResult
        )

        observePaymentMethodData()

        setupRecyclerView(addPaymentMethodLauncher)

        setSupportActionBar(viewBinding.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        createFooterView(viewBinding.footerContainer)?.let { footer ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                viewBinding.recycler.accessibilityTraversalBefore = footer.id
                footer.accessibilityTraversalAfter = viewBinding.recycler.id
            }
            viewBinding.footerContainer.addView(footer)
            viewBinding.footerContainer.isVisible = true
        }

        // This prevents the first click from being eaten by the focus.
        viewBinding.recycler.requestFocusFromTouch()
    }

    private fun setupRecyclerView(
        addPaymentMethodLauncher: ActivityResultLauncher<AddPaymentMethodActivityStarter.Args>,
    ) {
        val deletePaymentMethodDialogFactory = DeletePaymentMethodDialogFactory(
            this,
            adapter,
            cardDisplayTextFactory,
            customerSession,
            viewModel.productUsage
        ) { viewModel.onPaymentMethodRemoved(it) }

        adapter.listener = object : PaymentMethodsAdapter.Listener {
            override fun onPaymentMethodClick(paymentMethod: PaymentMethod) {
                viewBinding.recycler.tappedPaymentMethod = paymentMethod
            }

            override fun onAddPaymentMethodClick(args: AddPaymentMethodActivityStarter.Args) {
                addPaymentMethodLauncher.launch(args)
            }

            override fun onGooglePayClick() {
                finishWithGooglePay()
            }

            override fun onDeletePaymentMethodAction(paymentMethod: PaymentMethod) {
                deletePaymentMethodDialogFactory.create(paymentMethod).show()
            }
        }

        viewBinding.recycler.adapter = adapter
        viewBinding.recycler.paymentMethodSelectedCallback = { finishWithResult(it) }

        if (args.canDeletePaymentMethods) {
            viewBinding.recycler.attachItemTouchHelper(
                PaymentMethodSwipeCallback(
                    this,
                    adapter,
                    SwipeToDeleteCallbackListener(deletePaymentMethodDialogFactory)
                )
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finishWithResult(adapter.selectedPaymentMethod, Activity.RESULT_CANCELED)
        return true
    }

    @VisibleForTesting
    internal fun onAddPaymentMethodResult(result: AddPaymentMethodActivityStarter.Result) {
        when (result) {
            is AddPaymentMethodActivityStarter.Result.Success -> {
                onAddedPaymentMethod(result.paymentMethod)
            }
            is AddPaymentMethodActivityStarter.Result.Failure -> {
                // TODO(mshafrir-stripe): notify user that payment method can not be added at this time
            }
            else -> {
                // no-op
            }
        }
    }

    private fun onAddedPaymentMethod(paymentMethod: PaymentMethod) {
        if (paymentMethod.type?.isReusable == true) {
            // Refresh the list of Payment Methods with the new reusable Payment Method.
            viewModel.onPaymentMethodAdded(paymentMethod)
        } else {
            // If the added Payment Method is not reusable, it also can't be attached to a
            // customer, so immediately return to the launching host with the new
            // Payment Method.
            finishWithResult(paymentMethod)
        }
    }

    private fun finishWithGooglePay() {
        setResult(
            Activity.RESULT_OK,
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
                it.putExtras(
                    PaymentMethodsActivityStarter.Result(
                        paymentMethod = paymentMethod,
                        useGooglePay = args.useGooglePay && paymentMethod == null
                    ).toBundle()
                )
            }
        )

        finish()
    }

    private fun createFooterView(
        contentRoot: ViewGroup
    ): View? {
        return if (args.paymentMethodsFooterLayoutId > 0) {
            val footerView = layoutInflater.inflate(
                args.paymentMethodsFooterLayoutId,
                contentRoot,
                false
            )
            footerView.id = R.id.stripe_payment_methods_footer
            if (footerView is TextView) {
                LinkifyCompat.addLinks(footerView, Linkify.ALL)
                ViewCompat.enableAccessibleClickableSpanSupport(footerView)
                footerView.movementMethod = LinkMovementMethod.getInstance()
            }
            footerView
        } else {
            null
        }
    }

    private fun observePaymentMethodData() {
        lifecycleScope.launch {
            viewModel.paymentMethodsData.collect { result ->
                result?.fold(
                    onSuccess = { adapter.setPaymentMethods(it) },
                    onFailure = {
                        alertDisplayer.show(
                            when (it) {
                                is StripeException -> {
                                    TranslatorManager.getErrorMessageTranslator()
                                        .translate(it.statusCode, it.message, it.stripeError)
                                }
                                else -> {
                                    it.message.orEmpty()
                                }
                            }
                        )
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        if (!earlyExitDueToIllegalState) {
            viewModel.selectedPaymentMethodId = adapter.selectedPaymentMethod?.id
        }
        super.onDestroy()
    }

    internal companion object {
        internal const val PRODUCT_TOKEN: String = "PaymentMethodsActivity"
    }
}
