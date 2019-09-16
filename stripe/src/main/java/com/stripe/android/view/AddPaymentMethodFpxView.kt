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

        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.fpx_list)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
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
                ImageViewCompat.setImageTintList(checkMark, ColorStateList.valueOf(themeConfig.getTintColor(isSelected)))
                checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            }
        }
    }

    private enum class FpxBank(val code: String, val displayName: String, val brandIconResId: Int = R.drawable.ic_bank_generic) {
        AffinBank("affin_bank", "Affin Bank", brandIconResId = R.drawable.ic_bank_affin),
        AllianceBankBusiness("alliance_bank", "Alliance Bank (Business)", brandIconResId = R.drawable.ic_bank_alliance),
        AmBank("ambank", "AmBank", brandIconResId = R.drawable.ic_bank_ambank),
        BankIslam("bank_islam", "Bank Islam", brandIconResId = R.drawable.ic_bank_islam),
        BankMuamalat("bank_muamalat", "Bank Muamalat", brandIconResId = R.drawable.ic_bank_muamalat),
        BankRakyat("bank_rakyat", "Bank Rakyat", brandIconResId = R.drawable.ic_bank_raykat),
        Bsn("bsn", "BSN", brandIconResId = R.drawable.ic_bank_bsn),
        Cimb("cimb", "CIMB Clicks", brandIconResId = R.drawable.ic_bank_cimb),
        HongLeongBank("hong_leong_bank", "Hong Leong Bank", brandIconResId = R.drawable.ic_bank_hong_leong),
        Hsbc("hsbc", "HSBC BANK", brandIconResId = R.drawable.ic_bank_hsbc),
        Kfh("kfh", "KFH", brandIconResId = R.drawable.ic_bank_kfh),
        Maybank2E("maybank2e", "Maybank2E", brandIconResId = R.drawable.ic_bank_maybank),
        Maybank2U("maybank2u", "Maybank2U", brandIconResId = R.drawable.ic_bank_maybank),
        Ocbc("ocbc", "OCBC Bank", brandIconResId = R.drawable.ic_bank_ocbc),
        PublicBank("public_bank", "Public Bank", brandIconResId = R.drawable.ic_bank_public),
        Rhb("rhb", "RHB Bank", brandIconResId = R.drawable.ic_bank_rhb),
        StandardChartered("standard_chartered", "Standard Chartered", brandIconResId = R.drawable.ic_bank_standard_chartered),
        UobBank("uob", "UOB Bank", brandIconResId = R.drawable.ic_bank_uob)
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
