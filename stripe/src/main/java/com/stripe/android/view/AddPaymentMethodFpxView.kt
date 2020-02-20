package com.stripe.android.view

import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.model.FpxBankStatuses
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.android.synthetic.main.add_payment_method_fpx_layout.view.*

internal class AddPaymentMethodFpxView @JvmOverloads internal constructor(
    activity: FragmentActivity,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AddPaymentMethodView(activity, attrs, defStyleAttr) {
    private val fpxAdapter = Adapter(ThemeConfig(activity)) {
        viewModel.selectedPosition = it
    }

    private val viewModel: FpxViewModel by lazy {
        ViewModelProvider(
            activity,
            FpxViewModel.Factory(activity.application)
        ).get(FpxViewModel::class.java)
    }

    override val createParams: PaymentMethodCreateParams?
        get() {
            val fpxBank = fpxAdapter.selectedBank ?: return null

            return PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Fpx(bank = fpxBank.code)
            )
        }

    init {
        View.inflate(context, R.layout.add_payment_method_fpx_layout, this)

        // an id is required for state to be saved
        id = R.id.stripe_payment_methods_add_fpx

        fpx_list.run {
            adapter = fpxAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = DefaultItemAnimator()
        }

        viewModel.fpxBankStatuses.observe(activity, Observer {
            onFpxBankStatusesUpdated(it)
        })
        viewModel.loadFpxBankStatues()

        viewModel.selectedPosition?.let {
            fpxAdapter.updateSelected(it)
        }
    }

    private fun onFpxBankStatusesUpdated(fpxBankStatuses: FpxBankStatuses?) {
        fpxBankStatuses?.let {
            fpxAdapter.updateStatuses(it)
        }
    }

    private class Adapter constructor(
        private val themeConfig: ThemeConfig,
        private val itemSelectedCallback: (Int) -> Unit
    ) : RecyclerView.Adapter<Adapter.ViewHolder>() {
        var selectedPosition = RecyclerView.NO_POSITION
            set(value) {
                if (value != field) {
                    if (selectedPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(field)
                    }
                    notifyItemChanged(value)
                    itemSelectedCallback(value)
                }
                field = value
            }

        private var fpxBankStatuses: FpxBankStatuses = FpxBankStatuses()

        internal val selectedBank: FpxBank?
            get() = if (selectedPosition == -1) {
                null
            } else {
                getItem(selectedPosition)
            }

        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, i: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.fpx_bank, parent, false)
            return ViewHolder(itemView, themeConfig)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
            viewHolder.setSelected(i == selectedPosition)

            val fpxBank = getItem(i)
            viewHolder.update(fpxBank, fpxBankStatuses.isOnline(fpxBank))
            viewHolder.itemView.setOnClickListener {
                selectedPosition = viewHolder.adapterPosition
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemCount(): Int {
            return FpxBank.values().size
        }

        private fun getItem(position: Int): FpxBank {
            return FpxBank.values()[position]
        }

        internal fun updateSelected(position: Int) {
            selectedPosition = position
            notifyItemChanged(position)
        }

        internal fun updateStatuses(fpxBankStatuses: FpxBankStatuses) {
            this.fpxBankStatuses = fpxBankStatuses

            // flag offline bank
            FpxBank.values().indices
                .filterNot { position ->
                    fpxBankStatuses.isOnline(getItem(position))
                }
                .forEach { position ->
                    notifyItemChanged(position)
                }
        }

        private class ViewHolder constructor(
            itemView: View,
            private val themeConfig: ThemeConfig
        ) : RecyclerView.ViewHolder(itemView) {
            private val resources: Resources = itemView.resources
            private val name: TextView = itemView.findViewById(R.id.name)
            private val icon: ImageView = itemView.findViewById(R.id.icon)
            private val checkMark: ImageView = itemView.findViewById(R.id.check_icon)

            internal fun update(fpxBank: FpxBank, isOnline: Boolean) {
                name.text = if (isOnline) {
                    fpxBank.displayName
                } else {
                    resources.getString(
                        R.string.fpx_bank_offline,
                        fpxBank.displayName
                    )
                }
                icon.setImageResource(fpxBank.brandIconResId)
            }

            internal fun setSelected(isSelected: Boolean) {
                name.setTextColor(themeConfig.getTextColor(isSelected))
                ImageViewCompat.setImageTintList(
                    checkMark,
                    ColorStateList.valueOf(themeConfig.getTintColor(isSelected))
                )
                checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
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
