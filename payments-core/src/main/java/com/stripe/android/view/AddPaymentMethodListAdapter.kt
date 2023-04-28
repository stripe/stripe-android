package com.stripe.android.view

import android.content.res.ColorStateList
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.databinding.StripeBankItemBinding
import com.stripe.android.model.BankStatuses

internal class AddPaymentMethodListAdapter(
    val themeConfig: ThemeConfig,
    val items: List<Bank>,
    val itemSelectedCallback: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal var bankStatuses: BankStatuses? = null

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

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return BankViewHolder(
            StripeBankItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            themeConfig
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        holder.itemView.setOnClickListener {
            selectedPosition = holder.bindingAdapterPosition
        }

        val bankViewHolder = holder as BankViewHolder
        bankViewHolder.setSelected(position == selectedPosition)

        bankViewHolder.update(bank = item, isOnline = bankStatuses?.isOnline(item) ?: true)
    }

    internal fun updateSelected(position: Int) {
        selectedPosition = position
        notifyItemChanged(position)
    }

    fun notifyAdapterItemChanged(position: Int) {
        notifyItemChanged(position)
    }

    internal class BankViewHolder constructor(
        private val viewBinding: StripeBankItemBinding,
        private val themeConfig: ThemeConfig
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        private val resources: Resources = itemView.resources

        fun update(bank: Bank, isOnline: Boolean) {
            viewBinding.name.text = if (isOnline) {
                bank.displayName
            } else {
                resources.getString(
                    R.string.stripe_fpx_bank_offline,
                    bank.displayName
                )
            }

            bank.brandIconResId?.let {
                viewBinding.icon.setImageResource(it)
            }
        }

        internal fun setSelected(isSelected: Boolean) {
            viewBinding.name.setTextColor(themeConfig.getTextColor(isSelected))
            ImageViewCompat.setImageTintList(
                viewBinding.checkIcon,
                ColorStateList.valueOf(themeConfig.getTintColor(isSelected))
            )
            viewBinding.checkIcon.isVisible = isSelected
        }
    }
}
