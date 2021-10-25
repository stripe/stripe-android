package com.stripe.android.paymentsheet

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.paymentsheet.databinding.LayoutPaymentsheetAddPaymentMethodCardViewBinding
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlin.math.roundToInt
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
        return AddPaymentMethodViewHolder(parent)
            .apply {
                val targetWidth = parent.measuredWidth - parent.paddingStart - parent.paddingEnd
                val minItemWidth = 100 * parent.context.resources.displayMetrics.density +
                    itemView.marginEnd + itemView.marginStart
                // numItems is incremented in steps of 0.5 items (1, 1.5, 2, 2.5, 3, ...)
                val numItems = (targetWidth * 2 / minItemWidth).toInt() / 2f
                val viewWidth = targetWidth / numItems - itemView.marginEnd - itemView.marginStart
                itemView.layoutParams.width = viewWidth.toInt()
                itemView.setOnClickListener {
                    onItemSelected(bindingAdapterPosition)
                }
            }
    }

    override fun onBindViewHolder(holder: AddPaymentMethodViewHolder, position: Int) {
        holder.bind(paymentMethods[position])
        holder.setSelected(position == selectedItemPosition)
        holder.setEnabled(isEnabled)
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
        private val binding: LayoutPaymentsheetAddPaymentMethodCardViewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        constructor(parent: ViewGroup) : this(
            LayoutPaymentsheetAddPaymentMethodCardViewBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        fun setSelected(selected: Boolean) {
            binding.root.isSelected = selected
            binding.root.strokeWidth = if (selected) {
                itemView.resources
                    .getDimension(R.dimen.stripe_paymentsheet_add_pm_card_stroke_width_selected)
                    .roundToInt()
            } else {
                itemView.resources
                    .getDimension(R.dimen.stripe_paymentsheet_add_pm_card_stroke_width)
                    .roundToInt()
            }
            binding.root.elevation = if (selected) {
                itemView.resources
                    .getDimension(R.dimen.stripe_paymentsheet_add_pm_card_elevation_selected)
            } else {
                itemView.resources
                    .getDimension(R.dimen.stripe_paymentsheet_add_pm_card_elevation)
            }
        }

        fun setEnabled(enabled: Boolean) {
            binding.root.isEnabled = enabled
            binding.title.isEnabled = enabled
        }

        fun bind(paymentMethod: SupportedPaymentMethod) {
            binding.icon.setImageResource(paymentMethod.iconResource)
            binding.title.text = itemView.resources.getString(paymentMethod.displayNameResource)
        }
    }
}
