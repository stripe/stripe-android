package com.stripe.android.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.stripe.android.R;
import com.stripe.android.model.PaymentMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RecyclerView.Adapter} that holds a set of {@link MaskedCardView} items for a given set
 * of {@link PaymentMethod} objects.
 */
class MaskedCardAdapter extends RecyclerView.Adapter<MaskedCardAdapter.ViewHolder> {

    private static final int NO_SELECTION = -1;
    @NonNull private final List<PaymentMethod> mPaymentMethods;
    private int mSelectedIndex = NO_SELECTION;

    MaskedCardAdapter(@NonNull List<PaymentMethod> paymentMethods) {
        mPaymentMethods = new ArrayList<>();
        setPaymentMethods(paymentMethods);
    }

    void setPaymentMethods(@NonNull List<PaymentMethod> paymentMethods) {
        final PaymentMethod selectedPaymentMethod = getSelectedPaymentMethod();
        final String selectedPaymentMethodId =
                selectedPaymentMethod != null ? selectedPaymentMethod.id : null;

        mPaymentMethods.clear();
        mPaymentMethods.addAll(paymentMethods);

        // if there were no selected payment methods, or the previously selected payment method
        // was not found and set selected, select the newest payment method
        if (selectedPaymentMethodId == null || !setSelectedPaymentMethod(selectedPaymentMethodId)) {
            setSelectedIndex(getNewestPaymentMethodIndex());
        }

        notifyDataSetChanged();
    }

    private int getNewestPaymentMethodIndex() {
        int index = NO_SELECTION;
        Long created = 0L;
        for (int i = 0; i < mPaymentMethods.size(); i++) {
            final PaymentMethod paymentMethod = mPaymentMethods.get(i);
            if (paymentMethod.created != null && paymentMethod.created > created) {
                created = paymentMethod.created;
                index = i;
            }
        }

        return index;
    }

    @Override
    public int getItemCount() {
        return mPaymentMethods.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setMaskedCardData(mPaymentMethods.get(position));
        holder.setIndex(position);
        holder.setSelected(position == mSelectedIndex);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout itemView = (FrameLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.masked_card_row, parent, false);
        return new ViewHolder(itemView);
    }

    /**
     * Sets the selected payment method to the one whose ID is identical to the input string, if
     * such a value is found.
     *
     * @param paymentMethodId a stripe ID to search for among the list of {@link PaymentMethod}
     *         objects
     * @return {@code true} if the value was found, {@code false} if not
     */
    boolean setSelectedPaymentMethod(@NonNull String paymentMethodId) {
        for (int i = 0; i < mPaymentMethods.size(); i++) {
            if (paymentMethodId.equals(mPaymentMethods.get(i).id)) {
                setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    @Nullable
    String getSelectedPaymentMethodId() {
        if (mSelectedIndex == NO_SELECTION) {
            return null;
        }
        return mPaymentMethods.get(mSelectedIndex).id;
    }

    @Nullable
    PaymentMethod getSelectedPaymentMethod() {
        if (mSelectedIndex == NO_SELECTION) {
            return null;
        }

        return mPaymentMethods.get(mSelectedIndex);
    }

    void setSelectedIndex(int selectedIndex) {
        mSelectedIndex = selectedIndex;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @NonNull private final MaskedCardView maskedCardView;
        private int index;

        ViewHolder(FrameLayout itemLayout) {
            super(itemLayout);
            maskedCardView = itemLayout.findViewById(R.id.masked_card_item);
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!maskedCardView.isSelected()) {
                        maskedCardView.toggleSelected();
                        setSelectedIndex(index);
                        notifyDataSetChanged();
                    }
                }
            });
        }

        void setMaskedCardData(@NonNull PaymentMethod paymentMethod) {
            maskedCardView.setPaymentMethod(paymentMethod);
        }

        void setIndex(int index) {
            this.index = index;
        }

        void setSelected(boolean selected) {
            maskedCardView.setSelected(selected);
        }
    }
}
