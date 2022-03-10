package com.stripe.android.paymentsheet

import android.annotation.SuppressLint
import android.content.res.Resources
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsAdapter.Companion.PM_OPTIONS_DEFAULT_PADDING
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.ui.LpmSelectorText
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import com.stripe.android.ui.core.PaymentsTheme
import kotlin.properties.Delegates

@SuppressLint("NotifyDataSetChanged")
internal class PaymentOptionsAdapter(
    private val canClickSelectedItem: Boolean,
    val paymentOptionSelectedListener:
        (paymentSelection: PaymentSelection, isClick: Boolean) -> Unit,
    val paymentMethodDeleteListener:
        (paymentMethod: Item.SavedPaymentMethod) -> Unit,
    val addCardClickListener: () -> Unit
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
        val width = calculateViewWidth(parent)
        return when (ViewType.values()[viewType]) {
            ViewType.AddCard ->
                AddNewPaymentMethodViewHolder(parent, width, addCardClickListener)
            ViewType.GooglePay ->
                GooglePayViewHolder(parent, width, ::onItemSelected)
            ViewType.SavedPaymentMethod ->
                SavedPaymentMethodViewHolder(parent, width, ::onItemSelected) { position ->
                    paymentMethodDeleteListener(items[position] as Item.SavedPaymentMethod)
                }
        }
    }

    override fun onBindViewHolder(
        holder: PaymentOptionViewHolder,
        position: Int
    ) {
        // Saved methods are still enabled while editing.
        val enabled = if (holder is SavedPaymentMethodViewHolder) {
            isEnabled
        } else {
            isEnabled && !isEditing
        }

        holder.bind(
            isSelected = position == selectedItemPosition && !isEditing,
            isEnabled = enabled,
            isEditing = isEditing,
            item = items[position],
            position = position
        )
    }

    override fun onViewRecycled(holder: PaymentOptionViewHolder) {
        holder.onViewRecycled()
        super.onViewRecycled(holder)
    }

    @VisibleForTesting
    internal class SavedPaymentMethodViewHolder(
        private val composeView: ComposeView,
        private val width: Dp,
        private val onRemoveListener: (Int) -> Unit,
        private val onItemSelectedListener: ((Int, Boolean) -> Unit)
    ) : PaymentOptionViewHolder(
        composeView
    ) {
        constructor(
            parent: ViewGroup,
            width: Dp,
            onItemSelectedListener: ((Int, Boolean) -> Unit),
            onRemoveListener: (Int) -> Unit
        ) : this (
            composeView = ComposeView(parent.context),
            width = width,
            onRemoveListener = onRemoveListener,
            onItemSelectedListener = onItemSelectedListener
        )

        override fun bind(
            isSelected: Boolean,
            isEnabled: Boolean,
            isEditing: Boolean,
            item: Item,
            position: Int
        ) {
            val savedPaymentMethod = item as Item.SavedPaymentMethod
            val labelText = savedPaymentMethod.paymentMethod.getLabel(itemView.resources) ?: return

            composeView.setContent {
                PaymentOptionUi(
                    viewWidth = width,
                    isEditing = isEditing,
                    isSelected = isSelected,
                    isEnabled = isEnabled,
                    iconRes = savedPaymentMethod.paymentMethod.getSavedPaymentMethodIcon() ?: 0,
                    labelText = labelText,
                    accessibilityDescription = item.getDescription(itemView.resources),
                    onRemoveListener = { onRemoveListener(position) },
                    onRemoveAccessibilityDescription =
                    savedPaymentMethod.getRemoveDescription(itemView.resources),
                    onItemSelectedListener = { onItemSelectedListener(position, true) },
                )
            }
        }
    }

    @VisibleForTesting
    internal class AddNewPaymentMethodViewHolder(
        private val composeView: ComposeView,
        private val width: Dp,
        private val onItemSelectedListener: () -> Unit
    ) : PaymentOptionViewHolder(
        composeView
    ) {
        constructor(parent: ViewGroup, width: Dp, onItemSelectedListener: () -> Unit) : this(
            composeView = ComposeView(parent.context),
            width = width,
            onItemSelectedListener = onItemSelectedListener
        )

        override fun bind(
            isSelected: Boolean,
            isEnabled: Boolean,
            isEditing: Boolean,
            item: Item,
            position: Int
        ) {
            composeView.setContent {
                PaymentOptionUi(
                    viewWidth = width,
                    isEditing = false,
                    isSelected = false,
                    isEnabled = isEnabled,
                    labelText =
                    itemView.resources.getString(
                        R.string.stripe_paymentsheet_add_payment_method_button_label
                    ),
                    iconRes = R.drawable.stripe_ic_paymentsheet_add,
                    onItemSelectedListener = { onItemSelectedListener() },
                    accessibilityDescription =
                    itemView.resources.getString(R.string.add_new_payment_method),
                )
            }
        }
    }

    @VisibleForTesting
    internal class GooglePayViewHolder(
        private val composeView: ComposeView,
        private val width: Dp,
        private val onItemSelectedListener: ((Int, Boolean) -> Unit)
    ) : PaymentOptionViewHolder(
        composeView
    ) {
        constructor(
            parent: ViewGroup,
            width: Dp,
            onItemSelectedListener: ((Int, Boolean) -> Unit)
        ) : this(
            composeView = ComposeView(parent.context),
            width = width,
            onItemSelectedListener = onItemSelectedListener
        )

        override fun bind(
            isSelected: Boolean,
            isEnabled: Boolean,
            isEditing: Boolean,
            item: Item,
            position: Int
        ) {
            composeView.setContent {
                PaymentOptionUi(
                    viewWidth = width,
                    isEditing = false,
                    isSelected = isSelected,
                    isEnabled = isEnabled,
                    iconRes = R.drawable.stripe_google_pay_mark,
                    labelText = itemView.resources.getString(R.string.google_pay),
                    accessibilityDescription = itemView.resources.getString(R.string.google_pay),
                    onItemSelectedListener = { onItemSelectedListener(position, true) },
                )
            }
        }
    }

    internal abstract class PaymentOptionViewHolder(
        private val composeView: ComposeView,
    ) : RecyclerView.ViewHolder(composeView) {
        abstract fun bind(
            isSelected: Boolean,
            isEnabled: Boolean,
            isEditing: Boolean,
            item: Item,
            position: Int
        )

        init {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
        }

        fun onViewRecycled() {
            // Dispose the underlying Composition of the ComposeView
            // when RecyclerView has recycled this ViewHolder
            composeView.disposeComposition()
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

            fun getRemoveDescription(resources: Resources) = resources.getString(
                R.string.stripe_paymentsheet_remove_pm,
                getDescription(resources)
            )
        }
    }
    internal companion object {
        private fun calculateViewWidth(parent: ViewGroup): Dp {
            val targetWidth = parent.measuredWidth - parent.paddingStart - parent.paddingEnd
            val screenDensity = parent.context.resources.displayMetrics.density
            // minimum width for each item, accounting for the CardView margin so that the CardView
            // is at least 100dp wide
            val minItemWidth = 100 * screenDensity + (2 * PM_OPTIONS_DEFAULT_PADDING)
            // numVisibleItems is incremented in steps of 0.5 items (1, 1.5, 2, 2.5, 3, ...)
            val numVisibleItems = (targetWidth * 2 / minItemWidth).toInt() / 2f
            val viewWidth = (targetWidth / numVisibleItems)
            return (viewWidth / screenDensity).dp
        }

        internal const val PM_OPTIONS_DEFAULT_PADDING = 6.0F
    }
}

