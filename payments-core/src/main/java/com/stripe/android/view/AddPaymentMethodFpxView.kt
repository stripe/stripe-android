package com.stripe.android.view

import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.databinding.StripeBankListPaymentMethodBinding
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.launch

internal class AddPaymentMethodFpxView @JvmOverloads internal constructor(
    activity: FragmentActivity,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AddPaymentMethodView(activity, attrs, defStyleAttr) {

    private var fpxBankStatuses: BankStatuses = BankStatuses()

    private val fpxAdapter = AddPaymentMethodListAdapter(
        ThemeConfig(activity),
        items = FpxBank.entries,
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
            return fpxAdapter.selectedPosition.takeIf { it != RecyclerView.NO_POSITION }?.let {
                val fpxBank = FpxBank.entries[it]
                PaymentMethodCreateParams.create(
                    PaymentMethodCreateParams.Fpx(bank = fpxBank.code)
                )
            }
        }

    init {
        val viewBinding = StripeBankListPaymentMethodBinding.inflate(
            activity.layoutInflater,
            this,
            true
        )

        // an id is required for state to be saved
        id = R.id.stripe_payment_methods_add_fpx

        activity.lifecycleScope.launch {
            viewModel.fpxBankStatues.collect(::onFpxBankStatusesUpdated)
        }

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
        FpxBank.entries.indices
            .filterNot { position ->
                fpxBankStatuses.isOnline(getItem(position))
            }
            .forEach { position ->
                fpxAdapter.notifyAdapterItemChanged(position)
            }
    }

    private fun getItem(position: Int): FpxBank {
        return FpxBank.entries[position]
    }

    internal companion object {
        @JvmSynthetic
        internal fun create(activity: FragmentActivity): AddPaymentMethodFpxView {
            return AddPaymentMethodFpxView(activity)
        }
    }
}
