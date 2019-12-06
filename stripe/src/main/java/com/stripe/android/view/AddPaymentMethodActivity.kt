package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.Stripe
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
        createPaymentMethodView(args)
    }

    private val customerSession: CustomerSession by lazy {
        CustomerSession.getInstance()
    }

    private val viewModel: AddPaymentMethodViewModel by lazy {
        ViewModelProviders.of(this, AddPaymentMethodViewModel.Factory(
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
        viewModel.logProductUsage()
    }

    override fun onResume() {
        super.onResume()
        addPaymentMethodView.requestFocus()
    }

    private fun configureView(args: AddPaymentMethodActivityStarter.Args) {
        viewStub.layoutResource = R.layout.add_payment_method_layout
        val scrollView = viewStub.inflate() as ViewGroup
        val contentRoot: ViewGroup =
            scrollView.findViewById(R.id.stripe_add_payment_method_content_root)
        contentRoot.addView(addPaymentMethodView)

        if (args.addPaymentMethodFooterLayoutId > 0) {
            val footerView = layoutInflater.inflate(
                args.addPaymentMethodFooterLayoutId, contentRoot, false
            )
            if (footerView is TextView) {
                LinkifyCompat.addLinks(footerView, Linkify.ALL)
                footerView.movementMethod = LinkMovementMethod.getInstance()
            }
            contentRoot.addView(footerView)
        }

        setTitle(titleStringRes)
    }

    private fun createPaymentMethodView(
        args: AddPaymentMethodActivityStarter.Args
    ): AddPaymentMethodView {
        return when (paymentMethodType) {
            PaymentMethod.Type.Card -> {
                AddPaymentMethodCardView.create(this, args.shouldRequirePostalCode)
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

        setCommunicatingProgress(true)

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
                    setCommunicatingProgress(false)
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
                    setCommunicatingProgress(false)
                    showError(it.errorMessage)
                }
            }
        })
    }

    private fun finishWithPaymentMethod(paymentMethod: PaymentMethod) {
        setCommunicatingProgress(false)
        setResult(Activity.RESULT_OK, Intent()
            .putExtras(AddPaymentMethodActivityStarter.Result(paymentMethod).toBundle()))
        finish()
    }

    override fun setCommunicatingProgress(communicating: Boolean) {
        super.setCommunicatingProgress(communicating)
        addPaymentMethodView.setCommunicatingProgress(communicating)
    }

    internal companion object {
        internal const val TOKEN_ADD_PAYMENT_METHOD_ACTIVITY: String = "AddPaymentMethodActivity"
    }
}
