package com.stripe.android.view;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stripe.android.R;
import com.stripe.android.model.PaymentMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link RecyclerView.Adapter} that holds a set of {@link MaskedCardView} items for a given set
 * of {@link PaymentMethod} objects.
 */
final class PaymentMethodsAdapter extends RecyclerView.Adapter<PaymentMethodsAdapter.ViewHolder> {

    private static final int TYPE_CARD = 0;

    private static final int NO_SELECTION = -1;
    @NonNull private final List<PaymentMethod> mPaymentMethods = new ArrayList<>();
    private int mSelectedIndex = NO_SELECTION;

    PaymentMethodsAdapter() {
        setHasStableIds(true);
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
        long created = 0L;
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
    public int getItemViewType(int position) {
        final String type = mPaymentMethods.get(position).type;
        if (PaymentMethod.Type.Card.code.equals(type)) {
            return TYPE_CARD;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return Objects.requireNonNull(mPaymentMethods.get(position).id).hashCode();
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.setPaymentMethod(mPaymentMethods.get(position));
        holder.setSelected(position == mSelectedIndex);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int currentPosition = holder.getAdapterPosition();
                if (currentPosition != mSelectedIndex) {
                    holder.toggleSelected();
                    setSelectedIndex(currentPosition);
                    notifyDataSetChanged();
                }
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        @LayoutRes final int layoutRes;
        if (viewType == TYPE_CARD) {
            layoutRes = R.layout.masked_card_row;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + viewType);
        }
        final View itemView = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        return new ViewHolder(itemView);
    }

    /**
     * Sets the selected payment method based on ID.
     *
     * @param paymentMethodId the ID of the {@link PaymentMethod} to select
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
    PaymentMethod getSelectedPaymentMethod() {
        if (mSelectedIndex == NO_SELECTION) {
            return null;
        }

        return mPaymentMethods.get(mSelectedIndex);
    }

    void setSelectedIndex(int selectedIndex) {
        mSelectedIndex = selectedIndex;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @NonNull private final MaskedCardView mCardView;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            mCardView = itemView.findViewById(R.id.masked_card_item);
        }

        private void setPaymentMethod(@NonNull PaymentMethod paymentMethod) {
            mCardView.setPaymentMethod(paymentMethod);
        }

        private void setSelected(boolean selected) {
            mCardView.setSelected(selected);
        }

        private void toggleSelected() {
            mCardView.toggleSelected();
        }
    }
}
