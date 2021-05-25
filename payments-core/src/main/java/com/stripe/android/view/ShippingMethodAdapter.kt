package com.stripe.android.view

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.ShippingMethod

/**
 * Adapter that populates a list with shipping methods
 */
internal class ShippingMethodAdapter :
    RecyclerView.Adapter<ShippingMethodAdapter.ShippingMethodViewHolder>() {
    internal var onShippingMethodSelectedCallback: (ShippingMethod) -> Unit = {}

    internal var shippingMethods: List<ShippingMethod> = emptyList()
        set(value) {
            // reset selected
            selectedIndex = 0
            field = value
            notifyDataSetChanged()
        }

    @JvmSynthetic
    internal var selectedIndex = 0
        set(value) {
            if (field != value) {
                // only notify change if the field's value is changing
                notifyItemChanged(field)
                notifyItemChanged(value)
                field = value

                onShippingMethodSelectedCallback(shippingMethods[value])
            }
        }

    init {
        setHasStableIds(true)
    }

    val selectedShippingMethod: ShippingMethod?
        get() = shippingMethods.getOrNull(selectedIndex)

    override fun getItemCount(): Int {
        return shippingMethods.size
    }

    override fun getItemId(position: Int): Long {
        return shippingMethods[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ShippingMethodViewHolder {
        return ShippingMethodViewHolder(ShippingMethodView(viewGroup.context))
    }

    override fun onBindViewHolder(holder: ShippingMethodViewHolder, i: Int) {
        holder.setShippingMethod(shippingMethods[i])
        holder.setSelected(i == selectedIndex)
        holder.shippingMethodView.setOnClickListener {
            selectedIndex = holder.bindingAdapterPosition
        }
    }

    internal fun setSelected(shippingMethod: ShippingMethod) {
        selectedIndex = shippingMethods.indexOf(shippingMethod)
    }

    internal class ShippingMethodViewHolder constructor(
        internal val shippingMethodView: ShippingMethodView
    ) : RecyclerView.ViewHolder(shippingMethodView) {

        fun setShippingMethod(shippingMethod: ShippingMethod) {
            shippingMethodView.setShippingMethod(shippingMethod)
        }

        fun setSelected(selected: Boolean) {
            shippingMethodView.isSelected = selected
        }
    }
}
