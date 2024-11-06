package com.stripe.android.view

import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.databinding.StripeBankListPaymentMethodBinding
import com.stripe.android.model.PaymentMethodCreateParams

internal class AddPaymentMethodNetbankingView @JvmOverloads internal constructor(
    activity: FragmentActivity,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AddPaymentMethodView(activity, attrs, defStyleAttr) {
    private var selectedPosition: Int? = null

    private val netbankingAdapter = AddPaymentMethodListAdapter(
        ThemeConfig(activity),
        items = NetbankingBank.entries,
        itemSelectedCallback = {
            this.selectedPosition = it
        }
    )

    override val createParams: PaymentMethodCreateParams?
        get() {
            return netbankingAdapter.selectedPosition.takeIf { it != RecyclerView.NO_POSITION }
                ?.let {
                    val netbankingBank = NetbankingBank.entries[netbankingAdapter.selectedPosition]

                    return PaymentMethodCreateParams.create(
                        PaymentMethodCreateParams.Netbanking(bank = netbankingBank.code)
                    )
                }
        }

    init {
        val viewBinding = StripeBankListPaymentMethodBinding.inflate(
            activity.layoutInflater,
            this,
            true
        )

        id = R.id.stripe_payment_methods_add_netbanking

        with(viewBinding.bankList) {
            adapter = netbankingAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = DefaultItemAnimator()
        }

        selectedPosition?.let {
            netbankingAdapter.updateSelected(it)
        }
    }

    internal companion object {
        @JvmSynthetic
        internal fun create(activity: FragmentActivity): AddPaymentMethodNetbankingView {
            return AddPaymentMethodNetbankingView(activity)
        }
    }
}
