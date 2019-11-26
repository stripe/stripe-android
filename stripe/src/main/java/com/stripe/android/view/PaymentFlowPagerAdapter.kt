package com.stripe.android.view

import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.R
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import kotlinx.android.parcel.Parcelize

internal class PaymentFlowPagerAdapter(
    private val context: Context,
    private val paymentSessionConfig: PaymentSessionConfig,
    private val customerSession: CustomerSession,
    private val shippingInformation: ShippingInformation?,
    private val shippingMethod: ShippingMethod?,
    private val allowedShippingCountryCodes: Set<String> = emptySet()
) : PagerAdapter() {
    private val pages: MutableList<PaymentFlowPage> = mutableListOf()

    private var shippingInfoSaved: Boolean = false
    private var shippingMethods: List<ShippingMethod> = emptyList()
    private var defaultShippingMethod: ShippingMethod? = null

    init {
        pages.addAll(listOfNotNull(
            PaymentFlowPage.SHIPPING_INFO.takeIf {
                paymentSessionConfig.isShippingInfoRequired
            },
            PaymentFlowPage.SHIPPING_METHOD.takeIf {
                shouldAddShippingScreen()
            }
        ))
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
        shippingMethods: List<ShippingMethod>,
        defaultShippingMethod: ShippingMethod?
    ) {
        this.shippingMethods = shippingMethods
        this.defaultShippingMethod = defaultShippingMethod
    }

    fun hideShippingPage() {
        pages.remove(PaymentFlowPage.SHIPPING_METHOD)
        notifyDataSetChanged()
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        val paymentFlowPage = pages[position]
        val layout = createItemView(paymentFlowPage, collection)
        val viewHolder = when (paymentFlowPage) {
            PaymentFlowPage.SHIPPING_INFO -> {
                PaymentFlowViewHolder.ShippingInformationViewHolder(layout)
            }
            PaymentFlowPage.SHIPPING_METHOD -> {
                PaymentFlowViewHolder.ShippingMethodViewHolder(layout)
            }
        }
        when (viewHolder) {
            is PaymentFlowViewHolder.ShippingInformationViewHolder -> {
                customerSession
                    .addProductUsageTokenIfValid(PaymentFlowActivity.TOKEN_SHIPPING_INFO_SCREEN)
                viewHolder.bind(
                    paymentSessionConfig, shippingInformation, allowedShippingCountryCodes
                )
            }
            is PaymentFlowViewHolder.ShippingMethodViewHolder -> {
                customerSession
                    .addProductUsageTokenIfValid(PaymentFlowActivity.TOKEN_SHIPPING_METHOD_SCREEN)
                viewHolder.bind(shippingMethods, defaultShippingMethod, shippingMethod)
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
        return State(pages, shippingInfoSaved, shippingMethods, defaultShippingMethod)
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        if (state is State) {
            this.pages.clear()
            this.pages.addAll(state.pages)
            this.shippingInfoSaved = state.shippingInfoSaved
            this.shippingMethods = state.shippingMethods
            this.defaultShippingMethod = state.defaultShippingMethod

            notifyDataSetChanged()
        }
    }

    @Parcelize
    internal class State(
        internal val pages: List<PaymentFlowPage>,
        internal val shippingInfoSaved: Boolean,
        internal val shippingMethods: List<ShippingMethod>,
        internal val defaultShippingMethod: ShippingMethod?
    ) : Parcelable

    internal sealed class PaymentFlowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        class ShippingInformationViewHolder(itemView: View) : PaymentFlowViewHolder(itemView) {
            private val shippingInfoWidget: ShippingInfoWidget =
                itemView.findViewById(R.id.shipping_info_widget)

            fun bind(
                paymentSessionConfig: PaymentSessionConfig,
                shippingInformation: ShippingInformation?,
                allowedShippingCountryCodes: Set<String>
            ) {
                shippingInfoWidget
                    .setHiddenFields(paymentSessionConfig.hiddenShippingInfoFields)
                shippingInfoWidget
                    .setOptionalFields(paymentSessionConfig.optionalShippingInfoFields)
                shippingInfoWidget
                    .populateShippingInfo(shippingInformation)
                shippingInfoWidget
                    .setAllowedCountryCodes(allowedShippingCountryCodes)
            }
        }

        class ShippingMethodViewHolder(itemView: View) : PaymentFlowViewHolder(itemView) {
            private val shippingMethodWidget: SelectShippingMethodWidget =
                itemView.findViewById(R.id.select_shipping_method_widget)

            fun bind(
                shippingMethods: List<ShippingMethod>,
                defaultShippingMethod: ShippingMethod?,
                shippingMethod: ShippingMethod?
            ) {
                shippingMethodWidget
                    .setShippingMethods(shippingMethods, defaultShippingMethod)
                shippingMethod?.let {
                    shippingMethodWidget.setSelectedShippingMethod(it)
                }
            }
        }
    }
}
