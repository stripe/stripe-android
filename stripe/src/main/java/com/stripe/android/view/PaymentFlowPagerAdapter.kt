package com.stripe.android.view

import android.content.Context
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

internal class PaymentFlowPagerAdapter(
    private val context: Context,
    private val paymentSessionConfig: PaymentSessionConfig,
    private val customerSession: CustomerSession,
    private val allowedShippingCountryCodes: Set<String> = emptySet(),
    private val onShippingMethodSelectedCallback: (ShippingMethod) -> Unit = {}
) : PagerAdapter() {
    private val pages: List<PaymentFlowPage>
        get() {
            return listOfNotNull(
                PaymentFlowPage.SHIPPING_INFO.takeIf {
                    paymentSessionConfig.isShippingInfoRequired
                },
                PaymentFlowPage.SHIPPING_METHOD.takeIf {
                    paymentSessionConfig.isShippingMethodRequired &&
                        (!paymentSessionConfig.isShippingInfoRequired || isShippingInfoSubmitted)
                }
            )
        }

    internal var shippingInformation: ShippingInformation? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    internal var isShippingInfoSubmitted: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    internal var shippingMethods: List<ShippingMethod> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    internal var selectedShippingMethod: ShippingMethod? = null
        set(value) {
            field = value
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
                viewHolder.bind(shippingMethods, selectedShippingMethod, onShippingMethodSelectedCallback)
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
                    .setAllowedCountryCodes(allowedShippingCountryCodes)
                shippingInfoWidget
                    .populateShippingInfo(shippingInformation)
            }
        }

        class ShippingMethodViewHolder(itemView: View) : PaymentFlowViewHolder(itemView) {
            private val shippingMethodWidget: SelectShippingMethodWidget =
                itemView.findViewById(R.id.select_shipping_method_widget)

            fun bind(
                shippingMethods: List<ShippingMethod>,
                selectedShippingMethod: ShippingMethod?,
                onShippingMethodSelectedCallback: (ShippingMethod) -> Unit
            ) {
                shippingMethodWidget.setShippingMethods(shippingMethods)
                shippingMethodWidget.setShippingMethodSelectedCallback(
                    onShippingMethodSelectedCallback
                )
                selectedShippingMethod?.let {
                    shippingMethodWidget.setSelectedShippingMethod(it)
                }
            }
        }
    }
}
