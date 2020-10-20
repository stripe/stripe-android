package com.stripe.android.paymentsheet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.databinding.LayoutPaymentsheetAddCardItemBinding
import com.stripe.android.databinding.LayoutPaymentsheetPaymentMethodItemBinding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlin.properties.Delegates

internal class PaymentSheetPaymentMethodsAdapter(
    selectedPaymentMethod: PaymentSelection?,
    val paymentMethodSelectedListener: (PaymentSelection) -> Unit,
    val addCardClickListener: View.OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var paymentMethods: List<PaymentMethod> by Delegates.observable(
        emptyList()
    ) { _, _, _ ->
        notifyDataSetChanged()
    }

    private var selectedPaymentMethodId: String? = (selectedPaymentMethod as? PaymentSelection.Saved)?.paymentMethodId

    init {
        setHasStableIds(true)
    }

    private fun updateSelectedPaymentMethod(position: Int) {
        val currentlySelectedPosition = paymentMethods.indexOfFirst {
            it.id == selectedPaymentMethodId
        }
        if (currentlySelectedPosition != position) {
            // selected a new Payment Method
            notifyItemChanged(currentlySelectedPosition)
            notifyItemChanged(position)
            selectedPaymentMethodId = paymentMethods.getOrNull(position)?.id
            selectedPaymentMethodId?.let {
                paymentMethodSelectedListener(PaymentSelection.Saved(it))
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return when (position) {
            paymentMethods.size -> ADD_CARD_ID
            else -> paymentMethods[position].hashCode().toLong()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // TODO: Support GooglePay
        return when (ViewType.values()[viewType]) {
            ViewType.Card -> CardViewHolder(parent)
            ViewType.AddCard -> AddCardViewHolder(parent).apply {
                itemView.setOnClickListener(addCardClickListener)
            }
            else -> throw IllegalStateException("Unsupported view type")
        }
    }

    override fun getItemCount(): Int {
        return paymentMethods.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        // TODO: Support GooglePay
        val type = when (position) {
            paymentMethods.size -> ViewType.AddCard
            else -> ViewType.Card
        }
        return type.ordinal
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CardViewHolder) {
            val paymentMethod = paymentMethods[position]
            holder.setPaymentMethod(paymentMethod)
            holder.setSelected(paymentMethod.id == selectedPaymentMethodId)
            holder.itemView.setOnClickListener {
                updateSelectedPaymentMethod(holder.adapterPosition)
            }
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
                binding.brandIcon.setImageResource(card.brand.icon)
                binding.cardNumber.text = itemView.context
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

    private enum class ViewType {
        Card,
        AddCard,
        GooglePay
    }

    private companion object {
        private const val ADD_CARD_ID = 1234L
    }
}
