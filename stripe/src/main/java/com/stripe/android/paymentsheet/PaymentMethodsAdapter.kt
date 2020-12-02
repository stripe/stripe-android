package com.stripe.android.paymentsheet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.databinding.LayoutPaymentsheetAddCardItemBinding
import com.stripe.android.databinding.LayoutPaymentsheetGooglePayItemBinding
import com.stripe.android.databinding.LayoutPaymentsheetPaymentMethodItemBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlin.properties.Delegates

internal class PaymentMethodsAdapter(
    private var paymentSelection: PaymentSelection?,
    val paymentMethodSelectedListener: (PaymentSelection) -> Unit,
    val addCardClickListener: View.OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var shouldShowGooglePay: Boolean by Delegates.observable(false) { _, _, _ ->
        notifyDataSetChanged()
    }
    var paymentMethods: List<PaymentMethod> by Delegates.observable(
        emptyList()
    ) { _, _, _ ->
        notifyDataSetChanged()
    }

    private val selectedPaymentMethod: PaymentMethod? get() = (paymentSelection as? PaymentSelection.Saved)?.paymentMethod

    private val googlePayCount: Int get() = 1.takeIf { shouldShowGooglePay } ?: 0

    init {
        setHasStableIds(true)
    }

    private fun onPaymentMethodSelected(
        clickedPaymentMethod: PaymentMethod
    ) {
        if (selectedPaymentMethod?.id != clickedPaymentMethod.id) {
            // selected a new Payment Method
            selectedPaymentMethod?.let {
                notifyItemChanged(getPosition(it))
            }
            notifyItemChanged(getPosition(clickedPaymentMethod))
            val paymentSelection = PaymentSelection.Saved(clickedPaymentMethod).also {
                this.paymentSelection = it
            }
            paymentMethodSelectedListener(paymentSelection)
        }
    }

    private fun onGooglePaySelected() {
        if (paymentSelection != PaymentSelection.GooglePay) {
            // unselect item
            selectedPaymentMethod?.let {
                notifyItemChanged(getPosition(it))
            }
            notifyItemChanged(GOOGLE_PAY_POSITION)
            paymentSelection = PaymentSelection.GooglePay
            paymentMethodSelectedListener(PaymentSelection.GooglePay)
        }
    }

    override fun getItemId(position: Int): Long {
        return if (shouldShowGooglePay) {
            when (position) {
                ADD_NEW_POSITION -> ADD_NEW_ID
                GOOGLE_PAY_POSITION -> GOOGLE_PAY_ID
                else -> getPaymentMethodAtPosition(position).hashCode().toLong()
            }
        } else {
            when (position) {
                ADD_NEW_POSITION -> ADD_NEW_ID
                else -> getPaymentMethodAtPosition(position).hashCode().toLong()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (ViewType.values()[viewType]) {
            ViewType.GooglePay -> GooglePayViewHolder(parent).apply {
                itemView.setOnClickListener {
                    onGooglePaySelected()
                }
            }
            ViewType.Card -> CardViewHolder(parent)
            ViewType.AddCard -> AddCardViewHolder(parent).apply {
                itemView.setOnClickListener(addCardClickListener)
            }
        }
    }

    override fun getItemCount(): Int {
        return listOfNotNull(
            1, // Add new item
            googlePayCount, // Google Pay item
            paymentMethods.size
        ).sum()
    }

    override fun getItemViewType(position: Int): Int {
        val type = if (shouldShowGooglePay) {
            when (position) {
                ADD_NEW_POSITION -> ViewType.AddCard
                GOOGLE_PAY_POSITION -> ViewType.GooglePay
                else -> ViewType.Card
            }
        } else {
            when (position) {
                ADD_NEW_POSITION -> ViewType.AddCard
                else -> ViewType.Card
            }
        }
        return type.ordinal
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CardViewHolder) {
            val paymentMethod = getPaymentMethodAtPosition(position)
            holder.setPaymentMethod(paymentMethod)
            holder.setSelected(paymentMethod.id == selectedPaymentMethod?.id)
            holder.itemView.setOnClickListener {
                onPaymentMethodSelected(
                    getPaymentMethodAtPosition(holder.adapterPosition)
                )
            }
        }
    }

    /**
     * Given an adapter position, translate to a `paymentMethods` element
     */
    @JvmSynthetic
    internal fun getPaymentMethodAtPosition(position: Int): PaymentMethod {
        return paymentMethods[getPaymentMethodIndex(position)]
    }

    /**
     * Given an adapter position, translate to a `paymentMethods` index
     */
    private fun getPaymentMethodIndex(position: Int): Int {
        return position - googlePayCount - 1
    }

    private fun getPosition(paymentMethod: PaymentMethod): Int {
        return paymentMethods.indexOf(paymentMethod).let {
            it + googlePayCount + 1
        }
    }

    private class CardViewHolder(
        private val binding: LayoutPaymentsheetPaymentMethodItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        constructor(parent: ViewGroup) : this(
            LayoutPaymentsheetPaymentMethodItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        fun setPaymentMethod(method: PaymentMethod) {
            // TODO: Communicate error if card data not present
            method.card?.let { card ->
                // TODO: Get updated card brand icons
                binding.brandIcon.setImageResource(
                    when (card.brand) {
                        CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa
                        else -> R.drawable.stripe_ic_paymentsheet_card_visa
                    }
                )
                binding.label.text = itemView.context
                    .getString(R.string.paymentsheet_payment_method_item_card_number, card.last4)
            }
        }

        fun setSelected(selected: Boolean) {
            binding.checkIcon.visibility = if (selected) View.VISIBLE else View.GONE
        }
    }

    private class AddCardViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutPaymentsheetAddCardItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).root
    )

    private class GooglePayViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        // TODO(mshafrir-stripe): add check icon
        LayoutPaymentsheetGooglePayItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).root
    )

    internal enum class ViewType {
        Card,
        AddCard,
        GooglePay
    }

    internal companion object {
        internal const val ADD_NEW_ID = 1234L
        internal const val GOOGLE_PAY_ID = 1235L

        private const val ADD_NEW_POSITION = 0
        private const val GOOGLE_PAY_POSITION = 1
    }
}
