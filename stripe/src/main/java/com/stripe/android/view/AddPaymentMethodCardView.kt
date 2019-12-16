package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.stripe.android.R
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
    private val billingAddressFields: BillingAddressFields
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
        View.inflate(getContext(), R.layout.add_payment_method_card_layout, this)
        cardMultilineWidget = findViewById(R.id.card_multiline_widget)
        cardMultilineWidget.setShouldShowPostalCode(
            billingAddressFields == BillingAddressFields.PostalCode
        )

        billingAddressWidget = findViewById(R.id.billing_address_widget)
        if (billingAddressFields == BillingAddressFields.Full) {
            billingAddressWidget.visibility = View.VISIBLE
        }

        initEnterListeners()
    }

    private fun initEnterListeners() {
        val activity = context as AddPaymentMethodActivity

        val listener = OnEditorActionListenerImpl(
            activity,
            this,
            KeyboardController(activity)
        )

        cardMultilineWidget.findViewById<TextView>(R.id.et_card_number)
            .setOnEditorActionListener(listener)
        cardMultilineWidget.findViewById<TextView>(R.id.et_expiry)
            .setOnEditorActionListener(listener)
        cardMultilineWidget.findViewById<TextView>(R.id.et_cvc)
            .setOnEditorActionListener(listener)
        cardMultilineWidget.findViewById<TextView>(R.id.et_postal_code)
            .setOnEditorActionListener(listener)
    }

    override fun setCommunicatingProgress(communicating: Boolean) {
        cardMultilineWidget.isEnabled = !communicating
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
