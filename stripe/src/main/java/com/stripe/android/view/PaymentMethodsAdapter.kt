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
internal class PaymentMethodsAdapter constructor(
    private val intentArgs: PaymentMethodsActivityStarter.Args,
    private val addableTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card),
    initiallySelectedPaymentMethodId: String? = null
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
                ViewType.Card.ordinal
            } else {
                super.getItemViewType(position)
            }
        } else {
            val paymentMethodType =
                addableTypes[getAddableTypesPosition(position)]
            return when (paymentMethodType) {
                PaymentMethod.Type.Card -> ViewType.AddCard.ordinal
                PaymentMethod.Type.Fpx -> ViewType.AddFpx.ordinal
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
        if (holder is ViewHolder.PaymentMethodViewHolder) {
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (ViewType.values()[viewType]) {
            ViewType.Card -> createPaymentMethodViewHolder(parent)
            ViewType.AddCard -> createAddCardPaymentMethodViewHolder(parent)
            ViewType.AddFpx -> createAddFpxPaymentMethodViewHolder(parent)
            ViewType.GooglePay -> createGooglePayViewHolder(parent)
        }
    }

    private fun createAddCardPaymentMethodViewHolder(
        parent: ViewGroup
    ): ViewHolder.AddCardPaymentMethodViewHolder {
        return ViewHolder.AddCardPaymentMethodViewHolder(
            AddPaymentMethodCardRowView(parent.context as Activity, intentArgs)
        )
    }

    private fun createAddFpxPaymentMethodViewHolder(
        parent: ViewGroup
    ): ViewHolder.AddFpxPaymentMethodViewHolder {
        return ViewHolder.AddFpxPaymentMethodViewHolder(
            AddPaymentMethodFpxRowView(parent.context as Activity, intentArgs)
        )
    }

    private fun createPaymentMethodViewHolder(
        parent: ViewGroup
    ): ViewHolder.PaymentMethodViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.masked_card_row, parent, false)
        return ViewHolder.PaymentMethodViewHolder(itemView)
    }

    private fun createGooglePayViewHolder(
        parent: ViewGroup
    ): ViewHolder.GooglePayViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.google_pay_row, parent, false)
        return ViewHolder.GooglePayViewHolder(itemView)
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

    private fun getAddableTypesPosition(position: Int): Int {
        return position - paymentMethods.size
    }

    internal sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal class AddCardPaymentMethodViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView)

        internal class AddFpxPaymentMethodViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView)

        internal class GooglePayViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView)

        internal class PaymentMethodViewHolder constructor(
            itemView: View
        ) : ViewHolder(itemView) {
            private val cardView: MaskedCardView = itemView.findViewById(R.id.masked_card_item)

            fun setPaymentMethod(paymentMethod: PaymentMethod) {
                cardView.setPaymentMethod(paymentMethod)
            }

            fun setSelected(selected: Boolean) {
                cardView.isSelected = selected
            }
        }
    }

    internal interface Listener {
        fun onClick(paymentMethod: PaymentMethod)
    }

    private enum class ViewType {
        Card,
        AddCard,
        AddFpx,
        GooglePay
    }
}
