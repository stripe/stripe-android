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
internal class PaymentMethodsAdapter @JvmOverloads internal constructor(
    initiallySelectedPaymentMethodId: String?,
    private val intentArgs: PaymentMethodsActivityStarter.Args,
    private val addableTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal val paymentMethods = ArrayList<PaymentMethod>()
    internal var selectedPaymentMethodId: String? = initiallySelectedPaymentMethodId
    internal val selectedPaymentMethod: PaymentMethod?
        get() {
            return selectedPaymentMethodId?.let { selectedPaymentMethodId ->
                paymentMethods.firstOrNull { it.id == selectedPaymentMethodId }
            }
        }

    internal var listener: Listener? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        setHasStableIds(true)
    }

    @JvmSynthetic
    internal fun setPaymentMethods(paymentMethods: List<PaymentMethod>) {
        this.paymentMethods.clear()
        this.paymentMethods.addAll(paymentMethods)
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
            val paymentMethod = paymentMethods[position]
            holder.setPaymentMethod(paymentMethod)
            holder.setSelected(paymentMethod.id == selectedPaymentMethodId)
            holder.itemView.setOnClickListener {
                onPositionClicked(holder.adapterPosition)
            }
        }
    }

    private fun onPositionClicked(position: Int) {
        updateSelectedPaymentMethod(position)
        handler.post {
            listener?.onClick(paymentMethods[position])
        }
    }

    private fun updateSelectedPaymentMethod(position: Int) {
        val currentlySelectedPosition = paymentMethods.indexOfFirst {
            it.id == selectedPaymentMethodId
        }
        if (currentlySelectedPosition != position) {
            // selected a new Payment Method
            notifyItemChanged(currentlySelectedPosition)
            selectedPaymentMethodId = paymentMethods.getOrNull(position)?.id
        }

        // Notify the current position even if it's the currently selected position so that the
        // ItemAnimator defined in PaymentMethodActivity is triggered.
        notifyItemChanged(position)
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

    internal fun deletePaymentMethod(paymentMethod: PaymentMethod) {
        val indexToDelete = paymentMethods.indexOfFirst { it.id == paymentMethod.id }
        if (indexToDelete >= 0) {
            paymentMethods.removeAt(indexToDelete)
            notifyItemRemoved(indexToDelete)
        }
    }

    internal fun resetPaymentMethod(paymentMethod: PaymentMethod) {
        val indexToReset = paymentMethods.indexOfFirst { it.id == paymentMethod.id }
        if (indexToReset >= 0) {
            notifyItemChanged(indexToReset)
        }
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

    internal interface Listener {
        fun onClick(paymentMethod: PaymentMethod)
    }

    private companion object {
        private const val TYPE_CARD = 1
        private const val TYPE_ADD_CARD = 2
        private const val TYPE_ADD_FPX = 3
    }
}
