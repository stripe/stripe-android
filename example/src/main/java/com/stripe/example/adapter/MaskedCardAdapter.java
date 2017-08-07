package com.stripe.example.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.stripe.android.model.SourceCardData;
import com.stripe.android.view.MaskedCardView;
import com.stripe.example.R;

import java.util.ArrayList;
import java.util.List;

/**
 * An adapter that displays {@link MaskedCardView} objects
 */
public class MaskedCardAdapter extends RecyclerView.Adapter<MaskedCardAdapter.ViewHolder> {

    private static final int NO_SELECTION = -1;

    class ViewHolder extends RecyclerView.ViewHolder {
        MaskedCardView mMaskedCardView;

        private int mIndex;

        ViewHolder(FrameLayout itemLayout) {
            super(itemLayout);
            mMaskedCardView = itemLayout.findViewById(R.id.mcv);
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mMaskedCardView.toggleSelected();
                    if (mMaskedCardView.getIsSelected()) {
                        updateSelected(mIndex);
                    } else {
                        updateSelected(NO_SELECTION);
                    }
                }
            });
        }

        void setMaskedCardData(@NonNull SourceCardData sourceCardData) {
            mMaskedCardView.setCardData(sourceCardData);
        }

        void setIndex(int index) {
            mIndex = index;
        }

        void setSelected(boolean isSelected) {
            mMaskedCardView.setIsSelected(isSelected);
        }
    }

    private List<SourceCardData> mCardList;
    private int mSelectedIndex = NO_SELECTION;

    public MaskedCardAdapter() {
        mCardList = new ArrayList<>();
    }

    public void load() {
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SourceCardData card = mCardList.get(position);
        holder.setMaskedCardData(card);
        holder.setIndex(position);
        holder.setSelected(position == mSelectedIndex);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        FrameLayout itemView = (FrameLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_item_layout, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public int getItemCount() {
        return mCardList.size();
    }

    private void updateSelected(int selectedIndex) {
        mSelectedIndex = selectedIndex;
        notifyDataSetChanged();
    }

    public void clear() {
        mCardList.clear();
        mSelectedIndex = NO_SELECTION;
        notifyDataSetChanged();
    }

    public void addSourceCardData(@NonNull SourceCardData data) {
        if (mCardList.isEmpty()) {
            mSelectedIndex = 0;
        }
        mCardList.add(data);
        notifyDataSetChanged();
    }
}
