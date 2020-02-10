package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.model.ShippingMethod
import kotlinx.android.synthetic.main.select_shipping_method_widget.view.*

/**
 * A widget that allows the user to select a shipping method.
 */
internal class SelectShippingMethodWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val shippingMethodAdapter = ShippingMethodAdapter()

    /**
     * @return The [ShippingMethod] selected by the customer or `null` if no option is
     * selected.
     */
    val selectedShippingMethod: ShippingMethod?
        get() = shippingMethodAdapter.selectedShippingMethod

    init {
        View.inflate(context, R.layout.select_shipping_method_widget, this)
        shipping_methods.setHasFixedSize(true)
        shipping_methods.adapter = shippingMethodAdapter
        shipping_methods.layoutManager = LinearLayoutManager(context)
    }

    fun setShippingMethodSelectedCallback(callback: (ShippingMethod) -> Unit) {
        shippingMethodAdapter.onShippingMethodSelectedCallback = callback
    }

    /**
     * Specify the shipping methods to show.
     */
    fun setShippingMethods(shippingMethods: List<ShippingMethod>) {
        shippingMethodAdapter.shippingMethods = shippingMethods
    }

    fun setSelectedShippingMethod(shippingMethod: ShippingMethod) {
        shippingMethodAdapter.setSelected(shippingMethod)
    }
}
