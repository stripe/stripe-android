package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import kotlinx.android.synthetic.main.add_payment_method_row.view.*

@SuppressWarnings("ViewConstructor")
internal class AddPaymentMethodRowView private constructor(
    context: Context,
    @IdRes idRes: Int,
    @StringRes private val labelRes: Int,
    private val args: AddPaymentMethodActivityStarter.Args
) : FrameLayout(context) {

    init {
        View.inflate(context, R.layout.add_payment_method_row, this)
        id = idRes
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        isFocusable = true
        isClickable = true

        contentDescription = context.getString(labelRes)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        label.setText(labelRes)

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
