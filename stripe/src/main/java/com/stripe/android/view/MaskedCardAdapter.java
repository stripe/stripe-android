package com.stripe.android.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.stripe.android.R;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.Source;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RecyclerView.Adapter} that holds a set of {@link MaskedCardView} items for a given set
 * of {@link CustomerSource} objects.
 */
public class MaskedCardAdapter extends RecyclerView.Adapter<MaskedCardAdapter.ViewHolder> {

    private static final int NO_SELECTION = -1;
    private @NonNull List<CustomerSource> mCustomerSourceList;
    private int mSelectedIndex = NO_SELECTION;

    public MaskedCardAdapter(@NonNull List<CustomerSource> startingSources) {
        mCustomerSourceList = new ArrayList<>();
        CustomerSource[] customerSources = new CustomerSource[startingSources.size()];
        addCustomerSourceIfSupported(startingSources.toArray(customerSources));
    }

    public void addCustomerSource(@NonNull CustomerSource customerSource) {
        addCustomerSourceIfSupported(customerSource);
    }

    @Nullable
    public CustomerSource getSelectedCustomerSource() {
        if (mSelectedIndex == NO_SELECTION) {
            return null;
        }

        return mCustomerSourceList.get(mSelectedIndex);
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
     * Clear all {@link CustomerSource} objects stored in this adapter.
     */
    public void clearSources() {
        mCustomerSourceList.clear();
        notifyDataSetChanged();
    }

    /**
     * Sets the selected source to the one whose ID is identical to the input string, if such
     * a value is found.
     *
     * @param sourceId a stripe ID to search for among the list of {@link CustomerSource} objects
     * @return {@code true} if the value was found, {@code false} if not
     */
    public boolean setSelectedSource(@NonNull String sourceId) {
        for (int i = 0; i < mCustomerSourceList.size(); i++) {
            if (sourceId.equals(mCustomerSourceList.get(i).getId())) {
                updateSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    @Nullable
    public String getSelectedSource() {
        if (mSelectedIndex == NO_SELECTION) {
            return null;
        }

        return mCustomerSourceList.get(mSelectedIndex).getId();
    }

    void addCustomerSourceIfSupported(CustomerSource... customerSources) {
        if (customerSources == null) {
            return;
        }

        for(CustomerSource customerSource : customerSources) {
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
