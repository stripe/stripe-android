package com.stripe.android.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.databinding.FpxBankItemBinding

enum class PaymentMethodListType {
    PaymentMethodListTypeFpx,
    PaymentMethodListTypeNetbanking,
}

class AddPaymentMethodListAdapter constructor (
    var activity: FragmentActivity,
    var items : Array<FpxBank>,
    val itemSelectedCallback: (Int) -> Unit,
    var paymentMethodListType : PaymentMethodListType
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var statuses : Array<Boolean>

    // TODO: use a generic type, <Any> doesn't seem to work

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

    // TODO: use PaymentMethod.Type
    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // TODO Switch on paymentMethodListType, figure out a default
        return AddPaymentMethodFpxView.BankViewHolder(
            FpxBankItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            ThemeConfig(activity)
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
            Log.d("Ali click", item.toString())
            selectedPosition = holder.adapterPosition
        }

        val bankViewHolder = holder as AddPaymentMethodFpxView.BankViewHolder
        bankViewHolder.setSelected(position == selectedPosition)
        // TODO: figure out bank statuses
        bankViewHolder.update(fpxBank = item, isOnline = true)
    }

    internal fun updateSelected(position: Int) {
        selectedPosition = position
        notifyItemChanged(position)
    }

    fun notifyAdapterItemChanged(position: Int) {
        notifyItemChanged(position)
    }
}