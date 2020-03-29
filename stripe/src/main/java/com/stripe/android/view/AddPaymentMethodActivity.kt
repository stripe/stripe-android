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
import androidx.annotation.StringRes
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.Stripe
import com.stripe.android.databinding.AddPaymentMethodActivityBinding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams

/**
 * Activity used to display a [AddPaymentMethodView] and receive the resulting
 * [PaymentMethod] in the `Activity#onActivityResult(int, int, Intent)` of the
 * launching Activity.
 *
 *
 * Should be started with [AddPaymentMethodActivityStarter].
 */
class AddPaymentMethodActivity : StripeActivity() {

    private val args: AddPaymentMethodActivityStarter.Args by lazy {
        AddPaymentMethodActivityStarter.Args.create(intent)
    }

    private val stripe: Stripe by lazy {
        val paymentConfiguration = args.paymentConfiguration
            ?: PaymentConfiguration.getInstance(this)
        Stripe(applicationContext, paymentConfiguration.publishableKey)
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

    private val customerSession: CustomerSession by lazy {
        CustomerSession.getInstance()
    }

    private val viewModel: AddPaymentMethodViewModel by lazy {
        ViewModelProvider(this, AddPaymentMethodViewModel.Factory(
            stripe, customerSession, args
        ))[AddPaymentMethodViewModel::class.java]
    }

    private val titleStringRes: Int
        @StringRes
        get() {
            return when (paymentMethodType) {
                PaymentMethod.Type.Card -> {
                    R.string.title_add_a_card
                }
                PaymentMethod.Type.Fpx -> {
                    R.string.title_bank_account
                }
                else -> {
                    throw IllegalArgumentException(
                        "Unsupported Payment Method type: ${paymentMethodType.code}")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureView(args)
    }

    override fun onResume() {
        super.onResume()
        addPaymentMethodView.requestFocus()
    }

    private fun configureView(args: AddPaymentMethodActivityStarter.Args) {
        args.windowFlags?.let {
            window.addFlags(it)
        }

        viewStub.layoutResource = R.layout.add_payment_method_activity
        val scrollView = viewStub.inflate() as ViewGroup
        val viewBinding = AddPaymentMethodActivityBinding.bind(scrollView)

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
                )
            }
            PaymentMethod.Type.Fpx -> {
                AddPaymentMethodFpxView.create(this)
            }
            else -> {
                throw IllegalArgumentException(
                    "Unsupported Payment Method type: ${paymentMethodType.code}")
            }
        }
    }

    private fun createFooterView(
        contentRoot: ViewGroup
    ): View? {
        return if (args.addPaymentMethodFooterLayoutId > 0) {
            val footerView = layoutInflater.inflate(
                args.addPaymentMethodFooterLayoutId, contentRoot, false
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

        viewModel.createPaymentMethod(params).observe(this, Observer {
            when (it) {
                is AddPaymentMethodViewModel.PaymentMethodResult.Success -> {
                    if (shouldAttachToCustomer) {
                        attachPaymentMethodToCustomer(it.paymentMethod)
                    } else {
                        finishWithPaymentMethod(it.paymentMethod)
                    }
                }
                is AddPaymentMethodViewModel.PaymentMethodResult.Error -> {
                    isProgressBarVisible = false
                    showError(it.errorMessage)
                }
            }
        })
    }

    private fun attachPaymentMethodToCustomer(paymentMethod: PaymentMethod) {
        viewModel.attachPaymentMethod(
            paymentMethod
        ).observe(this, Observer {
            when (it) {
                is AddPaymentMethodViewModel.PaymentMethodResult.Success -> {
                    finishWithPaymentMethod(it.paymentMethod)
                }
                is AddPaymentMethodViewModel.PaymentMethodResult.Error -> {
                    isProgressBarVisible = false
                    showError(it.errorMessage)
                }
            }
        })
    }

    private fun finishWithPaymentMethod(paymentMethod: PaymentMethod) {
        isProgressBarVisible = false
        setResult(Activity.RESULT_OK, Intent()
            .putExtras(AddPaymentMethodActivityStarter.Result(paymentMethod).toBundle()))
        finish()
    }

    override fun onProgressBarVisibilityChanged(visible: Boolean) {
        addPaymentMethodView.setCommunicatingProgress(visible)
    }

    internal companion object {
        internal const val PRODUCT_TOKEN: String = "AddPaymentMethodActivity"
    }
}
