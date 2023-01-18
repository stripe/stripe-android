package com.stripe.android.paymentsheet

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.stripe.android.paymentsheet.PaymentOptionsAdapter.Companion.PM_OPTIONS_DEFAULT_PADDING
import com.stripe.android.paymentsheet.PaymentOptionsItem.ViewType
import com.stripe.android.paymentsheet.ui.LpmSelectorText
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.android.paymentsheet.ui.getLabelIcon
import com.stripe.android.paymentsheet.ui.getSavedPaymentMethodIcon
import com.stripe.android.ui.core.elements.SimpleDialogElementUI
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.stripeColors
import kotlin.properties.Delegates

@SuppressLint("NotifyDataSetChanged")
internal class PaymentOptionsAdapter(
    private val lpmRepository: LpmRepository,
    private val canClickSelectedItem: Boolean,
    val paymentOptionSelected: (PaymentOptionsItem) -> Unit,
    val paymentMethodDeleteListener: (PaymentOptionsItem.SavedPaymentMethod) -> Unit,
    val addCardClickListener: () -> Unit,
) : RecyclerView.Adapter<PaymentOptionsAdapter.PaymentOptionViewHolder>() {

    @VisibleForTesting
    internal var items: List<PaymentOptionsItem> = emptyList()
    private var selectedItemPosition: Int = NO_POSITION
    private var isEditing = false

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

    fun update(
        items: List<PaymentOptionsItem>,
        selectedIndex: Int,
    ) {
        this.items = items
        this.selectedItemPosition = selectedIndex
        notifyDataSetChanged()
    }

    fun hasSavedItems(): Boolean {
        return items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>().isNotEmpty()
    }

    @VisibleForTesting
    internal fun onItemSelected(position: Int) {
        val isAllowed = canClickSelectedItem || position != selectedItemPosition

        if (position != NO_POSITION && isAllowed && !isEditing) {
            val item = items[position]
            paymentOptionSelected(item)
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
            ViewType.AddCard -> {
                AddNewPaymentMethodViewHolder(parent, width, addCardClickListener)
            }
            ViewType.GooglePay -> {
                GooglePayViewHolder(parent, width, ::onItemSelected)
            }
            ViewType.Link -> {
                LinkViewHolder(parent, width, ::onItemSelected)
            }
            ViewType.SavedPaymentMethod -> {
                SavedPaymentMethodViewHolder(
                    parent,
                    width,
                    lpmRepository,
                    onItemSelectedListener = ::onItemSelected,
                    onRemoveListener = { position ->
                        val removedItem = items[position] as PaymentOptionsItem.SavedPaymentMethod
                        paymentMethodDeleteListener(removedItem)
                    }
                )
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
        private val lpmRepository: LpmRepository,
        @get:VisibleForTesting internal val onRemoveListener: (Int) -> Unit,
        private val onItemSelectedListener: (Int) -> Unit
    ) : PaymentOptionViewHolder(
        composeView
    ) {
        constructor(
            parent: ViewGroup,
            width: Dp,
            lpmRepository: LpmRepository,
            onItemSelectedListener: (Int) -> Unit,
            onRemoveListener: (Int) -> Unit
        ) : this(
            composeView = ComposeView(parent.context),
            width = width,
            lpmRepository = lpmRepository,
            onRemoveListener = onRemoveListener,
            onItemSelectedListener = onItemSelectedListener
        )

        override fun bind(
            isSelected: Boolean,
            isEnabled: Boolean,
            isEditing: Boolean,
            item: PaymentOptionsItem,
            position: Int
        ) {
            val savedPaymentMethod = item as PaymentOptionsItem.SavedPaymentMethod
            val labelIcon = savedPaymentMethod.paymentMethod.getLabelIcon()
            val labelText = savedPaymentMethod.paymentMethod.getLabel(itemView.resources) ?: return
            val removeTitle = itemView.resources.getString(
                R.string.stripe_paymentsheet_remove_pm,
                lpmRepository.fromCode(item.paymentMethod.type?.code)
                    ?.run {
                        itemView.resources.getString(
                            displayNameResource
                        )
                    }
            )

            composeView.setContent {
                StripeTheme {
                    PaymentOptionUi(
                        viewWidth = width,
                        isEditing = isEditing,
                        isSelected = isSelected,
                        isEnabled = isEnabled,
                        iconRes = savedPaymentMethod.paymentMethod.getSavedPaymentMethodIcon() ?: 0,
                        labelIcon = labelIcon,
                        labelText = labelText,
                        removePmDialogTitle = removeTitle,
                        description = item.getDescription(itemView.resources),
                        onRemoveListener = { onRemoveListener(position) },
                        onRemoveAccessibilityDescription =
                        savedPaymentMethod.getRemoveDescription(itemView.resources),
                        onItemSelectedListener = { onItemSelectedListener(position) }
                    )
                }
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
            item: PaymentOptionsItem,
            position: Int
        ) {
            composeView.setContent {
                StripeTheme {
                    val iconRes = if (
                        MaterialTheme.stripeColors.component.shouldUseDarkDynamicColor()
                    ) {
                        R.drawable.stripe_ic_paymentsheet_add_dark
                    } else {
                        R.drawable.stripe_ic_paymentsheet_add_light
                    }
                    PaymentOptionUi(
                        viewWidth = width,
                        isEditing = false,
                        isSelected = false,
                        isEnabled = isEnabled,
                        labelText = itemView.resources.getString(
                            R.string.stripe_paymentsheet_add_payment_method_button_label
                        ),
                        iconRes = iconRes,
                        onItemSelectedListener = onItemSelectedListener,
                        description =
                        itemView.resources.getString(R.string.add_new_payment_method)
                    )
                }
            }
        }
    }

    @VisibleForTesting
    internal class GooglePayViewHolder(
        private val composeView: ComposeView,
        private val width: Dp,
        private val onItemSelectedListener: (Int) -> Unit
    ) : PaymentOptionViewHolder(
        composeView
    ) {
        constructor(
            parent: ViewGroup,
            width: Dp,
            onItemSelectedListener: (Int) -> Unit
        ) : this(
            composeView = ComposeView(parent.context),
            width = width,
            onItemSelectedListener = onItemSelectedListener
        )

        override fun bind(
            isSelected: Boolean,
            isEnabled: Boolean,
            isEditing: Boolean,
            item: PaymentOptionsItem,
            position: Int
        ) {
            composeView.setContent {
                StripeTheme {
                    PaymentOptionUi(
                        viewWidth = width,
                        isEditing = false,
                        isSelected = isSelected,
                        isEnabled = isEnabled,
                        iconRes = R.drawable.stripe_google_pay_mark,
                        labelText = itemView.resources.getString(R.string.google_pay),
                        description = itemView.resources.getString(R.string.google_pay),
                        onItemSelectedListener = { onItemSelectedListener(position) }
                    )
                }
            }
        }
    }

    @VisibleForTesting
    internal class LinkViewHolder(
        private val composeView: ComposeView,
        private val width: Dp,
        private val onItemSelectedListener: (Int) -> Unit
    ) : PaymentOptionViewHolder(
        composeView
    ) {
        constructor(
            parent: ViewGroup,
            width: Dp,
            onItemSelectedListener: (Int) -> Unit
        ) : this(
            composeView = ComposeView(parent.context),
            width = width,
            onItemSelectedListener = onItemSelectedListener
        )

        override fun bind(
            isSelected: Boolean,
            isEnabled: Boolean,
            isEditing: Boolean,
            item: PaymentOptionsItem,
            position: Int
        ) {
            composeView.setContent {
                StripeTheme {
                    PaymentOptionUi(
                        viewWidth = width,
                        isEditing = false,
                        isSelected = isSelected,
                        isEnabled = isEnabled,
                        iconRes = R.drawable.stripe_link_mark,
                        labelText = itemView.resources.getString(R.string.link),
                        description = itemView.resources.getString(R.string.link),
                        onItemSelectedListener = { onItemSelectedListener(position) }
                    )
                }
            }
        }
    }

    internal abstract class PaymentOptionViewHolder(
        private val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView) {
        abstract fun bind(
            isSelected: Boolean,
            isEnabled: Boolean,
            isEditing: Boolean,
            item: PaymentOptionsItem,
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

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
const val PAYMENT_OPTION_CARD_TEST_TAG = "PAYMENT_OPTION_CARD_TEST_TAG"

@Composable
internal fun PaymentOptionUi(
    viewWidth: Dp,
    isSelected: Boolean,
    isEditing: Boolean,
    isEnabled: Boolean,
    iconRes: Int,
    @DrawableRes labelIcon: Int? = null,
    labelText: String = "",
    removePmDialogTitle: String = "",
    description: String,
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
    ) {
        val (checkIcon, deleteIcon, label, card) = createRefs()
        SectionCard(
            isSelected = isSelected,
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
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PAYMENT_OPTION_CARD_TEST_TAG + labelText)
                    .selectable(
                        selected = isSelected,
                        enabled = isEnabled,
                        onClick = onItemSelectedListener,
                    ),
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
            val iconColor = MaterialTheme.colors.primary
            val checkSymbolColor = if (iconColor.shouldUseDarkDynamicColor()) {
                Color.Black
            } else {
                Color.White
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(24.dp)
                    .background(MaterialTheme.colors.primary)
                    .constrainAs(checkIcon) {
                        top.linkTo(card.bottom, (-18).dp)
                        end.linkTo(card.end)
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = checkSymbolColor,
                    modifier = Modifier
                        .size(12.dp)
                )
            }
        }
        if (isEditing && onRemoveListener != null) {
            val openDialog = remember { mutableStateOf(false) }

            SimpleDialogElementUI(
                openDialog = openDialog,
                titleText = removePmDialogTitle,
                messageText = description,
                confirmText = stringResource(R.string.remove),
                dismissText = stringResource(R.string.cancel),
                onConfirmListener = onRemoveListener
            )

            // tint the delete symbol so it contrasts well with the error color around it.
            val iconColor = MaterialTheme.colors.error
            val deleteIconColor = if (iconColor.shouldUseDarkDynamicColor()) {
                Color.Black
            } else {
                Color.White
            }
            Image(
                painter = painterResource(R.drawable.stripe_ic_delete_symbol),
                contentDescription = onRemoveAccessibilityDescription,
                colorFilter = ColorFilter.tint(deleteIconColor),
                modifier = Modifier
                    .constrainAs(deleteIcon) {
                        top.linkTo(card.top, margin = (-9).dp)
                        end.linkTo(card.end)
                    }
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color = iconColor)
                    .clickable(
                        onClick = {
                            openDialog.value = true
                        }
                    )
            )
        }

        LpmSelectorText(
            icon = labelIcon,
            text = labelText,
            textColor = MaterialTheme.colors.onSurface,
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
                        description.replace("\\d".toRegex(), "$0 ")
                }
        )
    }
}
