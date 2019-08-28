package com.stripe.android.view

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.stripe.android.model.ShippingMethod
import java.util.ArrayList

/**
 * Adapter that populates a list with shipping methods
 */
internal class ShippingMethodAdapter : RecyclerView.Adapter<ShippingMethodAdapter.ViewHolder>() {

    private var shippingMethods: List<ShippingMethod> = ArrayList()
    private var selectedIndex = 0

    val selectedShippingMethod: ShippingMethod?
        get() = shippingMethods[selectedIndex]

    override fun getItemCount(): Int {
        return shippingMethods.size
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        return ViewHolder(ShippingMethodView(viewGroup.context), this)
    }

    override fun onBindViewHolder(holder: ViewHolder, i: Int) {
        holder.setShippingMethod(shippingMethods[i])
        holder.setSelected(i == selectedIndex)
    }

    fun setShippingMethods(
        shippingMethods: List<ShippingMethod>?,
        defaultShippingMethod: ShippingMethod?
    ) {
        if (shippingMethods != null) {
            this.shippingMethods = shippingMethods
        }
        selectedIndex = if (defaultShippingMethod == null) {
            0
        } else {
            this.shippingMethods.indexOf(defaultShippingMethod)
        }
        notifyDataSetChanged()
    }

    fun onShippingMethodSelected(selectedIndex: Int) {
        this.selectedIndex = selectedIndex
        notifyDataSetChanged()
    }

    internal class ViewHolder constructor(
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
