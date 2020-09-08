package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.databinding.GooglePayRowBinding
import com.stripe.android.databinding.MaskedCardRowBinding
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * A [RecyclerView.Adapter] that holds a set of [MaskedCardView] items for a given set
 * of [PaymentMethod] objects.
 */
internal class PaymentMethodsAdapter constructor(
    private val intentArgs: PaymentMethodsActivityStarter.Args,
    private val addableTypes: List<PaymentMethod.Type> = listOf(PaymentMethod.Type.Card),
    initiallySelectedPaymentMethodId: String? = null,
    private val shouldShowGooglePay: Boolean = false,
    private val useGooglePay: Boolean = false,
    private val canDeletePaymentMethods: Boolean = true,
    private val workContext: CoroutineContext = Dispatchers.IO
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal val paymentMethods = mutableListOf<PaymentMethod>()
    internal var selectedPaymentMethodId: String? = initiallySelectedPaymentMethodId
    internal val selectedPaymentMethod: PaymentMethod?
        get() {
            return selectedPaymentMethodId?.let { selectedPaymentMethodId ->
                paymentMethods.firstOrNull { it.id == selectedPaymentMethodId }
            }
        }

    internal var listener: Listener? = null
    private val googlePayCount = 1.takeIf { shouldShowGooglePay } ?: 0

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
        return paymentMethods.size + addableTypes.size + googlePayCount
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isGooglePayPosition(position) -> ViewType.GooglePay.ordinal
            isPaymentMethodsPosition(position) -> {
                val type = getPaymentMethodAtPosition(position).type
                if (PaymentMethod.Type.Card == type) {
                    ViewType.Card.ordinal
                } else {
                    super.getItemViewType(position)
                }
            }
            else -> {
                val paymentMethodType =
                    addableTypes[getAddableTypesPosition(position)]
                return when (paymentMethodType) {
                    PaymentMethod.Type.Card -> ViewType.AddCard.ordinal
                    PaymentMethod.Type.Fpx -> ViewType.AddFpx.ordinal
                    else ->
                        throw IllegalArgumentException(
                            "Unsupported PaymentMethod type: ${paymentMethodType.code}"
                        )
                }
            }
        }
    }

    private fun isGooglePayPosition(position: Int): Boolean {
        return shouldShowGooglePay && position == 0
    }

    private fun isPaymentMethodsPosition(position: Int): Boolean {
        val range = if (shouldShowGooglePay) {
            1..paymentMethods.size
        } else {
            0 until paymentMethods.size
        }
        return position in range
    }

    override fun getItemId(position: Int): Long {
        return when {
            isGooglePayPosition(position) ->
                GOOGLE_PAY_ITEM_ID
            isPaymentMethodsPosition(position) ->
                getPaymentMethodAtPosition(position).hashCode().toLong()
            else ->
                addableTypes[getAddableTypesPosition(position)].code.hashCode().toLong()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder.PaymentMethodViewHolder) {
            val paymentMethod = getPaymentMethodAtPosition(position)
            holder.setPaymentMethod(paymentMethod)
            holder.setSelected(paymentMethod.id == selectedPaymentMethodId)
            holder.itemView.setOnClickListener {
                onPositionClicked(holder.adapterPosition)
            }
        } else if (holder is ViewHolder.GooglePayViewHolder) {
            holder.itemView.setOnClickListener {
                selectedPaymentMethodId = null
                listener?.onGooglePayClick()
            }
            holder.bind(useGooglePay)
        }
    }

    @JvmSynthetic
    internal fun onPositionClicked(position: Int) {
        updateSelectedPaymentMethod(position)
        CoroutineScope(workContext).launch {
            delay(0)

            withContext(Dispatchers.Main) {
                listener?.onPaymentMethodClick(getPaymentMethodAtPosition(position))
            }
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
            AddPaymentMethodRowView.createCard(parent.context as Activity, intentArgs)
        )
    }

    private fun createAddFpxPaymentMethodViewHolder(
        parent: ViewGroup
    ): ViewHolder.AddFpxPaymentMethodViewHolder {
        return ViewHolder.AddFpxPaymentMethodViewHolder(
            AddPaymentMethodRowView.createFpx(parent.context as Activity, intentArgs)
        )
    }

    private fun createPaymentMethodViewHolder(
        parent: ViewGroup
    ): ViewHolder.PaymentMethodViewHolder {
        val viewHolder = ViewHolder.PaymentMethodViewHolder(parent)

        if (canDeletePaymentMethods) {
            ViewCompat.addAccessibilityAction(
                viewHolder.itemView,
                parent.context.getString(R.string.delete_payment_method)
            ) { _, _ ->
                listener?.onDeletePaymentMethodAction(
                    paymentMethod = getPaymentMethodAtPosition(viewHolder.adapterPosition)
                )
                true
            }
        }
        return viewHolder
    }

    private fun createGooglePayViewHolder(
        parent: ViewGroup
    ): ViewHolder.GooglePayViewHolder {
        return ViewHolder.GooglePayViewHolder(parent.context, parent)
    }

    @JvmSynthetic
    internal fun deletePaymentMethod(paymentMethod: PaymentMethod) {
        getPosition(paymentMethod)?.let {
            paymentMethods.remove(paymentMethod)
            notifyItemRemoved(it)
        }
    }

    @JvmSynthetic
    internal fun resetPaymentMethod(paymentMethod: PaymentMethod) {
        getPosition(paymentMethod)?.let {
            notifyItemChanged(it)
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
        return position - googlePayCount
    }

    /**
     * Given a Payment Method, get its adapter position. For example, if the Google Pay button is
     * being shown, the 2nd element in [paymentMethods] is actually the 3rd item in the adapter.
     */
    internal fun getPosition(paymentMethod: PaymentMethod): Int? {
        return paymentMethods.indexOf(paymentMethod).takeIf { it >= 0 }?.let {
            it + googlePayCount
        }
    }

    private fun getAddableTypesPosition(position: Int): Int {
        return position - paymentMethods.size - googlePayCount
    }

    internal sealed class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal class AddCardPaymentMethodViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView)

        internal class AddFpxPaymentMethodViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView)

        internal class GooglePayViewHolder(
            private val viewBinding: GooglePayRowBinding
        ) : RecyclerView.ViewHolder(viewBinding.root) {
            constructor(context: Context, parent: ViewGroup) : this(
                GooglePayRowBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )

            private val themeConfig = ThemeConfig(itemView.context)

            init {
                ImageViewCompat.setImageTintList(
                    viewBinding.checkIcon,
                    ColorStateList.valueOf(themeConfig.getTintColor(true))
                )
            }

            fun bind(isSelected: Boolean) {
                viewBinding.label.setTextColor(
                    ColorStateList.valueOf(themeConfig.getTextColor(isSelected))
                )

                viewBinding.checkIcon.visibility = if (isSelected) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

                itemView.isSelected = isSelected
            }
        }

        internal class PaymentMethodViewHolder constructor(
            private val viewBinding: MaskedCardRowBinding
        ) : ViewHolder(viewBinding.root) {
            constructor(parent: ViewGroup) : this(
                MaskedCardRowBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            fun setPaymentMethod(paymentMethod: PaymentMethod) {
                viewBinding.maskedCardItem.setPaymentMethod(paymentMethod)
            }

            fun setSelected(selected: Boolean) {
                viewBinding.maskedCardItem.isSelected = selected
                itemView.isSelected = selected
            }
        }
    }

    internal interface Listener {
        fun onPaymentMethodClick(paymentMethod: PaymentMethod)
        fun onGooglePayClick()
        fun onDeletePaymentMethodAction(paymentMethod: PaymentMethod)
    }

    internal enum class ViewType {
        Card,
        AddCard,
        AddFpx,
        GooglePay
    }

    internal companion object {
        internal val GOOGLE_PAY_ITEM_ID = "pm_google_pay".hashCode().toLong()
    }
}
