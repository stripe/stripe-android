package com.stripe.android.paymentsheet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.stripe.android.R
import com.stripe.android.databinding.LayoutPaymentsheetAddCardItemBinding
import com.stripe.android.databinding.LayoutPaymentsheetGooglePayItemBinding
import com.stripe.android.databinding.LayoutPaymentsheetPaymentMethodItemBinding
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlin.properties.Delegates

internal class PaymentOptionsAdapter(
    private val canClickSelectedItem: Boolean,
    val paymentOptionSelectedListener: (paymentSelection: PaymentSelection, isClick: Boolean) -> Unit,
    val addCardClickListener: View.OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
        config: FragmentConfig
    ) {
        val items = listOfNotNull(
            Item.AddCard,
            Item.GooglePay.takeIf { config.isGooglePayReady }
        ) + config.sortedPaymentMethods.map {
            Item.ExistingPaymentMethod(it)
        }

        this.items = items

        onItemSelected(
            position = findInitialSelectedPosition(config.savedSelection),
            isClick = false
        )

        notifyDataSetChanged()
    }

    /**
     * The initial selection position follows this prioritization:
     * 1. The index of [Item.ExistingPaymentMethod] if it matches the [SavedSelection]
     * 2. The index of [Item.GooglePay] if it exists
     * 3. The index of the first [Item.ExistingPaymentMethod]
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
                            is Item.ExistingPaymentMethod -> {
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
            items.indexOfFirst { it is Item.ExistingPaymentMethod }.takeIf { it != -1 }
        ).firstOrNull() ?: NO_POSITION
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
                is Item.ExistingPaymentMethod -> PaymentSelection.Saved(newSelectedItem.paymentMethod)
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
    ): RecyclerView.ViewHolder {
        return when (ViewType.values()[viewType]) {
            ViewType.AddCard -> AddCardViewHolder(parent).apply {
                itemView.setOnClickListener(addCardClickListener)
            }
            ViewType.GooglePay -> GooglePayViewHolder(parent).apply {
                itemView.setOnClickListener {
                    onItemSelected(bindingAdapterPosition, isClick = true)
                }
            }
            ViewType.Card -> CardViewHolder(parent).apply {
                itemView.setOnClickListener {
                    onItemSelected(bindingAdapterPosition, isClick = true)
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val item = items[position]
        if (holder is CardViewHolder) {
            holder.setSelected(position == selectedItemPosition)

            when (item) {
                is Item.ExistingPaymentMethod -> {
                    holder.bindPaymentMethod(item.paymentMethod)
                }
                else -> {
                    // noop
                }
            }
        } else if (holder is GooglePayViewHolder) {
            holder.setSelected(position == selectedItemPosition)
        }
        holder.itemView.isEnabled = isEnabled
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
            // ensure that the check icon is above the card
            binding.checkIcon.elevation = binding.card.elevation + 1
        }

        fun bindPaymentMethod(method: PaymentMethod) {
            // TODO: Communicate error if card data not present
            method.card?.let { card ->
                bind(
                    brand = card.brand,
                    last4 = card.last4
                )
            }
        }

        private fun bind(
            brand: CardBrand,
            last4: String?
        ) {
            binding.brandIcon.setImageResource(
                when (brand) {
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
                .getString(R.string.paymentsheet_payment_method_item_card_number, last4)
        }

        fun setSelected(selected: Boolean) {
            binding.card.isSelected = selected
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
            // ensure that the check icon is above the card
            binding.checkIcon.elevation = binding.card.elevation + 1
        }

        fun setSelected(selected: Boolean) {
            binding.card.isSelected = selected
            binding.checkIcon.isVisible = selected
        }
    }

    internal enum class ViewType {
        Card,
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

        data class ExistingPaymentMethod(
            val paymentMethod: PaymentMethod
        ) : Item() {
            override val viewType: ViewType = ViewType.Card
        }
    }
}
