package com.stripe.android.view

import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.databinding.BankListPaymentMethodBinding
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.PaymentMethodCreateParams

internal class AddPaymentMethodFpxView @JvmOverloads internal constructor(
    activity: FragmentActivity,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AddPaymentMethodView(activity, attrs, defStyleAttr) {

    private var fpxBankStatuses: BankStatuses = BankStatuses()

    private val fpxAdapter = AddPaymentMethodListAdapter(
        ThemeConfig(activity),
        items = FpxBank.values().asList() as List<Bank>,
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
            val fpxBank = FpxBank.values()[fpxAdapter.selectedPosition]

            return PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Fpx(bank = fpxBank.code)
            )
        }

    init {
        val viewBinding = BankListPaymentMethodBinding.inflate(
            activity.layoutInflater,
            this,
            true
        )

        // an id is required for state to be saved
        id = R.id.stripe_payment_methods_add_fpx

        viewModel.getFpxBankStatues()
            .observe(activity, Observer(::onFpxBankStatusesUpdated))

        with(viewBinding.bankList) {
            adapter = fpxAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = DefaultItemAnimator()
        }

        viewModel.selectedPosition?.let {
            fpxAdapter.updateSelected(it)
        }
    }

    private fun onFpxBankStatusesUpdated(fpxBankStatuses: BankStatuses?) {
        fpxBankStatuses?.let {
            updateStatuses(it)
        }
    }

    private fun updateStatuses(fpxBankStatuses: BankStatuses) {
        this.fpxBankStatuses = fpxBankStatuses
        this.fpxAdapter.bankStatuses = fpxBankStatuses

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

    internal companion object {
        @JvmSynthetic
        internal fun create(activity: FragmentActivity): AddPaymentMethodFpxView {
            return AddPaymentMethodFpxView(activity)
        }
    }
}
