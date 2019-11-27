package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams

/**
 * View for adding a payment method of type [PaymentMethod.Type.Card].
 *
 * See [AddPaymentMethodActivity] for usage.
 */
internal class AddPaymentMethodCardView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    shouldShowPostalCode: Boolean
) : AddPaymentMethodView(context, attrs, defStyleAttr) {
    private val cardMultilineWidget: CardMultilineWidget

    override val createParams: PaymentMethodCreateParams?
        get() = cardMultilineWidget.paymentMethodCreateParams

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        this(context, attrs, defStyleAttr, false)

    init {
        View.inflate(getContext(), R.layout.add_payment_method_card_layout, this)
        cardMultilineWidget = findViewById(R.id.card_multiline_widget)
        cardMultilineWidget.setShouldShowPostalCode(shouldShowPostalCode)
        initEnterListeners()
    }

    private fun initEnterListeners() {
        val activity = context as AddPaymentMethodActivity

        val listener = OnEditorActionListenerImpl(
            activity,
            this,
            activity
                .getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
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
        private val inputMethodManager: InputMethodManager
    ) : TextView.OnEditorActionListener {

        override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (addPaymentMethodCardView.createParams != null) {
                    inputMethodManager.hideSoftInputFromWindow(activity.windowToken, 0)
                }
                activity.onActionSave()
                return true
            }
            return false
        }
    }

    companion object {
        @JvmStatic
        fun create(context: Context, shouldShowPostalCode: Boolean): AddPaymentMethodCardView {
            return AddPaymentMethodCardView(context, null, 0, shouldShowPostalCode)
        }
    }
}
