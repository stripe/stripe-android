package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.stripe.android.R
import com.stripe.android.databinding.AddPaymentMethodRowBinding
import com.stripe.android.model.PaymentMethod

@SuppressWarnings("ViewConstructor")
internal class AddPaymentMethodRowView private constructor(
    context: Context,
    @IdRes idRes: Int,
    @StringRes private val labelRes: Int,
    private val args: AddPaymentMethodActivityStarter.Args
) : FrameLayout(context) {

    private val viewBinding = AddPaymentMethodRowBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {
        id = idRes
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        isFocusable = true
        isClickable = true

        contentDescription = context.getString(labelRes)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewBinding.label.setText(labelRes)

        (context as? Activity)?.let { activity ->
            setOnClickListener {
                AddPaymentMethodActivityStarter(activity).startForResult(args)
            }
        }
    }

    internal companion object {
        internal fun createCard(
            activity: Activity,
            args: PaymentMethodsActivityStarter.Args
        ): AddPaymentMethodRowView {
            return AddPaymentMethodRowView(
                activity,
                R.id.stripe_payment_methods_add_card,
                R.string.payment_method_add_new_card,
                AddPaymentMethodActivityStarter.Args.Builder()
                    .setBillingAddressFields(args.billingAddressFields)
                    .setShouldAttachToCustomer(true)
                    .setIsPaymentSessionActive(args.isPaymentSessionActive)
                    .setPaymentMethodType(PaymentMethod.Type.Card)
                    .setAddPaymentMethodFooter(args.addPaymentMethodFooterLayoutId)
                    .setPaymentConfiguration(args.paymentConfiguration)
                    .setWindowFlags(args.windowFlags)
                    .build()
            )
        }

        internal fun createFpx(
            activity: Activity,
            args: PaymentMethodsActivityStarter.Args
        ): AddPaymentMethodRowView {
            return AddPaymentMethodRowView(
                activity,
                R.id.stripe_payment_methods_add_fpx,
                R.string.payment_method_add_new_fpx,
                AddPaymentMethodActivityStarter.Args.Builder()
                    .setIsPaymentSessionActive(args.isPaymentSessionActive)
                    .setPaymentMethodType(PaymentMethod.Type.Fpx)
                    .setPaymentConfiguration(args.paymentConfiguration)
                    .build()
            )
        }
    }
}
