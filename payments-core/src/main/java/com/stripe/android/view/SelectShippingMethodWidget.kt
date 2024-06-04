package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.databinding.StripeShippingMethodWidgetBinding
import com.stripe.android.model.ShippingMethod

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
     * The [ShippingMethod] selected by the customer or `null` if no option is selected.
     */
    val selectedShippingMethod: ShippingMethod?
        get() = shippingMethodAdapter.selectedShippingMethod

    init {
        val viewBinding = StripeShippingMethodWidgetBinding.inflate(
            LayoutInflater.from(context),
            this
        )
        viewBinding.shippingMethods.apply {
            setHasFixedSize(true)
            adapter = shippingMethodAdapter
            layoutManager = LinearLayoutManager(context)
        }
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
