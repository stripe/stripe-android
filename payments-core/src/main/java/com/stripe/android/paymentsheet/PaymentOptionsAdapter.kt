package com.stripe.android.paymentsheet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.stripe.android.R
import com.stripe.android.databinding.LayoutPaymentsheetAddNewPaymentMethodItemBinding
import com.stripe.android.databinding.LayoutPaymentsheetGooglePayItemBinding
import com.stripe.android.databinding.LayoutPaymentsheetPaymentMethodItemBinding
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import kotlin.math.roundToInt
import kotlin.properties.Delegates

internal class PaymentOptionsAdapter(
    private val canClickSelectedItem: Boolean,
    val paymentOptionSelectedListener: (paymentSelection: PaymentSelection, isClick: Boolean) -> Unit,
    val addCardClickListener: View.OnClickListener
) : RecyclerView.Adapter<PaymentOptionsAdapter.PaymentOptionViewHolder>() {
    private var items: List<Item> = emptyList()
    private var selectedItemPosition: Int = NO_POSITION

    internal val selectedItem: Item? get() = items.getOrNull(selectedItemPosition)

    internal var isEnabled: Boolean by Delegates.observable(true) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    init {
        setHasStableIds(true)
    }

    fun update(
        config: FragmentConfig,
        paymentSelection: PaymentSelection? = null
    ) {
        val items = listOfNotNull(
            Item.AddCard,
            Item.GooglePay.takeIf { config.isGooglePayReady }
        ) + config.sortedPaymentMethods.map {
            Item.SavedPaymentMethod(it)
        }

        this.items = items

        onItemSelected(
            position = paymentSelection?.let { findSelectedPosition(it) }.takeIf { it != -1 }
                ?: findInitialSelectedPosition(config.savedSelection),
            isClick = false
        )

        notifyDataSetChanged()
    }

    /**
     * The initial selection position follows this prioritization:
     * 1. The index of [Item.SavedPaymentMethod] if it matches the [SavedSelection]
     * 2. The index of [Item.GooglePay] if it exists
     * 3. The index of the first [Item.SavedPaymentMethod]
     * 4. None (-1)
     */
    private fun findInitialSelectedPosition(
        savedSelection: SavedSelection
    ): Int {
        return listOfNotNull(
            // saved selection
            items.indexOfFirst { item ->
                when (savedSelection) {
                    SavedSelection.GooglePay -> item is Item.GooglePay
                    is SavedSelection.PaymentMethod -> {
                        when (item) {
                            is Item.SavedPaymentMethod -> {
                                savedSelection.id == item.paymentMethod.id
                            }
                            else -> false
                        }
                    }
                    SavedSelection.None -> false
                }
            }.takeIf { it != -1 },

            // Google Pay
            items.indexOfFirst { it is Item.GooglePay }.takeIf { it != -1 },

            // the first payment method
            items.indexOfFirst { it is Item.SavedPaymentMethod }.takeIf { it != -1 }
        ).firstOrNull() ?: NO_POSITION
    }

    /**
     * Find the index of [paymentSelection] in the current items. Return -1 if not found.
     */
    private fun findSelectedPosition(paymentSelection: PaymentSelection): Int {
        return items.indexOfFirst { item ->
            when (paymentSelection) {
                PaymentSelection.GooglePay -> item is Item.GooglePay
                is PaymentSelection.Saved -> {
                    when (item) {
                        is Item.SavedPaymentMethod -> {
                            paymentSelection.paymentMethod.id == item.paymentMethod.id
                        }
                        else -> false
                    }
                }
                else -> false
            }
        }
    }

    @VisibleForTesting
    internal fun onItemSelected(
        position: Int,
        isClick: Boolean
    ) {
        if (position != NO_POSITION &&
            (canClickSelectedItem || position != selectedItemPosition)
        ) {
            val previousSelectedIndex = selectedItemPosition
            selectedItemPosition = position

            notifyItemChanged(previousSelectedIndex)
            notifyItemChanged(position)

            val newSelectedItem = items[position]

            when (newSelectedItem) {
                Item.AddCard -> null
                Item.GooglePay -> PaymentSelection.GooglePay
                is Item.SavedPaymentMethod -> PaymentSelection.Saved(newSelectedItem.paymentMethod)
            }?.let { paymentSelection ->
                paymentOptionSelectedListener(
                    paymentSelection,
                    isClick
                )
            }
        }
    }

    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()
    override fun getItemCount(): Int = items.size
    override fun getItemViewType(position: Int): Int = items[position].viewType.ordinal

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PaymentOptionViewHolder {
        return when (ViewType.values()[viewType]) {
            ViewType.AddCard -> AddNewPaymentMethodViewHolder(parent).apply {
                itemView.setOnClickListener(addCardClickListener)
            }
            ViewType.GooglePay -> GooglePayViewHolder(parent).apply {
                itemView.setOnClickListener {
                    onItemSelected(bindingAdapterPosition, isClick = true)
                }
            }
            ViewType.SavedPaymentMethod -> SavedPaymentMethodViewHolder(parent).apply {
                itemView.setOnClickListener {
                    onItemSelected(bindingAdapterPosition, isClick = true)
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: PaymentOptionViewHolder,
        position: Int
    ) {
        val item = items[position]
        if (holder is SavedPaymentMethodViewHolder) {
            holder.setSelected(position == selectedItemPosition)

            when (item) {
                is Item.SavedPaymentMethod -> {
                    holder.bindPaymentMethod(item.paymentMethod)
                }
                else -> {
                    // noop
                }
            }
        } else if (holder is GooglePayViewHolder) {
            holder.setSelected(position == selectedItemPosition)
        }
        holder.setEnabled(isEnabled)
    }

    private class SavedPaymentMethodViewHolder(
        private val binding: LayoutPaymentsheetPaymentMethodItemBinding
    ) : PaymentOptionViewHolder(binding.root) {
        constructor(parent: ViewGroup) : this(
            LayoutPaymentsheetPaymentMethodItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        init {
            // ensure that the check icon is above the card
            binding.checkIcon.elevation = binding.card.elevation + 1
        }

        fun bindPaymentMethod(paymentMethod: PaymentMethod) {
            binding.brandIcon.setImageResource(paymentMethod.getSavedPaymentMethodIcon() ?: 0)
            binding.label.text = paymentMethod.getLabel(itemView.resources)
        }

        fun setSelected(selected: Boolean) {
            binding.root.isSelected = selected
            binding.checkIcon.isVisible = selected
            binding.card.strokeWidth = cardStrokeWidth(selected)
        }

        override fun setEnabled(enabled: Boolean) {
            binding.card.isEnabled = enabled
            binding.root.isEnabled = enabled
            binding.label.isEnabled = enabled
            binding.brandIcon.alpha = if (enabled) 1F else 0.6F
        }
    }

    private class AddNewPaymentMethodViewHolder(
        private val binding: LayoutPaymentsheetAddNewPaymentMethodItemBinding
    ) : PaymentOptionViewHolder(
        binding.root
    ) {
        constructor(parent: ViewGroup) : this(
            LayoutPaymentsheetAddNewPaymentMethodItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        override fun setEnabled(enabled: Boolean) {
            binding.card.isEnabled = enabled
            binding.root.isEnabled = enabled
            binding.label.isEnabled = enabled
            binding.plusIcon.alpha = if (enabled) 1F else 0.6F
        }
    }

    private class GooglePayViewHolder(
        private val binding: LayoutPaymentsheetGooglePayItemBinding
    ) : PaymentOptionViewHolder(
        binding.root
    ) {
        constructor(parent: ViewGroup) : this(
            LayoutPaymentsheetGooglePayItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        init {
            // ensure that the check icon is above the card
            binding.checkIcon.elevation = binding.card.elevation + 1
        }

        fun setSelected(selected: Boolean) {
            binding.root.isSelected = selected
            binding.checkIcon.isVisible = selected
            binding.card.strokeWidth = cardStrokeWidth(selected)
        }

        override fun setEnabled(enabled: Boolean) {
            binding.card.isEnabled = enabled
            binding.root.isEnabled = enabled
            binding.label.isEnabled = enabled
            binding.googlePayMark.alpha = if (enabled) 1F else 0.6F
        }
    }

    internal abstract class PaymentOptionViewHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(parent) {

        abstract fun setEnabled(enabled: Boolean)

        fun cardStrokeWidth(selected: Boolean): Int {
            return if (selected) {
                itemView.resources
                    .getDimension(R.dimen.stripe_paymentsheet_card_stroke_width_selected)
                    .roundToInt()
            } else {
                itemView.resources
                    .getDimension(R.dimen.stripe_paymentsheet_card_stroke_width)
                    .roundToInt()
            }
        }
    }

    internal enum class ViewType {
        SavedPaymentMethod,
        AddCard,
        GooglePay
    }

    internal sealed class Item {
        abstract val viewType: ViewType

        object AddCard : Item() {
            override val viewType: ViewType = ViewType.AddCard
        }

        object GooglePay : Item() {
            override val viewType: ViewType = ViewType.GooglePay
        }

        /**
         * Represents a [PaymentMethod] that is already saved and attached to the current customer.
         */
        data class SavedPaymentMethod(
            val paymentMethod: PaymentMethod
        ) : Item() {
            override val viewType: ViewType = ViewType.SavedPaymentMethod
        }
    }
}
