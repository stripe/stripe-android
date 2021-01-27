package com.stripe.android.paymentsheet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.databinding.LayoutPaymentsheetAddCardItemBinding
import com.stripe.android.databinding.LayoutPaymentsheetGooglePayItemBinding
import com.stripe.android.databinding.LayoutPaymentsheetPaymentMethodItemBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlin.properties.Delegates

internal class PaymentOptionsAdapter(
    private val canClickSelectedItem: Boolean,
    private var paymentSelection: PaymentSelection?,
    val paymentOptionSelectedListener: (PaymentSelection, Boolean) -> Unit,
    val addCardClickListener: View.OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var shouldShowGooglePay: Boolean by Delegates.observable(false) { _, _, _ ->
        notifyDataSetChanged()
    }

    var paymentMethods: List<PaymentMethod> = emptyList()
        set(value) {
            val sortedPaymentMethods = sortPaymentMethods(value)

            if (sortedPaymentMethods != field) {
                field = sortedPaymentMethods
                notifyDataSetChanged()
                updatePaymentSelection(defaultPaymentMethodId)
            }
        }

    var defaultPaymentMethodId: String? by Delegates.observable(
        null
    ) { _, oldDefaultPaymentMethodId, newDefaultPaymentMethodId ->
        updatePaymentSelection(newDefaultPaymentMethodId)

        paymentMethods = sortPaymentMethods(paymentMethods)
        if (newDefaultPaymentMethodId != oldDefaultPaymentMethodId) {
            notifyDataSetChanged()
        }
    }

    private val selectedPaymentMethod: PaymentMethod? get() = (paymentSelection as? PaymentSelection.Saved)?.paymentMethod

    private val googlePayCount: Int get() = 1.takeIf { shouldShowGooglePay } ?: 0

    init {
        setHasStableIds(true)
    }

    private fun onPaymentMethodSelected(
        clickedPaymentMethod: PaymentMethod,
        isClick: Boolean
    ) {
        // allowed to click the selected item or a new Payment Method was selected
        if (canClickSelectedItem || selectedPaymentMethod?.id != clickedPaymentMethod.id
        ) {
            selectedPaymentMethod?.let {
                notifyItemChanged(getPosition(it))
            }
            notifyItemChanged(getPosition(clickedPaymentMethod))
            val paymentSelection = PaymentSelection.Saved(clickedPaymentMethod).also {
                this.paymentSelection = it
            }

            // unselect Google Pay if Google Pay is enabled
            if (shouldShowGooglePay) {
                notifyItemChanged(GOOGLE_PAY_POSITION)
            }

            paymentOptionSelectedListener(paymentSelection, isClick)
        }
    }

    private fun onGooglePaySelected() {
        if (paymentSelection != PaymentSelection.GooglePay) {
            // unselect previous item
            val previouslySelectedPaymentMethod = selectedPaymentMethod
            paymentSelection = PaymentSelection.GooglePay
            previouslySelectedPaymentMethod?.let {
                notifyItemChanged(getPosition(it))
            }

            // select Google Pay item
            notifyItemChanged(GOOGLE_PAY_POSITION)
            paymentOptionSelectedListener(PaymentSelection.GooglePay, true)
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
            ViewType.GooglePay -> GooglePayViewHolder(parent)
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
                    getPaymentMethodAtPosition(holder.adapterPosition),
                    true
                )
            }
        } else if (holder is GooglePayViewHolder) {
            holder.setSelected(paymentSelection == PaymentSelection.GooglePay)
            holder.itemView.setOnClickListener {
                onGooglePaySelected()
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

    private fun updatePaymentSelection(
        paymentMethodId: String?
    ) {
        paymentSelection = paymentMethods.firstOrNull { it.id == paymentMethodId }?.let {
            PaymentSelection.Saved(it)
        }
        paymentSelection?.let {
            paymentOptionSelectedListener(it, false)
        }
    }

    private fun sortPaymentMethods(
        paymentMethods: List<PaymentMethod>
    ): List<PaymentMethod> {
        val primaryPaymentMethodIndex = paymentMethods.indexOfFirst {
            it.id == defaultPaymentMethodId
        }
        return if (primaryPaymentMethodIndex != -1) {
            val mutablePaymentMethods = paymentMethods.toMutableList()
            mutablePaymentMethods.removeAt(primaryPaymentMethodIndex)
                .also { primaryPaymentMethod ->
                    mutablePaymentMethods.add(0, primaryPaymentMethod)
                }
            mutablePaymentMethods
        } else {
            paymentMethods
        }
    }

    private class CardViewHolder(
        private val binding: LayoutPaymentsheetPaymentMethodItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        constructor(parent: ViewGroup) : this(
            LayoutPaymentsheetPaymentMethodItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        init {
            binding.card.setOnClickListener {
                // TODO(mshafrir-stripe): Card view was ignoring clicks without this - investigate?
                itemView.performClick()
            }

            // ensure that the check icon is above the card
            binding.checkIcon.elevation = binding.card.elevation + 1
        }

        fun setPaymentMethod(method: PaymentMethod) {
            // TODO: Communicate error if card data not present
            method.card?.let { card ->
                binding.brandIcon.setImageResource(
                    when (card.brand) {
                        CardBrand.Visa -> R.drawable.stripe_ic_paymentsheet_card_visa
                        CardBrand.AmericanExpress -> R.drawable.stripe_ic_paymentsheet_card_amex
                        CardBrand.Discover -> R.drawable.stripe_ic_paymentsheet_card_discover
                        CardBrand.JCB -> R.drawable.stripe_ic_paymentsheet_card_jcb
                        CardBrand.DinersClub -> R.drawable.stripe_ic_paymentsheet_card_dinersclub
                        CardBrand.MasterCard -> R.drawable.stripe_ic_paymentsheet_card_mastercard
                        CardBrand.UnionPay -> R.drawable.stripe_ic_paymentsheet_card_unionpay
                        CardBrand.Unknown -> R.drawable.stripe_ic_paymentsheet_card_unknown
                    }
                )
                binding.label.text = itemView.context
                    .getString(R.string.paymentsheet_payment_method_item_card_number, card.last4)
            }
        }

        fun setSelected(selected: Boolean) {
            binding.card.isChecked = selected
            binding.checkIcon.isVisible = selected
        }
    }

    private class AddCardViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutPaymentsheetAddCardItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).root
    )

    private class GooglePayViewHolder(
        private val binding: LayoutPaymentsheetGooglePayItemBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {
        constructor(parent: ViewGroup) : this(
            LayoutPaymentsheetGooglePayItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        init {
            binding.card.setOnClickListener {
                // TODO(mshafrir-stripe): Card view was ignoring clicks without this - investigate?
                itemView.performClick()
            }

            // ensure that the check icon is above the card
            binding.checkIcon.elevation = binding.card.elevation + 1
        }

        fun setSelected(selected: Boolean) {
            binding.card.isChecked = selected
            binding.checkIcon.isVisible = selected
        }
    }

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
