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
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.stripe.android.BASIC_INTEGRATION_DEPRECATION_WARNING
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.Stripe
import com.stripe.android.databinding.StripeAddPaymentMethodActivityBinding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.utils.argsAreInvalid
import kotlinx.coroutines.launch

/**
 * Activity used to display a [AddPaymentMethodView] and receive the resulting
 * [PaymentMethod] in the `Activity#onActivityResult(int, int, Intent)` of the
 * launching Activity.
 *
 *
 * Should be started with [AddPaymentMethodActivityStarter].
 */
@Deprecated(BASIC_INTEGRATION_DEPRECATION_WARNING)
class AddPaymentMethodActivity : StripeActivity() {

    private val args: AddPaymentMethodActivityStarter.Args by lazy {
        AddPaymentMethodActivityStarter.Args.create(intent)
    }

    private val stripe: Stripe by lazy {
        val paymentConfiguration = args.paymentConfiguration
            ?: PaymentConfiguration.getInstance(this)
        Stripe(
            applicationContext,
            publishableKey = paymentConfiguration.publishableKey,
            stripeAccountId = paymentConfiguration.stripeAccountId
        )
    }

    private val paymentMethodType: PaymentMethod.Type by lazy {
        args.paymentMethodType
    }

    private val shouldAttachToCustomer: Boolean by lazy {
        paymentMethodType.isReusable && args.shouldAttachToCustomer
    }

    private val addPaymentMethodView: AddPaymentMethodView by lazy {
        createPaymentMethodView(args).also {
            it.id = R.id.stripe_add_payment_method_form
        }
    }

    private val viewModel: AddPaymentMethodViewModel by viewModels {
        AddPaymentMethodViewModel.Factory(
            stripe,
            args
        )
    }

    private val cardInputListener = object : CardInputListener {
        override fun onFocusChange(focusField: CardInputListener.FocusField) {
            // No-op
        }
        override fun onCardComplete() {
            viewModel.onCardNumberCompleted()
        }
        override fun onExpirationComplete() {
            // No-op
        }
        override fun onCvcComplete() {
            // No-op
        }
        override fun onPostalCodeComplete() {
            // No-op
        }
    }

    private val titleStringRes: Int
        @StringRes
        get() {
            return when (paymentMethodType) {
                PaymentMethod.Type.Card -> {
                    R.string.stripe_title_add_a_card
                }
                PaymentMethod.Type.Fpx -> {
                    R.string.stripe_title_bank_account
                }
                PaymentMethod.Type.Netbanking -> {
                    R.string.stripe_title_bank_account
                }
                else -> {
                    throw IllegalArgumentException(
                        "Unsupported Payment Method type: ${paymentMethodType.code}"
                    )
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (argsAreInvalid { args }) {
            return
        }
        viewModel.onFormShown()
        configureView(args)
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(
                    AddPaymentMethodActivityStarter.Result.Canceled
                        .toBundle()
                )
        )
    }

    override fun onResume() {
        super.onResume()
        addPaymentMethodView.requestFocus()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        viewModel.onFormInteracted()
    }

    private fun configureView(args: AddPaymentMethodActivityStarter.Args) {
        args.windowFlags?.let {
            window.addFlags(it)
        }

        viewStub.layoutResource = R.layout.stripe_add_payment_method_activity
        val scrollView = viewStub.inflate() as ViewGroup
        val viewBinding = StripeAddPaymentMethodActivityBinding.bind(scrollView)

        viewBinding.root.addView(addPaymentMethodView)
        createFooterView(viewBinding.root)?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                addPaymentMethodView.accessibilityTraversalBefore = it.id
                it.accessibilityTraversalAfter = addPaymentMethodView.id
            }
            viewBinding.root.addView(it)
        }

        setTitle(titleStringRes)
    }

    private fun createPaymentMethodView(
        args: AddPaymentMethodActivityStarter.Args
    ): AddPaymentMethodView {
        return when (paymentMethodType) {
            PaymentMethod.Type.Card -> {
                AddPaymentMethodCardView(
                    context = this,
                    billingAddressFields = args.billingAddressFields
                ).also { view ->
                    view.setCardInputListener(cardInputListener)
                }
            }
            PaymentMethod.Type.Fpx -> {
                AddPaymentMethodFpxView.create(this)
            }
            PaymentMethod.Type.Netbanking -> {
                AddPaymentMethodNetbankingView.create(this)
            }
            else -> {
                throw IllegalArgumentException(
                    "Unsupported Payment Method type: ${paymentMethodType.code}"
                )
            }
        }
    }

    private fun createFooterView(
        contentRoot: ViewGroup
    ): View? {
        return if (args.addPaymentMethodFooterLayoutId > 0) {
            val footerView = layoutInflater.inflate(
                args.addPaymentMethodFooterLayoutId,
                contentRoot,
                false
            )
            footerView.id = R.id.stripe_add_payment_method_footer
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

    public override fun onActionSave() {
        viewModel.onSaveClicked()
        createPaymentMethod(viewModel, addPaymentMethodView.createParams)
    }

    internal fun createPaymentMethod(
        viewModel: AddPaymentMethodViewModel,
        params: PaymentMethodCreateParams?
    ) {
        if (params == null) {
            return
        }

        isProgressBarVisible = true

        lifecycleScope.launch {
            viewModel.createPaymentMethod(params).fold(
                onSuccess = {
                    if (shouldAttachToCustomer) {
                        attachPaymentMethodToCustomer(it)
                    } else {
                        finishWithPaymentMethod(it)
                    }
                },
                onFailure = {
                    isProgressBarVisible = false
                    showError(it.message.orEmpty())
                }
            )
        }
    }

    private fun attachPaymentMethodToCustomer(paymentMethod: PaymentMethod) {
        runCatching {
            CustomerSession.getInstance()
        }.fold(
            onSuccess = { customerSession ->
                lifecycleScope.launch {
                    viewModel.attachPaymentMethod(
                        customerSession,
                        paymentMethod
                    ).fold(
                        onSuccess = ::finishWithPaymentMethod,
                        onFailure = {
                            isProgressBarVisible = false
                            showError(it.message.orEmpty())
                        }
                    )
                }
            },
            onFailure = {
                finishWithResult(AddPaymentMethodActivityStarter.Result.Failure(it))
            }
        )
    }

    private fun finishWithPaymentMethod(paymentMethod: PaymentMethod) {
        finishWithResult(AddPaymentMethodActivityStarter.Result.Success(paymentMethod))
    }

    private fun finishWithResult(result: AddPaymentMethodActivityStarter.Result) {
        isProgressBarVisible = false
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
        finish()
    }

    override fun onProgressBarVisibilityChanged(visible: Boolean) {
        addPaymentMethodView.setCommunicatingProgress(visible)
    }

    internal companion object {
        internal const val PRODUCT_TOKEN: String = "AddPaymentMethodActivity"
    }
}
