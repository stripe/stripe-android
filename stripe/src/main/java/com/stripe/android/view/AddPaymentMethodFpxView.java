package com.stripe.android.view;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stripe.android.R;
import com.stripe.android.model.PaymentMethodCreateParams;

class AddPaymentMethodFpxView extends AddPaymentMethodView {
    @NonNull private final Adapter mAdapter;

    @NonNull
    static AddPaymentMethodFpxView create(@NonNull Context context) {
        return new AddPaymentMethodFpxView(context);
    }

    private AddPaymentMethodFpxView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.add_payment_method_fpx_layout, this);

        // an id is required for state to be saved
        setId(R.id.payment_methods_add_fpx);

        mAdapter = new Adapter(new ThemeConfig(context));
        final RecyclerView recyclerView = findViewById(R.id.fpx_list);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setItemAnimator(null);
    }

    @Nullable
    @Override
    PaymentMethodCreateParams getCreateParams() {
        final FpxBank fpxBank = mAdapter.getSelectedBank();
        if (fpxBank == null) {
            return null;
        }

        return PaymentMethodCreateParams.create(
                new PaymentMethodCreateParams.Fpx.Builder()
                        .setBank(fpxBank.code)
                        .build()
        );
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), mAdapter.mSelectedPosition);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            final SavedState savedState = (SavedState) state;
            super.onRestoreInstanceState(savedState.getSuperState());
            mAdapter.updateSelected(savedState.selectedPosition);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static final class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private int mSelectedPosition = -1;

        @NonNull private final ThemeConfig mThemeConfig;

        private Adapter(@NonNull ThemeConfig themeConfig) {
            super();
            setHasStableIds(true);
            mThemeConfig = themeConfig;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            final View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fpx_bank, parent, false);
            return new ViewHolder(itemView, mThemeConfig);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
            viewHolder.setSelected(i == mSelectedPosition);
            viewHolder.update(FpxBank.values()[i].displayName);
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO(mshafrir): update UI for selected bank
                    final int currentPosition = viewHolder.getAdapterPosition();
                    if (currentPosition != mSelectedPosition) {
                        final int prevSelectedPosition = mSelectedPosition;
                        mSelectedPosition = currentPosition;
                        viewHolder.setSelected(true);
                        notifyItemChanged(prevSelectedPosition);
                        notifyItemChanged(currentPosition);
                    }
                }
            });
        }

        @Override
        public long getItemId(int position) {
            return FpxBank.values()[position].hashCode();
        }

        @Override
        public int getItemCount() {
            return FpxBank.values().length;
        }

        @Nullable
        FpxBank getSelectedBank() {
            if (mSelectedPosition == -1) {
                return null;
            } else {
                return FpxBank.values()[mSelectedPosition];
            }
        }

        private void updateSelected(int position) {
            mSelectedPosition = position;
            notifyItemChanged(position);
        }

        private static final class ViewHolder extends RecyclerView.ViewHolder {
            @NonNull private final TextView mName;
            @NonNull private final ThemeConfig mThemeConfig;

            private ViewHolder(@NonNull View itemView, @NonNull ThemeConfig themeConfig) {
                super(itemView);
                mName = itemView.findViewById(R.id.name);
                mThemeConfig = themeConfig;
            }

            void update(@NonNull String bankName) {
                mName.setText(bankName);
            }

            void setSelected(boolean isSelected) {
                mName.setTextColor(mThemeConfig.getTextColor(isSelected));
            }
        }
    }

    private enum FpxBank {
        // TODO(mshafrir): add complete bank list

        AffinBank("affin_bank", "Affin Bank"),
        AllianceBankBusiness("alliance_bank", "Alliance Bank (Business)"),
        AmBank("ambank", "AmBank"),
        BankMuamalat("bank_muamalat", "Bank Muamalat"),
        DeutscheBank("deutsche_bank", "Deutsche Bank"),
        Maybank2e("maybank2e", "Maybank2E"),
        PublicBankEnterprise("pb_enterprise", "PB Enterprise"),
        StandardChartered("standard_chartered", "Standard Chartered"),
        UobBank("uob", "UOB Bank"),
        UobRegional("uob_regional", "UOB Regional");

        @NonNull private final String code;
        @NonNull private final String displayName;

        FpxBank(@NonNull String code, @NonNull String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
    }

    private static class SavedState extends BaseSavedState {
        final int selectedPosition;

        private SavedState(@Nullable Parcelable superState, int selectedPosition) {
            super(superState);
            this.selectedPosition = selectedPosition;
        }

        private SavedState(@NonNull Parcel in) {
            super(in);
            this.selectedPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(selectedPosition);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @NonNull
                    public SavedState createFromParcel(@NonNull Parcel in) {
                        return new SavedState(in);
                    }

                    @NonNull
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
