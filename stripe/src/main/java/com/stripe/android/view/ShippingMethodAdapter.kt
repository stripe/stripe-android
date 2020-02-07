package com.stripe.android.view

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.ShippingMethod

/**
 * Adapter that populates a list with shipping methods
 */
internal class ShippingMethodAdapter :
    RecyclerView.Adapter<ShippingMethodAdapter.ShippingMethodViewHolder>() {

    private var shippingMethods: List<ShippingMethod> = emptyList()
    private var selectedIndex = 0

    init {
        setHasStableIds(true)
    }

    val selectedShippingMethod: ShippingMethod?
        get() = shippingMethods[selectedIndex]

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
            onShippingMethodSelected(holder.adapterPosition)
        }
    }

    fun setShippingMethods(
        shippingMethods: List<ShippingMethod>,
        defaultShippingMethod: ShippingMethod? = null
    ) {
        this.shippingMethods = shippingMethods
        selectedIndex = defaultShippingMethod?.let { shippingMethods.indexOf(it) } ?: 0
        notifyDataSetChanged()
    }

    private fun onShippingMethodSelected(selectedIndex: Int) {
        val previousSelectedIndex = this.selectedIndex
        this.selectedIndex = selectedIndex

        notifyItemChanged(previousSelectedIndex)
        notifyItemChanged(selectedIndex)
    }

    internal fun setSelected(shippingMethod: ShippingMethod) {
        val previouslySelectedIndex = selectedIndex
        selectedIndex = shippingMethods.indexOf(shippingMethod)
        if (previouslySelectedIndex != selectedIndex) {
            notifyItemChanged(previouslySelectedIndex)
            notifyItemChanged(selectedIndex)
        }
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
