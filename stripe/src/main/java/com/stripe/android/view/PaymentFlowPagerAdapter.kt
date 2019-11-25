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
    private val shippingMethod: ShippingMethod?,
    private val allowedShippingCountryCodes: Set<String> = emptySet()
) : PagerAdapter() {
    private val pages: MutableList<PaymentFlowPage>

    private var shippingInfoSaved: Boolean = false
    private var validShippingMethods: List<ShippingMethod>? = ArrayList()
    private var defaultShippingMethod: ShippingMethod? = null

    init {
        pages = listOfNotNull(
            PaymentFlowPage.SHIPPING_INFO.takeIf {
                paymentSessionConfig.isShippingInfoRequired
            },
            PaymentFlowPage.SHIPPING_METHOD.takeIf {
                shouldAddShippingScreen()
            }
        ).toMutableList()
    }

    private fun shouldAddShippingScreen(): Boolean {
        return paymentSessionConfig.isShippingMethodRequired &&
            (!paymentSessionConfig.isShippingInfoRequired || shippingInfoSaved) &&
            !pages.contains(PaymentFlowPage.SHIPPING_METHOD)
    }

    fun setShippingInfoSaved(addressSaved: Boolean) {
        shippingInfoSaved = addressSaved
        if (shouldAddShippingScreen()) {
            pages.add(PaymentFlowPage.SHIPPING_METHOD)
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
        pages.remove(PaymentFlowPage.SHIPPING_METHOD)
        notifyDataSetChanged()
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val paymentFlowPagerEnum = pages[position]
        val layout = createItemView(paymentFlowPagerEnum, collection)
        when (paymentFlowPagerEnum) {
            PaymentFlowPage.SHIPPING_METHOD -> {
                customerSession
                    .addProductUsageTokenIfValid(PaymentFlowActivity.TOKEN_SHIPPING_METHOD_SCREEN)
                bindShippingMethod(layout.findViewById(R.id.select_shipping_method_widget))
            }
            PaymentFlowPage.SHIPPING_INFO -> {
                customerSession
                    .addProductUsageTokenIfValid(PaymentFlowActivity.TOKEN_SHIPPING_INFO_SCREEN)
                bindShippingInfo(layout.findViewById(R.id.shipping_info_widget))
            }
        }
        collection.addView(layout)
        return layout
    }

    private fun createItemView(
        paymentFlowPage: PaymentFlowPage,
        collection: ViewGroup
    ): View {
        return LayoutInflater.from(context)
            .inflate(paymentFlowPage.layoutResId, collection, false)
    }

    private fun bindShippingMethod(selectShippingMethodWidget: SelectShippingMethodWidget) {
        selectShippingMethodWidget
            .setShippingMethods(validShippingMethods, defaultShippingMethod)
        shippingMethod?.let {
            selectShippingMethodWidget.setSelectedShippingMethod(it)
        }
    }

    private fun bindShippingInfo(shippingInfoWidget: ShippingInfoWidget) {
        shippingInfoWidget
            .setHiddenFields(paymentSessionConfig.hiddenShippingInfoFields)
        shippingInfoWidget
            .setOptionalFields(paymentSessionConfig.optionalShippingInfoFields)
        shippingInfoWidget
            .populateShippingInfo(shippingInformation)
        shippingInfoWidget
            .setAllowedCountryCodes(allowedShippingCountryCodes)
    }

    override fun destroyItem(collection: ViewGroup, position: Int, view: Any) {
        collection.removeView(view as View)
    }

    override fun getCount(): Int {
        return pages.size
    }

    internal fun getPageAt(position: Int): PaymentFlowPage? {
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
        internal val pages: List<PaymentFlowPage>,
        internal val shippingInfoSaved: Boolean,
        internal val validShippingMethods: List<ShippingMethod>?,
        internal val defaultShippingMethod: ShippingMethod?
    ) : Parcelable
}
