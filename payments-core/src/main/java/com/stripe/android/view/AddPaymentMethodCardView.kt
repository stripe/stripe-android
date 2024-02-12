package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.stripe.android.databinding.StripeAddPaymentMethodCardViewBinding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams

/**
 * View for adding a payment method of type [PaymentMethod.Type.Card].
 *
 * See [AddPaymentMethodActivity] for usage.
 */
internal class AddPaymentMethodCardView @JvmOverloads internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val billingAddressFields: BillingAddressFields = BillingAddressFields.PostalCode
) : AddPaymentMethodView(context, attrs, defStyleAttr) {
    private val cardMultilineWidget: CardMultilineWidget
    private val billingAddressWidget: ShippingInfoWidget

    override val createParams: PaymentMethodCreateParams?
        get() {
            return when (billingAddressFields) {
                BillingAddressFields.Full -> {
                    val paymentMethodCard = cardMultilineWidget.paymentMethodCard
                    val billingDetails = this.billingDetails
                    if (paymentMethodCard != null && billingDetails != null) {
                        PaymentMethodCreateParams.create(
                            paymentMethodCard,
                            billingDetails
                        )
                    } else {
                        null
                    }
                }
                BillingAddressFields.None -> {
                    cardMultilineWidget.paymentMethodCreateParams
                }
                BillingAddressFields.PostalCode -> {
                    cardMultilineWidget.paymentMethodCreateParams
                }
            }
        }

    private val billingDetails: PaymentMethod.BillingDetails?
        get() {
            return if (billingAddressFields == BillingAddressFields.Full) {
                billingAddressWidget.shippingInformation?.let {
                    PaymentMethod.BillingDetails.create(it)
                }
            } else {
                null
            }
        }

    init {
        val viewBinding = StripeAddPaymentMethodCardViewBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        cardMultilineWidget = viewBinding.cardMultilineWidget
        cardMultilineWidget.setShouldShowPostalCode(
            billingAddressFields == BillingAddressFields.PostalCode
        )

        billingAddressWidget = viewBinding.billingAddressWidget
        if (billingAddressFields == BillingAddressFields.Full) {
            billingAddressWidget.visibility = View.VISIBLE
        }

        (context as? AddPaymentMethodActivity?)?.let {
            initEnterListeners(it)
        }
    }

    private fun initEnterListeners(activity: AddPaymentMethodActivity) {
        val listener = OnEditorActionListenerImpl(
            activity,
            this,
            KeyboardController(activity)
        )

        cardMultilineWidget.cardNumberEditText
            .setOnEditorActionListener(listener)
        cardMultilineWidget.expiryDateEditText
            .setOnEditorActionListener(listener)
        cardMultilineWidget.cvcEditText
            .setOnEditorActionListener(listener)
        cardMultilineWidget.postalCodeEditText
            .setOnEditorActionListener(listener)
    }

    override fun setCommunicatingProgress(communicating: Boolean) {
        cardMultilineWidget.isEnabled = !communicating
    }

    fun setCardInputListener(listener: CardInputListener?) {
        cardMultilineWidget.setCardInputListener(listener)
    }

    internal class OnEditorActionListenerImpl(
        private val activity: AddPaymentMethodActivity,
        private val addPaymentMethodCardView: AddPaymentMethodCardView,
        private val keyboardController: KeyboardController
    ) : TextView.OnEditorActionListener {

        override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (addPaymentMethodCardView.createParams != null) {
                    keyboardController.hide()
                }
                activity.onActionSave()
                return true
            }
            return false
        }
    }
}
