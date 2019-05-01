package com.stripe.samplestore;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Currency;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {

    private static final int[] EMOJI_CLOTHES = {
            0x1F455,
            0x1F456,
            0x1F457,
            0x1F458,
            0x1F459,
            0x1F45A,
            0x1F45B,
            0x1F45C,
            0x1F45D,
            0x1F45E,
            0x1F45F,
            0x1F460,
            0x1F461,
            0x1F462
    };

    private static final int[] EMOJI_PRICES = {
            2000,
            4000,
            3000,
            700,
            600,
            1000,
            2000,
            2500,
            800,
            3000,
            2000,
            5000,
            5500,
            6000
    };

    static class ViewHolder extends RecyclerView.ViewHolder {
        @NonNull private final Currency mCurrency;
        @NonNull private final TextView mEmojiTextView;
        @NonNull private final TextView mPriceTextView;
        @NonNull private final TextView mQuantityTextView;
        @NonNull private final ImageButton mAddButton;
        @NonNull private final ImageButton mRemoveButton;

        private int mPosition;

        ViewHolder(@NonNull final View pollingLayout, @NonNull Currency currency,
                   @NonNull StoreAdapter adapter) {
            super(pollingLayout);
            mEmojiTextView = pollingLayout.findViewById(R.id.tv_emoji);
            mPriceTextView = pollingLayout.findViewById(R.id.tv_price);
            mQuantityTextView = pollingLayout.findViewById(R.id.tv_quantity);
            mAddButton = pollingLayout.findViewById(R.id.tv_plus);
            mRemoveButton = pollingLayout.findViewById(R.id.tv_minus);

            mCurrency = currency;
            mAddButton.setOnClickListener(v -> adapter.bumpItemQuantity(mPosition, true));

            mRemoveButton.setOnClickListener(v -> adapter.bumpItemQuantity(mPosition, false));
        }

        void setHidden(boolean hidden) {
            int visibility = hidden ? View.INVISIBLE : View.VISIBLE;
            mEmojiTextView.setVisibility(visibility);
            mPriceTextView.setVisibility(visibility);
            mQuantityTextView.setVisibility(visibility);
            mAddButton.setVisibility(visibility);
            mRemoveButton.setVisibility(visibility);
        }

        void setEmoji(int emojiUnicode) {
            mEmojiTextView.setText(StoreUtils.getEmojiByUnicode(emojiUnicode));
        }

        void setPrice(int price) {
            mPriceTextView.setText(StoreUtils.getPriceString(price, mCurrency));
        }

        void setQuantity(int quantity) {
            mQuantityTextView.setText(String.valueOf(quantity));
        }

        void setPosition(int position) {
            mPosition = position;
        }
    }

    // Storing an activity here only so we can launch for result
    @NonNull private final Activity mActivity;
    @NonNull private final Currency mCurrency;

    private int[] mQuantityOrdered;
    private int mTotalOrdered;
    @NonNull private final TotalItemsChangedListener mTotalItemsChangedListener;

    StoreAdapter(@NonNull StoreActivity activity) {
        mActivity = activity;
        mTotalItemsChangedListener = activity;
        // Note: our sample backend assumes USD as currency. This code would be
        // otherwise functional if you switched that assumption on the backend and passed
        // currency code as a parameter.
        mCurrency = Currency.getInstance("USD");
        mQuantityOrdered = new int[EMOJI_CLOTHES.length];
    }

    private void bumpItemQuantity(int index, boolean increase) {
        if (index >= 0 && index < mQuantityOrdered.length) {
            if (increase) {
                mQuantityOrdered[index]++;
                mTotalOrdered++;
                mTotalItemsChangedListener.onTotalItemsChanged(mTotalOrdered);
            } else if (mQuantityOrdered[index] > 0) {
                mQuantityOrdered[index]--;
                mTotalOrdered--;
                mTotalItemsChangedListener.onTotalItemsChanged(mTotalOrdered);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position == EMOJI_CLOTHES.length) {
            holder.setHidden(true);
        } else {
            holder.setHidden(false);
            holder.setEmoji(EMOJI_CLOTHES[position]);
            holder.setPrice(EMOJI_PRICES[position]);
            holder.setQuantity(mQuantityOrdered[position]);
            holder.setPosition(position);
        }

    }

    @Override
    public int getItemCount() {
        return EMOJI_CLOTHES.length + 1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View pollingView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.store_item, parent, false);

        return new ViewHolder(pollingView, mCurrency, this);
    }

    void launchPurchaseActivityWithCart() {
        StoreCart cart = new StoreCart(mCurrency);
        for (int i = 0; i < mQuantityOrdered.length; i++) {
            if (mQuantityOrdered[i] > 0) {
                cart.addStoreLineItem(
                        StoreUtils.getEmojiByUnicode(EMOJI_CLOTHES[i]),
                        mQuantityOrdered[i], EMOJI_PRICES[i]);
            }
        }

        Intent paymentLaunchIntent = PaymentActivity.createIntent(mActivity, cart);
        mActivity.startActivityForResult(
                paymentLaunchIntent, StoreActivity.PURCHASE_REQUEST);
    }

    void clearItemSelections() {
        mQuantityOrdered = new int[EMOJI_CLOTHES.length];
        notifyDataSetChanged();
        mTotalItemsChangedListener.onTotalItemsChanged(0);
    }

    public interface TotalItemsChangedListener {
        void onTotalItemsChanged(int totalItems);
    }
}
