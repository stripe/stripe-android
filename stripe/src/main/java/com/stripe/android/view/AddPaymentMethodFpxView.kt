package com.stripe.android.view

import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.databinding.FpxBankItemBinding
import com.stripe.android.databinding.FpxPaymentMethodBinding
import com.stripe.android.model.FpxBankStatuses
import com.stripe.android.model.PaymentMethodCreateParams

internal class AddPaymentMethodFpxView @JvmOverloads internal constructor(
    activity: FragmentActivity,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AddPaymentMethodView(activity, attrs, defStyleAttr) {

    private var fpxBankStatuses: FpxBankStatuses = FpxBankStatuses()

    private val fpxAdapter = AddPaymentMethodListAdapter(
        activity,
        items = FpxBank.values(),
        paymentMethodListType = PaymentMethodListType.PaymentMethodListTypeFpx,
        itemSelectedCallback = {
            viewModel.selectedPosition = it
        }
    )

    private val viewModel: FpxViewModel by lazy {
        ViewModelProvider(
            activity,
            FpxViewModel.Factory(activity.application)
        ).get(FpxViewModel::class.java)
    }

    override val createParams: PaymentMethodCreateParams?
        get() {
            val fpxBank = FpxBank.values()[fpxAdapter.selectedPosition] ?: return null

            return PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Fpx(bank = fpxBank.code)
            )
        }

    init {
        val viewBinding = FpxPaymentMethodBinding.inflate(
            activity.layoutInflater,
            this,
            true
        )

        // an id is required for state to be saved
        id = R.id.stripe_payment_methods_add_fpx

        viewBinding.fpxList.run {
            adapter = fpxAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = DefaultItemAnimator()
        }

        viewModel.getFpxBankStatues()
            .observe(activity, Observer(::onFpxBankStatusesUpdated))

        viewModel.selectedPosition?.let {
            fpxAdapter.updateSelected(it)
        }
    }

    private fun onFpxBankStatusesUpdated(fpxBankStatuses: FpxBankStatuses?) {
        fpxBankStatuses?.let {
            updateStatuses(it)
        }
    }

    internal fun updateStatuses(fpxBankStatuses: FpxBankStatuses) {
        this.fpxBankStatuses = fpxBankStatuses

        // flag offline bank
        FpxBank.values().indices
            .filterNot { position ->
                fpxBankStatuses.isOnline(getItem(position))
            }
            .forEach { position ->
                fpxAdapter.notifyAdapterItemChanged(position)
            }
    }

    private fun getItem(position: Int): FpxBank {
        return FpxBank.values()[position]
    }

    class BankViewHolder constructor(
        private val viewBinding: FpxBankItemBinding,
        private val themeConfig: ThemeConfig
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        private val resources: Resources = itemView.resources

        fun update(fpxBank: FpxBank, isOnline: Boolean) {
            viewBinding.name.text = if (isOnline) {
                fpxBank.displayName
            } else {
                resources.getString(
                    R.string.fpx_bank_offline,
                    fpxBank.displayName
                )
            }
            viewBinding.icon.setImageResource(fpxBank.brandIconResId)
        }

        internal fun setSelected(isSelected: Boolean) {
            viewBinding.name.setTextColor(themeConfig.getTextColor(isSelected))
            ImageViewCompat.setImageTintList(
                viewBinding.checkIcon,
                ColorStateList.valueOf(themeConfig.getTintColor(isSelected))
            )
            viewBinding.checkIcon.visibility = if (isSelected) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    internal companion object {
        @JvmSynthetic
        internal fun create(activity: FragmentActivity): AddPaymentMethodFpxView {
            return AddPaymentMethodFpxView(activity)
        }
    }
}
