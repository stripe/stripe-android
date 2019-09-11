package com.stripe.android.view

import android.os.Handler
import android.os.Looper
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
internal class PaymentMethodsAdapter constructor(
    private val initiallySelectedPaymentMethodId: String?
) : RecyclerView.Adapter<PaymentMethodsAdapter.ViewHolder>() {
    private val paymentMethods = ArrayList<PaymentMethod>()
    private var selectedIndex = NO_SELECTION
    var listener: Listener? = null
    private val handler = Handler(Looper.getMainLooper())

    private val newestPaymentMethodIndex: Int
        get() {
            var index = NO_SELECTION
            var created = 0L

            paymentMethods.forEachIndexed { i, paymentMethod ->
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
        this.paymentMethods.clear()
        this.paymentMethods.addAll(paymentMethods)

        // if there were no selected payment methods, or the previously selected payment method
        // was not found and set selected, select the newest payment method
        setSelectedPaymentMethod(initiallySelectedPaymentMethodId)

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
            onPositionClicked(holder.adapterPosition)
        }
    }

    private fun onPositionClicked(position: Int) {
        if (selectedIndex != position) {
            // selected a Payment Method that wasn't previously selected
            notifyItemChanged(position)
            notifyItemChanged(selectedIndex)
            setSelectedIndex(position)
        }

        handler.post {
            listener?.onClick(paymentMethods[position])
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
     * Sets the selected payment method based on ID, or most recently created
     *
     * @param paymentMethodId the ID of the [PaymentMethod] to select
     */
    private fun setSelectedPaymentMethod(paymentMethodId: String?) {
        val indexToSelect = paymentMethodId?.let {
            paymentMethods.indexOfFirst { paymentMethodId == it.id }
        } ?: NO_SELECTION

        setSelectedIndex(
            if (indexToSelect >= 0) {
                indexToSelect
            } else {
                newestPaymentMethodIndex
            }
        )
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

    interface Listener {
        fun onClick(paymentMethod: PaymentMethod)
    }

    companion object {
        private const val TYPE_CARD = 0
        private const val NO_SELECTION = -1
    }
}
