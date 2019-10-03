package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.util.LinkifyCompat
import com.stripe.android.ApiResultCallback
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSession.TOKEN_PAYMENT_SESSION
import com.stripe.android.R
import com.stripe.android.Stripe
import com.stripe.android.StripeError
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import java.lang.ref.WeakReference

/**
 * Activity used to display a [AddPaymentMethodView] and receive the resulting
 * [PaymentMethod] in the `Activity#onActivityResult(int, int, Intent)` of the
 * launching Activity.
 *
 *
 * Should be started with [AddPaymentMethodActivityStarter].
 */
open class AddPaymentMethodActivity : StripeActivity() {
    private lateinit var stripe: Stripe
    private var addPaymentMethodView: AddPaymentMethodView? = null
    private var paymentMethodType: PaymentMethod.Type? = null
    private var startedFromPaymentSession: Boolean = false
    private var shouldAttachToCustomer: Boolean = false

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
                        "Unsupported Payment Method type: ${paymentMethodType?.code}")
                }
            }
        }

    internal open val windowToken: IBinder?
        get() = viewStub.windowToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = AddPaymentMethodActivityStarter.Args.create(intent)
        val paymentConfiguration = args.paymentConfiguration
            ?: PaymentConfiguration.getInstance(this)
        stripe = Stripe(applicationContext, paymentConfiguration.publishableKey)
        paymentMethodType = args.paymentMethodType

        configureView(args)

        shouldAttachToCustomer = paymentMethodType?.isReusable == true &&
            args.shouldAttachToCustomer
        startedFromPaymentSession = args.isPaymentSessionActive

        if (shouldAttachToCustomer && args.shouldInitCustomerSessionTokens) {
            initCustomerSessionTokens()
        }
    }

    private fun configureView(args: AddPaymentMethodActivityStarter.Args) {
        viewStub.layoutResource = R.layout.add_payment_method_layout
        val scrollView = viewStub.inflate() as ViewGroup
        val contentRoot = scrollView.getChildAt(0) as ViewGroup

        addPaymentMethodView = createPaymentMethodView(args)
        contentRoot.addView(addPaymentMethodView)

        if (args.addPaymentMethodFooter  > 0) {
            val footerView = layoutInflater.inflate(args.addPaymentMethodFooter, contentRoot, false)
            if (footerView is TextView) {
                LinkifyCompat.addLinks(footerView, Linkify.ALL)
                footerView.movementMethod = LinkMovementMethod.getInstance()
            }
            contentRoot.addView(footerView)
        }

        setTitle(titleStringRes)

        if (paymentMethodType == PaymentMethod.Type.Card) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
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
                    "Unsupported Payment Method type: ${paymentMethodType?.code}")
            }
        }
    }

    fun initCustomerSessionTokens() {
        val customerSession = CustomerSession.getInstance()
        customerSession.addProductUsageTokenIfValid(TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        if (startedFromPaymentSession) {
            customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        }
    }

    public override fun onActionSave() {
        createPaymentMethod(stripe, addPaymentMethodView?.createParams)
    }

    fun createPaymentMethod(stripe: Stripe, params: PaymentMethodCreateParams?) {
        if (params == null) {
            return
        }

        setCommunicatingProgress(true)
        stripe.createPaymentMethod(params,
            PaymentMethodCallbackImpl(this, shouldAttachToCustomer))
    }

    private fun attachPaymentMethodToCustomer(paymentMethod: PaymentMethod) {
        CustomerSession.getInstance().attachPaymentMethod(
            requireNotNull(paymentMethod.id),
            PaymentMethodRetrievalListenerImpl(this)
        )
    }

    private fun finishWithPaymentMethod(paymentMethod: PaymentMethod) {
        setCommunicatingProgress(false)
        setResult(Activity.RESULT_OK, Intent()
            .putExtra(EXTRA_NEW_PAYMENT_METHOD, paymentMethod)
            .putExtras(AddPaymentMethodActivityStarter.Result(paymentMethod).toBundle()))
        finish()
    }

    override fun setCommunicatingProgress(communicating: Boolean) {
        super.setCommunicatingProgress(communicating)
        addPaymentMethodView?.setCommunicatingProgress(communicating)
    }

    private class PaymentMethodCallbackImpl constructor(
        activity: AddPaymentMethodActivity,
        private val updatesCustomer: Boolean
    ) : ActivityPaymentMethodCallback<AddPaymentMethodActivity>(activity) {

        override fun onError(error: Exception) {
            activity?.let { activity ->
                activity.setCommunicatingProgress(false)
                // This error is independent of the CustomerSession, so we have to surface it here.
                activity.showError(error.localizedMessage.orEmpty())
            }
        }

        override fun onSuccess(paymentMethod: PaymentMethod) {
            activity?.let { activity ->
                if (updatesCustomer) {
                    activity.attachPaymentMethodToCustomer(paymentMethod)
                } else {
                    activity.finishWithPaymentMethod(paymentMethod)
                }
            }
        }
    }

    private class PaymentMethodRetrievalListenerImpl constructor(
        activity: AddPaymentMethodActivity
    ) : CustomerSession.ActivityPaymentMethodRetrievalListener<AddPaymentMethodActivity>(activity) {
        override fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod) {
            activity?.finishWithPaymentMethod(paymentMethod)
        }

        override fun onError(
            errorCode: Int,
            errorMessage: String,
            stripeError: StripeError?
        ) {
            // No need to show this error, because it will be broadcast
            // from the CustomerSession
            activity?.setCommunicatingProgress(false)
        }
    }

    /**
     * Abstract implementation of [ApiResultCallback] that holds a [WeakReference]
     * to an `Activity` object.
     */
    private abstract class ActivityPaymentMethodCallback<A : Activity> internal constructor(
        activity: A
    ) : ApiResultCallback<PaymentMethod> {
        private val activityRef: WeakReference<A> = WeakReference(activity)

        val activity: A?
            get() = activityRef.get()
    }

    companion object {
        const val TOKEN_ADD_PAYMENT_METHOD_ACTIVITY = "AddPaymentMethodActivity"

        @Deprecated("use {@link AddPaymentMethodActivityStarter.Result}")
        const val EXTRA_NEW_PAYMENT_METHOD = "new_payment_method"
    }
}
