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

class AddPaymentMethodListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder> {
    lateinit  var activity: FragmentActivity

    // TODO: use a generic type, <Any> doesn't seem to work
    var items : Array<FpxBank>
    var selectedPosition : Int = 0

    // TODO: use PaymentMethod.Type
    lateinit var paymentMethodListType : PaymentMethodListType

    constructor(
        activity: FragmentActivity,
        paymentMethodListType: PaymentMethodListType,
        items : Array<FpxBank>) {
        this.activity = activity
        this.paymentMethodListType = paymentMethodListType
        this.items = items
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // TODO Switch on paymentMethodListType, figure out a default
        return AddPaymentMethodFpxView.ViewHolder(
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
//        holder.update(item)

        holder.itemView.setOnClickListener {
            Log.d("Ali click", item.toString())
            selectedPosition = position
        }
    }

    internal fun updateSelected(position: Int) {
        selectedPosition = position
        notifyItemChanged(position)
    }
}