@Composable
internal fun PaymentOptionUi(
    viewWidth: Dp,
    isSelected: Boolean,
    isEditing: Boolean,
    isEnabled: Boolean,
    iconRes: Int,
    accessibilityDescription: String,
    labelText: String = "",
    onRemoveListener: (() -> Unit)? = null,
    onRemoveAccessibilityDescription: String = "",
    onItemSelectedListener: (() -> Unit)
) {
    // An attempt was made to not use constraint layout here but it was unsuccessful in
    // precisely positioning the check and delete icons to match the mocks.
    ConstraintLayout(
        modifier = Modifier
            .padding(top = 12.dp)
            .width(viewWidth)
            .alpha(alpha = if (isEnabled) 1.0F else 0.6F)
            .selectable(selected = isSelected, enabled = isEnabled, onClick = {
                onItemSelectedListener()
            })
    ) {
        val (checkIcon, deleteIcon, label, card) = createRefs()
        Card(
            border = PaymentsTheme.getBorderStroke(isSelected),
            elevation = 2.dp,
            backgroundColor = PaymentsTheme.colors.colorComponentBackground,
            modifier = Modifier
                .height(64.dp)
                .padding(horizontal = PM_OPTIONS_DEFAULT_PADDING.dp)
                .fillMaxWidth()
                .constrainAs(card) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .height(40.dp)
                        .width(56.dp)
                )
            }
        }

        if (isSelected) {
            Image(
                painter = painterResource(R.drawable.stripe_ic_check_circle),
                contentDescription = null,

                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color = PaymentsTheme.colors.material.primary)
                    .constrainAs(checkIcon) {
                        top.linkTo(card.bottom, (-12).dp)
                        end.linkTo(card.end)
                    }
            )
        }
        if (isEditing && onRemoveListener != null) {
            Image(
                painter = painterResource(R.drawable.stripe_ic_delete_symbol),
                contentDescription = onRemoveAccessibilityDescription,
                modifier = Modifier
                    .constrainAs(deleteIcon) {
                        top.linkTo(card.top, margin = (-9).dp)
                        end.linkTo(card.end)
                    }
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color = PaymentsTheme.colors.material.error)
                    .clickable(
                        onClick = {
                            onRemoveListener()
                        }
                    )
            )
        }

        LpmSelectorText(
            text = labelText,
            textColor = PaymentsTheme.colors.material.onPrimary,
            isEnabled = isEnabled,
            modifier = Modifier
                .constrainAs(label) {
                    top.linkTo(card.bottom)
                    start.linkTo(card.start)
                }
                .padding(
                    top = 4.dp,
                    start = PM_OPTIONS_DEFAULT_PADDING.dp,
                    end = PM_OPTIONS_DEFAULT_PADDING.dp
                )
                .semantics {
                    // This makes the screen reader read out numbers digit by digit
                    // one one one one vs one thousand one hundred eleven
                    this.contentDescription =
                        accessibilityDescription.replace("\\d".toRegex(), "$0 ")
                },
        )
    }
}
