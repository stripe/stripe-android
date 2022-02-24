package com.stripe.android.paymentsheet

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlin.properties.Delegates

@SuppressLint("NotifyDataSetChanged")
internal class AddPaymentMethodsAdapter(
    private val paymentMethods: List<SupportedPaymentMethod>,
    private var selectedItemPosition: Int,
    val paymentMethodSelectedListener: (paymentMethod: SupportedPaymentMethod) -> Unit,
) : RecyclerView.Adapter<AddPaymentMethodsAdapter.AddPaymentMethodViewHolder>() {

    internal var isEnabled: Boolean by Delegates.observable(true) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = paymentMethods.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddPaymentMethodViewHolder {
        return AddPaymentMethodViewHolder(parent, paymentMethods.size)
    }

    override fun onBindViewHolder(holder: AddPaymentMethodViewHolder, position: Int) {
        holder.bind(
            paymentMethod = paymentMethods[position],
            isSelected = position == selectedItemPosition,
            isEnabled = isEnabled,
            onItemSelectedListener = { onItemSelected(position) }
        )
    }

    override fun onViewRecycled(holder: AddPaymentMethodViewHolder) {
        holder.onViewRecycled()
        super.onViewRecycled(holder)
    }

    private fun onItemSelected(position: Int) {
        if (position != RecyclerView.NO_POSITION &&
            position != selectedItemPosition
        ) {
            val previousSelectedIndex = selectedItemPosition
            selectedItemPosition = position

            notifyItemChanged(previousSelectedIndex)
            notifyItemChanged(position)

            paymentMethodSelectedListener(paymentMethods[position])
        }
    }

    internal class AddPaymentMethodViewHolder(
        private val composeView: ComposeView,
        private val viewWidth: Dp
    ) : RecyclerView.ViewHolder(composeView) {

        constructor(parent: ViewGroup, numberOfPaymentMethods: Int) : this(
            composeView = ComposeView(parent.context).apply {
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            },
            viewWidth = calculateViewWidth(parent, numberOfPaymentMethods)
        )

        fun bind(
            paymentMethod: SupportedPaymentMethod,
            isSelected: Boolean,
            isEnabled: Boolean,
            onItemSelectedListener: () -> Unit
        ) {
            composeView.setContent {
                PaymentMethodUI(
                    viewWidth = viewWidth,
                    iconRes = paymentMethod.iconResource,
                    title = itemView.resources.getString(paymentMethod.displayNameResource),
                    isSelected = isSelected,
                    isEnabled = isEnabled,
                    onItemSelectedListener = onItemSelectedListener
                )
            }
        }

        fun onViewRecycled() {
            // Dispose the underlying Composition of the ComposeView
            // when RecyclerView has recycled this ViewHolder
            composeView.disposeComposition()
        }
    }

    internal companion object {
        private fun calculateViewWidth(parent: ViewGroup, numberOfPaymentMethods: Int): Dp {
            val targetWidth = parent.measuredWidth - parent.paddingStart - parent.paddingEnd
            val screenDensity = parent.context.resources.displayMetrics.density
            val minItemWidth = 100 * screenDensity + 6.0f + 6.0f

            // if all items fit at min width, then span them across the sheet evenly filling it.
            // otherwise the number of items visible should be a multiple of .5
            val viewWidth =
                if (minItemWidth * numberOfPaymentMethods < targetWidth) {
                    targetWidth / numberOfPaymentMethods
                } else {
                    // numVisibleItems is incremented in steps of 0.5 items
                    // (1, 1.5, 2, 2.5, 3, ...)
                    val numVisibleItems = (targetWidth * 2 / minItemWidth).toInt() / 2f
                    targetWidth / numVisibleItems
                }

            return (viewWidth.toInt() / screenDensity).dp
        }
    }
}

@Composable
fun PaymentMethodUI(
    viewWidth: Dp,
    iconRes: Int,
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onItemSelectedListener: () -> Unit
) {
    val strokeColor = colorResource(
        if (isSelected) {
            R.color.stripe_paymentsheet_add_pm_card_selected_stroke
        } else {
            R.color.stripe_paymentsheet_add_pm_card_unselected_stroke
        }
    )

    val cardBackgroundColor = if (isEnabled) {
        MaterialTheme.colors.surface
    } else {
        colorResource(R.color.stripe_paymentsheet_elements_background_disabled)
    }

    Card(
        border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, strokeColor),
        shape = RoundedCornerShape(6.dp),
        elevation = if (isSelected) 1.5.dp else 0.dp,
        backgroundColor = cardBackgroundColor,
        modifier = Modifier
            .height(60.dp)
            .width(viewWidth)
            .padding(horizontal = 6.dp)
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                onClick = {
                    onItemSelectedListener()
                }
            )
    ) {
        Column {
            Image(
                painter = painterResource(iconRes),
                contentDescription = "",
                modifier = Modifier
                    .padding(top = 12.dp, start = 12.dp)
            )
            val color = colorResource(R.color.stripe_paymentsheet_title_text)
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) color else color.copy(alpha = 0.6f),
                lineHeight = 1.sp,
                modifier = Modifier
                    .padding(top = 6.dp, start = 12.dp)
            )
        }
    }
}
