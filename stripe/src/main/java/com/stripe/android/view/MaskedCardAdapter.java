package com.stripe.android.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.stripe.android.R;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.Source;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RecyclerView.Adapter} that holds a set of {@link MaskedCardView} items for a given set
 * of {@link CustomerSource} objects.
 */
class MaskedCardAdapter extends RecyclerView.Adapter<MaskedCardAdapter.ViewHolder> {

    private static final int NO_SELECTION = -1;
    private @NonNull List<CustomerSource> mCustomerSourceList;
    private int mSelectedIndex = NO_SELECTION;

    MaskedCardAdapter(@NonNull List<CustomerSource> startingSources) {
        mCustomerSourceList = new ArrayList<>();
        setCustomerSourceList(startingSources);
    }

    void setCustomerSourceList(@NonNull List<CustomerSource> sourceList) {
        mCustomerSourceList.clear();
        CustomerSource[] customerSources = new CustomerSource[sourceList.size()];
        addCustomerSourceIfSupported(sourceList.toArray(customerSources));
    }

    void updateCustomer(@NonNull Customer customer) {
        mCustomerSourceList.clear();
        CustomerSource[] customerSources = new CustomerSource[customer.getSources().size()];
        addCustomerSourceIfSupported(customer.getSources().toArray(customerSources));
        String sourceId = customer.getDefaultSource();
        if (sourceId == null) {
            updateSelectedIndex(NO_SELECTION);
        } else {
            setSelectedSource(sourceId);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mCustomerSourceList.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setMaskedCardData(mCustomerSourceList.get(position));
        holder.setIndex(position);
        holder.setSelected(position == mSelectedIndex);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        FrameLayout itemView = (FrameLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.masked_card_row, parent, false);
        return new ViewHolder(itemView);
    }

    /**
     * Sets the selected source to the one whose ID is identical to the input string, if such
     * a value is found.
     *
     * @param sourceId a stripe ID to search for among the list of {@link CustomerSource} objects
     * @return {@code true} if the value was found, {@code false} if not
     */
    boolean setSelectedSource(@NonNull String sourceId) {
        for (int i = 0; i < mCustomerSourceList.size(); i++) {
            if (sourceId.equals(mCustomerSourceList.get(i).getId())) {
                updateSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    @Nullable
    CustomerSource getSelectedSource() {
        if (mSelectedIndex == NO_SELECTION) {
            return null;
        }

        return mCustomerSourceList.get(mSelectedIndex);
    }

    void addCustomerSourceIfSupported(CustomerSource... customerSources) {
        if (customerSources == null) {
            return;
        }

        for (CustomerSource customerSource : customerSources) {
            if (customerSource.asCard() != null || canDisplaySource(customerSource.asSource())) {
                // If it's a card, we can display it.
                mCustomerSourceList.add(customerSource);
            }
        }

        notifyDataSetChanged();
    }

    boolean canDisplaySource(@Nullable Source source) {
        return source != null && Source.CARD.equals(source.getType());
    }

    void updateSelectedIndex(int selectedIndex) {
        mSelectedIndex = selectedIndex;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        MaskedCardView maskedCardView;
        int index;

        ViewHolder(FrameLayout itemLayout) {
            super(itemLayout);
            maskedCardView = itemLayout.findViewById(R.id.masked_card_item);
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!maskedCardView.isSelected()) {
                        maskedCardView.toggleSelected();
                        updateSelectedIndex(index);
                    }
                }
            });
        }

        void setMaskedCardData(@NonNull CustomerSource customerSource) {
            maskedCardView.setCustomerSource(customerSource);
        }

        void setIndex(int index) {
            this.index = index;
        }

        void setSelected(boolean selected) {
            maskedCardView.setSelected(selected);
        }
    }
}
