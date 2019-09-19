package com.stripe.android.view

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.ShippingMethod
import java.util.ArrayList

/**
 * Adapter that populates a list with shipping methods
 */
internal class ShippingMethodAdapter :
    RecyclerView.Adapter<ShippingMethodAdapter.ShippingMethodViewHolder>() {

    private var shippingMethods: List<ShippingMethod> = ArrayList()
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
        return ShippingMethodViewHolder(ShippingMethodView(viewGroup.context), this)
    }

    override fun onBindViewHolder(holder: ShippingMethodViewHolder, i: Int) {
        holder.setShippingMethod(shippingMethods[i])
        holder.setSelected(i == selectedIndex)
    }

    fun setShippingMethods(
        shippingMethods: List<ShippingMethod>?,
        defaultShippingMethod: ShippingMethod?
    ) {
        this.shippingMethods = shippingMethods ?: emptyList()
        selectedIndex = if (defaultShippingMethod == null) {
            0
        } else {
            this.shippingMethods.indexOf(defaultShippingMethod)
        }
        notifyDataSetChanged()
    }

    fun onShippingMethodSelected(selectedIndex: Int) {
        val previousSelectedIndex = this.selectedIndex
        this.selectedIndex = selectedIndex

        notifyItemChanged(previousSelectedIndex)
        notifyItemChanged(selectedIndex)
    }

    internal class ShippingMethodViewHolder constructor(
        private val shippingMethodView: ShippingMethodView,
        adapter: ShippingMethodAdapter
    ) : RecyclerView.ViewHolder(shippingMethodView) {

        init {
            shippingMethodView.setOnClickListener {
                adapter.onShippingMethodSelected(adapterPosition)
            }
        }

        fun setShippingMethod(shippingMethod: ShippingMethod) {
            shippingMethodView.setShippingMethod(shippingMethod)
        }

        fun setSelected(selected: Boolean) {
            shippingMethodView.isSelected = selected
        }
    }
}
