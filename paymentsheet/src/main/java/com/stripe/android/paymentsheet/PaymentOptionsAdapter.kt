package com.stripe.android.paymentsheet

import android.annotation.SuppressLint
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.IMPORTANT_FOR_ACCESSIBILITY_NO
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.databinding.LayoutPaymentsheetAddNewPaymentMethodItemBinding
import com.stripe.android.paymentsheet.databinding.LayoutPaymentsheetGooglePayItemBinding
import com.stripe.android.paymentsheet.databinding.LayoutPaymentsheetPaymentMethodItemBinding
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import kotlin.math.roundToInt
import kotlin.properties.Delegates

@SuppressLint("NotifyDataSetChanged")
internal class PaymentOptionsAdapter(
    private val canClickSelectedItem: Boolean,
    val paymentOptionSelectedListener:
        (paymentSelection: PaymentSelection, isClick: Boolean) -> Unit,
    val paymentMethodDeleteListener:
        (paymentMethod: Item.SavedPaymentMethod) -> Unit,
    val addCardClickListener: View.OnClickListener
) : RecyclerView.Adapter<PaymentOptionsAdapter.PaymentOptionViewHolder>() {
    @VisibleForTesting
    internal var items: List<Item> = emptyList()
    private var selectedItemPosition: Int = NO_POSITION
    private var isEditing = false

    internal val selectedItem: Item? get() = items.getOrNull(selectedItemPosition)

    internal var isEnabled: Boolean by Delegates.observable(true) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    init {
        setHasStableIds(true)
    }

    fun setEditing(editing: Boolean) {
        if (editing != isEditing) {
            isEditing = editing
            notifyDataSetChanged()
        }
    }

    fun setItems(
        config: FragmentConfig,
        paymentMethods: List<PaymentMethod>,
        paymentSelection: PaymentSelection? = null
    ) {
        val items = listOfNotNull(
            Item.AddCard,
            Item.GooglePay.takeIf { config.isGooglePayReady }
        ) + sortedPaymentMethods(paymentMethods, config.savedSelection).map {
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

    fun removeItem(item: Item) {
        val itemIndex = items.indexOf(item)
        items = items.toMutableList().apply { removeAt(itemIndex) }
        notifyItemRemoved(itemIndex)
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
                val b = when (savedSelection) {
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
                b
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

    private fun sortedPaymentMethods(
        paymentMethods: List<PaymentMethod>,
        savedSelection: SavedSelection
    ): List<PaymentMethod> {
        val primaryPaymentMethodIndex = when (savedSelection) {
            is SavedSelection.PaymentMethod -> {
                paymentMethods.indexOfFirst {
                    it.id == savedSelection.id
                }
            }
            else -> -1
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

    @VisibleForTesting
    internal fun onItemSelected(
        position: Int,
        isClick: Boolean
    ) {
        if (position != NO_POSITION &&
            (canClickSelectedItem || position != selectedItemPosition) &&
            !isEditing
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
            ViewType.SavedPaymentMethod -> SavedPaymentMethodViewHolder(parent) { position ->
                paymentMethodDeleteListener(items[position] as Item.SavedPaymentMethod)
            }.apply {
                itemView.setOnClickListener {
                    onItemSelected(bindingAdapterPosition, isClick = true)
                }
            }
        }.apply {
            val targetWidth = parent.measuredWidth - parent.paddingStart - parent.paddingEnd
            // minimum width for each item, accounting for the CardView margin so that the CardView
            // is at least 100dp wide
            val minItemWidth = 100 * parent.context.resources.displayMetrics.density +
                cardView.marginEnd + cardView.marginStart
            // numVisibleItems is incremented in steps of 0.5 items (1, 1.5, 2, 2.5, 3, ...)
            val numVisibleItems = (targetWidth * 2 / minItemWidth).toInt() / 2f
            val viewWidth = targetWidth / numVisibleItems
            itemView.layoutParams.width = viewWidth.toInt()
        }
    }

    override fun onBindViewHolder(
        holder: PaymentOptionViewHolder,
        position: Int
    ) {
        val item = items[position]
        when (holder) {
            is SavedPaymentMethodViewHolder -> {
                holder.bindSavedPaymentMethod(item as Item.SavedPaymentMethod)
                holder.setSelected(position == selectedItemPosition && !isEditing)
                holder.setEnabled(isEnabled)
                holder.setEditing(isEditing)
            }
            is GooglePayViewHolder -> {
                holder.setSelected(position == selectedItemPosition && !isEditing)
                holder.setEnabled(isEnabled && !isEditing)
            }
            else -> {
                holder.setEnabled(isEnabled && !isEditing)
            }
        }
    }

    internal class SavedPaymentMethodViewHolder(
        internal val binding: LayoutPaymentsheetPaymentMethodItemBinding,
        private val onRemoveListener: (Int) -> Unit
    ) : PaymentOptionViewHolder(binding.root) {
        constructor(parent: ViewGroup, onRemoveListener: (Int) -> Unit) : this(
            LayoutPaymentsheetPaymentMethodItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onRemoveListener
        )

        override val cardView: View
            get() = binding.card

        init {
            // ensure that the icons are above the card
            binding.checkIcon.elevation = binding.card.elevation + 1
            binding.deleteIcon.elevation = binding.card.elevation + 1
            binding.deleteIcon.setOnClickListener {
                onRemoveListener(absoluteAdapterPosition)
            }
        }

        fun bindSavedPaymentMethod(item: Item.SavedPaymentMethod) {
            binding.brandIcon.setImageResource(item.paymentMethod.getSavedPaymentMethodIcon() ?: 0)
            binding.label.text = item.paymentMethod.getLabel(itemView.resources)
            binding.root.contentDescription = item.getDescription(itemView.resources)
            binding.deleteIcon.contentDescription =
                itemView.resources.getString(
                    R.string.stripe_paymentsheet_remove_pm,
                    item.getDescription(itemView.resources)
                )
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

        fun setEditing(editing: Boolean) {
            binding.deleteIcon.isVisible = editing
            binding.root.importantForAccessibility = if (editing) {
                IMPORTANT_FOR_ACCESSIBILITY_NO
            } else {
                IMPORTANT_FOR_ACCESSIBILITY_YES
            }
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

        override val cardView: View
            get() = binding.card

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

        override val cardView: View
            get() = binding.card

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

        abstract val cardView: View
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

            fun getDescription(resources: Resources) = when (paymentMethod.type) {
                PaymentMethod.Type.Card -> resources.getString(
                    R.string.card_ending_in,
                    paymentMethod.card?.brand,
                    paymentMethod.card?.last4
                )
                PaymentMethod.Type.SepaDebit -> resources.getString(
                    R.string.bank_account_ending_in,
                    paymentMethod.sepaDebit?.last4
                )
                else -> ""
            }
        }
    }
}
