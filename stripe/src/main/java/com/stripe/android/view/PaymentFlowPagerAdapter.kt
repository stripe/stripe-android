package com.stripe.android.view

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.R
import com.stripe.android.model.ShippingMethod
import java.util.ArrayList

internal class PaymentFlowPagerAdapter(
    private val context: Context,
    private val paymentSessionConfig: PaymentSessionConfig,
    private val customerSession: CustomerSession
) : PagerAdapter() {
    private val pages: MutableList<PaymentFlowPagerEnum>

    private var shippingInfoSaved: Boolean = false
    private var validShippingMethods: List<ShippingMethod>? = ArrayList()
    private var defaultShippingMethod: ShippingMethod? = null

    init {
        pages = ArrayList()
        if (paymentSessionConfig.isShippingInfoRequired) {
            pages.add(PaymentFlowPagerEnum.SHIPPING_INFO)
        }
        if (shouldAddShippingScreen()) {
            pages.add(PaymentFlowPagerEnum.SHIPPING_METHOD)
        }
    }

    private fun shouldAddShippingScreen(): Boolean {
        return paymentSessionConfig.isShippingMethodRequired &&
            (!paymentSessionConfig.isShippingInfoRequired || shippingInfoSaved) &&
            !pages.contains(PaymentFlowPagerEnum.SHIPPING_METHOD)
    }

    fun setShippingInfoSaved(addressSaved: Boolean) {
        shippingInfoSaved = addressSaved
        if (shouldAddShippingScreen()) {
            pages.add(PaymentFlowPagerEnum.SHIPPING_METHOD)
        }
        notifyDataSetChanged()
    }

    fun setShippingMethods(
        validShippingMethods: List<ShippingMethod>?,
        defaultShippingMethod: ShippingMethod?
    ) {
        this.validShippingMethods = validShippingMethods
        this.defaultShippingMethod = defaultShippingMethod
    }

    fun hideShippingPage() {
        pages.remove(PaymentFlowPagerEnum.SHIPPING_METHOD)
        notifyDataSetChanged()
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val paymentFlowPagerEnum = pages[position]
        val inflater = LayoutInflater.from(context)
        val layout = inflater
            .inflate(paymentFlowPagerEnum.layoutResId, collection, false) as ViewGroup
        if (paymentFlowPagerEnum == PaymentFlowPagerEnum.SHIPPING_METHOD) {
            customerSession
                .addProductUsageTokenIfValid(PaymentFlowActivity.TOKEN_SHIPPING_METHOD_SCREEN)
            val selectShippingMethodWidget =
                layout.findViewById<SelectShippingMethodWidget>(R.id.select_shipping_method_widget)
            selectShippingMethodWidget
                .setShippingMethods(validShippingMethods, defaultShippingMethod)
        }
        if (paymentFlowPagerEnum == PaymentFlowPagerEnum.SHIPPING_INFO) {
            customerSession
                .addProductUsageTokenIfValid(PaymentFlowActivity.TOKEN_SHIPPING_INFO_SCREEN)
            val shippingInfoWidget =
                layout.findViewById<ShippingInfoWidget>(R.id.shipping_info_widget)
            shippingInfoWidget.setHiddenFields(paymentSessionConfig.hiddenShippingInfoFields)
            shippingInfoWidget.setOptionalFields(paymentSessionConfig.optionalShippingInfoFields)
            shippingInfoWidget.populateShippingInfo(paymentSessionConfig.prepopulatedShippingInfo)
        }
        collection.addView(layout)
        return layout
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    override fun getCount(): Int {
        return pages.size
    }

    fun getPageAt(position: Int): PaymentFlowPagerEnum? {
        return if (position < pages.size) {
            pages[position]
        } else null
    }

    override fun isViewFromObject(view: View, o: Any): Boolean {
        return view === o
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(pages[position].titleResId)
    }
}
