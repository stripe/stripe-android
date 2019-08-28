package com.stripe.android.view

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.widget.ImageViewCompat
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.stripe.android.R
import com.stripe.android.model.PaymentMethodCreateParams

internal class AddPaymentMethodFpxView private constructor(
    context: Context
) : AddPaymentMethodView(context) {
    private val adapter = Adapter(ThemeConfig(context))

    override val createParams: PaymentMethodCreateParams?
        get() {
            val fpxBank = adapter.selectedBank ?: return null

            return PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Fpx.Builder()
                    .setBank(fpxBank.code)
                    .build()
            )
        }

    init {
        View.inflate(getContext(), R.layout.add_payment_method_fpx_layout, this)

        // an id is required for state to be saved
        id = R.id.payment_methods_add_fpx

        val recyclerView = findViewById<RecyclerView>(R.id.fpx_list)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.itemAnimator = null
    }

    override fun onSaveInstanceState(): Parcelable? {
        return SavedState(super.onSaveInstanceState(), adapter.selectedPosition)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            adapter.updateSelected(state.selectedPosition)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    private class Adapter constructor(
        private val themeConfig: ThemeConfig
    ) : RecyclerView.Adapter<Adapter.ViewHolder>() {
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
                    viewHolder.setSelected(true)
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
        ) : RecyclerView.ViewHolder(itemView) {

            private val name: TextView = itemView.findViewById(R.id.name)
            private val icon: AppCompatImageView = itemView.findViewById(R.id.icon)
            private val checkMark: AppCompatImageView = itemView.findViewById(R.id.check_icon)

            internal fun update(fpxBank: FpxBank) {
                name.text = fpxBank.displayName
                icon.setImageResource(fpxBank.brandIconResId)
            }

            internal fun setSelected(isSelected: Boolean) {
                name.setTextColor(themeConfig.getTextColor(isSelected))
                ImageViewCompat.setImageTintList(checkMark, ColorStateList.valueOf(themeConfig.getTintColor(isSelected)))
                checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            }
        }
    }

    private enum class FpxBank(val code: String, val displayName: String, val brandIconResId: Int = 0) {
        AffinBank("affin_bank", "Affin Bank"),
        AllianceBankBusiness("alliance_bank", "Alliance Bank (Business)"),
        AmBank("ambank", "AmBank"),
        BankIslam("bank_islam", "Bank Islam"),
        BankMuamalat("bank_muamalat", "Bank Muamalat"),
        BankRakyat("bank_rakyat", "Bank Rakyat"),
        Bsn("bsn", "BSN"),
        Cimb("cimb", "CIMB Clicks"),
        HongLeongBank("hong_leong_bank", "Hong Leong Bank"),
        Hsbc("hsbc", "HSBC BANK"),
        Kfh("kfh", "KFH"),
        Maybank2E("maybank2e", "Maybank2E"),
        Maybank2U("maybank2u", "Maybank2U"),
        Ocbc("ocbc", "OCBC Bank"),
        PublicBank("public_bank", "Public Bank"),
        Rhb("rhb", "RHB Bank"),
        StandardChartered("standard_chartered", "Standard Chartered"),
        UobBank("uob", "UOB Bank")
    }

    private class SavedState : BaseSavedState {
        internal val selectedPosition: Int

        constructor(superState: Parcelable?, selectedPosition: Int) : super(superState) {
            this.selectedPosition = selectedPosition
        }

        private constructor(`in`: Parcel) : super(`in`) {
            this.selectedPosition = `in`.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(selectedPosition)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
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
