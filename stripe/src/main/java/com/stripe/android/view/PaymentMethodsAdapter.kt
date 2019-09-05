package com.stripe.android.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import java.util.ArrayList

/**
 * A [RecyclerView.Adapter] that holds a set of [MaskedCardView] items for a given set
 * of [PaymentMethod] objects.
 */
internal class PaymentMethodsAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<PaymentMethodsAdapter.ViewHolder>() {
    private val paymentMethods = ArrayList<PaymentMethod>()
    private var selectedIndex = NO_SELECTION

    private val newestPaymentMethodIndex: Int
        get() {
            var index = NO_SELECTION
            var created = 0L
            for (i in paymentMethods.indices) {
                val paymentMethod = paymentMethods[i]
                if (paymentMethod.created != null && paymentMethod.created > created) {
                    created = paymentMethod.created
                    index = i
                }
            }

            return index
        }

    val selectedPaymentMethod: PaymentMethod?
        get() = if (selectedIndex == NO_SELECTION) {
            null
        } else paymentMethods[selectedIndex]

    init {
        setHasStableIds(true)
    }

    fun setPaymentMethods(paymentMethods: List<PaymentMethod>) {
        val selectedPaymentMethod = selectedPaymentMethod
        val selectedPaymentMethodId = selectedPaymentMethod?.id

        this.paymentMethods.clear()
        this.paymentMethods.addAll(paymentMethods)

        // if there were no selected payment methods, or the previously selected payment method
        // was not found and set selected, select the newest payment method
        if (selectedPaymentMethodId == null || !setSelectedPaymentMethod(selectedPaymentMethodId)) {
            setSelectedIndex(newestPaymentMethodIndex)
        }

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return paymentMethods.size
    }

    override fun getItemViewType(position: Int): Int {
        val type = paymentMethods[position].type
        return if (PaymentMethod.Type.Card.code == type) {
            TYPE_CARD
        } else {
            super.getItemViewType(position)
        }
    }

    override fun getItemId(position: Int): Long {
        return paymentMethods[position].id.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setPaymentMethod(paymentMethods[position])
        holder.setSelected(position == selectedIndex)
        holder.itemView.setOnClickListener {
            val currentPosition = holder.adapterPosition
            if (currentPosition != selectedIndex) {
                val prevSelectedIndex = selectedIndex
                setSelectedIndex(currentPosition)

                notifyItemChanged(prevSelectedIndex)
                notifyItemChanged(currentPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        @LayoutRes val layoutRes: Int
        if (viewType == TYPE_CARD) {
            layoutRes = R.layout.masked_card_row
        } else {
            throw IllegalArgumentException("Unsupported type: $viewType")
        }
        val itemView = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return ViewHolder(itemView)
    }

    /**
     * Sets the selected payment method based on ID.
     *
     * @param paymentMethodId the ID of the [PaymentMethod] to select
     * @return `true` if the value was found, `false` if not
     */
    fun setSelectedPaymentMethod(paymentMethodId: String): Boolean {
        for (i in paymentMethods.indices) {
            if (paymentMethodId == paymentMethods[i].id) {
                setSelectedIndex(i)
                return true
            }
        }
        return false
    }

    fun setSelectedIndex(selectedIndex: Int) {
        this.selectedIndex = selectedIndex
    }

    internal class ViewHolder constructor(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val cardView: MaskedCardView = itemView.findViewById(R.id.masked_card_item)

        fun setPaymentMethod(paymentMethod: PaymentMethod) {
            cardView.setPaymentMethod(paymentMethod)
        }

        fun setSelected(selected: Boolean) {
            cardView.isSelected = selected
        }
    }

    companion object {
        private const val TYPE_CARD = 0
        private const val NO_SELECTION = -1
    }
}
