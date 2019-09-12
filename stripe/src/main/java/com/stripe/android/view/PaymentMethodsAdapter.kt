package com.stripe.android.view

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import java.util.ArrayList

/**
 * A [RecyclerView.Adapter] that holds a set of [MaskedCardView] items for a given set
 * of [PaymentMethod] objects.
 */
internal class PaymentMethodsAdapter @JvmOverloads constructor(
    private val initiallySelectedPaymentMethodId: String?,
    private val intentArgs: PaymentMethodsActivityStarter.Args,
    private val addableTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val paymentMethods = ArrayList<PaymentMethod>()

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
        return paymentMethods.size + addableTypes.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < paymentMethods.size) {
            val type = paymentMethods[position].type
            if (PaymentMethod.Type.Card.code == type) {
                TYPE_CARD
            } else {
                super.getItemViewType(position)
            }
        } else {
            val paymentMethodType = addableTypes[getAddableTypesPosition(position)]
            return when (paymentMethodType) {
                PaymentMethod.Type.Card -> TYPE_ADD_CARD
                PaymentMethod.Type.Fpx -> TYPE_ADD_FPX
                else ->
                    throw IllegalArgumentException(
                        "Unsupported PaymentMethod type: ${paymentMethodType.code}")
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position < paymentMethods.size) {
            paymentMethods[position].hashCode().toLong()
        } else {
            addableTypes[getAddableTypesPosition(position)].hashCode().toLong()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PaymentMethodViewHolder) {
            holder.setPaymentMethod(paymentMethods[position])
            holder.setSelected(position == selectedIndex)
            holder.itemView.setOnClickListener {
                onPositionClicked(holder.adapterPosition)
            }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CARD -> createPaymentMethodViewHolder(parent)
            TYPE_ADD_CARD -> createAddCardPaymentMethodViewHolder(parent)
            TYPE_ADD_FPX -> createAddFpxPaymentMethodViewHolder(parent)
            else -> throw IllegalArgumentException("Unsupported viewType: $viewType")
        }
    }

    private fun createAddCardPaymentMethodViewHolder(parent: ViewGroup): AddCardPaymentMethodViewHolder {
        return AddCardPaymentMethodViewHolder(
            AddPaymentMethodCardRowView(parent.context as Activity, intentArgs)
        )
    }

    private fun createAddFpxPaymentMethodViewHolder(parent: ViewGroup): AddFpxPaymentMethodViewHolder {
        return AddFpxPaymentMethodViewHolder(
            AddPaymentMethodFpxRowView(parent.context as Activity, intentArgs)
        )
    }

    private fun createPaymentMethodViewHolder(parent: ViewGroup): PaymentMethodViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.masked_card_row, parent, false)
        return PaymentMethodViewHolder(itemView)
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

    private fun getAddableTypesPosition(position: Int) = position - paymentMethods.size

    internal class PaymentMethodViewHolder constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaskedCardView = itemView.findViewById(R.id.masked_card_item)

        fun setPaymentMethod(paymentMethod: PaymentMethod) {
            cardView.setPaymentMethod(paymentMethod)
        }

        fun setSelected(selected: Boolean) {
            cardView.isSelected = selected
        }
    }

    internal class AddCardPaymentMethodViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView)

    internal class AddFpxPaymentMethodViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView)

    interface Listener {
        fun onClick(paymentMethod: PaymentMethod)
    }

    companion object {
        private const val NO_SELECTION = -1

        private const val TYPE_CARD = 1
        private const val TYPE_ADD_CARD = 2
        private const val TYPE_ADD_FPX = 3
    }
}
