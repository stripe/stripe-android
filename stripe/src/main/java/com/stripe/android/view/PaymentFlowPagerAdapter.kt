package com.stripe.android.view

import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.R
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import java.util.ArrayList
import kotlinx.android.parcel.Parcelize

internal class PaymentFlowPagerAdapter(
    private val context: Context,
    private val paymentSessionConfig: PaymentSessionConfig,
    private val customerSession: CustomerSession,
    private val shippingInformation: ShippingInformation?,
    private val shippingMethod: ShippingMethod?
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
        val layout = LayoutInflater.from(context)
            .inflate(paymentFlowPagerEnum.layoutResId, collection, false) as ViewGroup

        when (paymentFlowPagerEnum) {
            PaymentFlowPagerEnum.SHIPPING_METHOD -> {
                customerSession
                    .addProductUsageTokenIfValid(PaymentFlowActivity.TOKEN_SHIPPING_METHOD_SCREEN)
                val selectShippingMethodWidget: SelectShippingMethodWidget =
                    layout.findViewById(R.id.select_shipping_method_widget)
                selectShippingMethodWidget
                    .setShippingMethods(validShippingMethods, defaultShippingMethod)
                shippingMethod?.let {
                    selectShippingMethodWidget.setSelectedShippingMethod(it)
                }
            }
            PaymentFlowPagerEnum.SHIPPING_INFO -> {
                customerSession
                    .addProductUsageTokenIfValid(PaymentFlowActivity.TOKEN_SHIPPING_INFO_SCREEN)
                val shippingInfoWidget: ShippingInfoWidget =
                    layout.findViewById(R.id.shipping_info_widget)
                shippingInfoWidget
                    .setHiddenFields(paymentSessionConfig.hiddenShippingInfoFields)
                shippingInfoWidget
                    .setOptionalFields(paymentSessionConfig.optionalShippingInfoFields)
                shippingInfoWidget
                    .populateShippingInfo(shippingInformation)
            }
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

    internal fun getPageAt(position: Int): PaymentFlowPagerEnum? {
        return pages.getOrNull(position)
    }

    override fun isViewFromObject(view: View, o: Any): Boolean {
        return view === o
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(pages[position].titleResId)
    }

    override fun saveState(): Parcelable? {
        return State(pages, shippingInfoSaved, validShippingMethods, defaultShippingMethod)
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        if (state is State) {
            this.pages.clear()
            this.pages.addAll(state.pages)
            this.shippingInfoSaved = state.shippingInfoSaved
            this.validShippingMethods = state.validShippingMethods
            this.defaultShippingMethod = state.defaultShippingMethod

            notifyDataSetChanged()
        }
    }

    @Parcelize
    internal class State(
        internal val pages: List<PaymentFlowPagerEnum>,
        internal val shippingInfoSaved: Boolean,
        internal val validShippingMethods: List<ShippingMethod>?,
        internal val defaultShippingMethod: ShippingMethod?
    ) : Parcelable
}
