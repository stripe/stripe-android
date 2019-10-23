package com.stripe.android.view

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.stripe.android.R
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.android.synthetic.main.add_payment_method_fpx_layout.view.*

internal class AddPaymentMethodFpxView private constructor(
    context: Context
) : AddPaymentMethodView(context) {
    private val fpxAdapter = Adapter(ThemeConfig(context))

    override val createParams: PaymentMethodCreateParams?
        get() {
            val fpxBank = fpxAdapter.selectedBank ?: return null

            return PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Fpx.Builder()
                    .setBank(fpxBank.code)
                    .build()
            )
        }

    init {
        View.inflate(getContext(), R.layout.add_payment_method_fpx_layout, this)

        // an id is required for state to be saved
        id = R.id.stripe_payment_methods_add_fpx

        fpx_list.run {
            adapter = fpxAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        return SavedState(super.onSaveInstanceState(), fpxAdapter.selectedPosition)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            fpxAdapter.updateSelected(state.selectedPosition)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class Adapter constructor(
        private val themeConfig: ThemeConfig
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<Adapter.ViewHolder>() {
        var selectedPosition = -1

        internal val selectedBank: FpxBank?
            get() = if (selectedPosition == -1) {
                null
            } else {
                FpxBank.values()[selectedPosition]
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
            viewHolder.update(FpxBank.values()[i])
            viewHolder.itemView.setOnClickListener {
                val currentPosition = viewHolder.adapterPosition
                if (currentPosition != selectedPosition) {
                    val prevSelectedPosition = selectedPosition
                    selectedPosition = currentPosition
                    notifyItemChanged(prevSelectedPosition)
                    notifyItemChanged(currentPosition)
                }
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemCount(): Int {
            return FpxBank.values().size
        }

        fun updateSelected(position: Int) {
            selectedPosition = position
            notifyItemChanged(position)
        }

        private class ViewHolder constructor(
            itemView: View,
            private val themeConfig: ThemeConfig
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

            private val name: TextView = itemView.findViewById(R.id.name)
            private val icon: AppCompatImageView = itemView.findViewById(R.id.icon)
            private val checkMark: AppCompatImageView = itemView.findViewById(R.id.check_icon)

            internal fun update(fpxBank: FpxBank) {
                name.text = fpxBank.displayName
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

    private class SavedState : BaseSavedState {
        internal val selectedPosition: Int

        constructor(superState: Parcelable?, selectedPosition: Int) : super(superState) {
            this.selectedPosition = selectedPosition
        }

        private constructor(parcel: Parcel) : super(parcel) {
            this.selectedPosition = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(selectedPosition)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun create(context: Context): AddPaymentMethodFpxView {
            return AddPaymentMethodFpxView(context)
        }
    }
}
