package com.stripe.samplestore;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.wallet.Cart;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

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

    class ViewHolder extends RecyclerView.ViewHolder {
        private Currency mCurrency;
        private TextView mEmojiTextView;
        private TextView mPriceTextView;
        private TextView mQuantityTextView;
        private ImageButton mAddButton;
        private ImageButton mRemoveButton;

        private int mPosition;
        ViewHolder(final LinearLayout pollingLayout, Currency currency) {
            super(pollingLayout);
            mEmojiTextView = (TextView) pollingLayout.findViewById(R.id.tv_emoji);
            mPriceTextView = (TextView) pollingLayout.findViewById(R.id.tv_price);
            mQuantityTextView = (TextView) pollingLayout.findViewById(R.id.tv_quantity);
            mAddButton = (ImageButton) pollingLayout.findViewById(R.id.tv_plus);
            mRemoveButton = (ImageButton) pollingLayout.findViewById(R.id.tv_minus);

            mCurrency = currency;
            mAddButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StoreAdapter.this.bumpItemQuantity(mPosition, true);
                }
            });

            mRemoveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StoreAdapter.this.bumpItemQuantity(mPosition, false);
                }
            });
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
    private Activity mActivity;
    private Currency mCurrency;

    private int[] mQuantityOrdered;
    private int mTotalOrdered;
    private TotalItemsChangedListener mTotalItemsChangedListener;

    public StoreAdapter(StoreActivity activity) {
        mActivity = activity;
        mTotalItemsChangedListener = activity;
        // Note: our sample backend assumes USD as currency. This code would be
        // otherwise functional if you switched that assumption on the backend and passed
        // currency code as a parameter.
        mCurrency = Currency.getInstance("USD");
        mQuantityOrdered = new int[EMOJI_CLOTHES.length];
    }

    public void bumpItemQuantity(int index, boolean increase) {
        if (index >= 0 && index < mQuantityOrdered.length) {
            if (increase) {
                mQuantityOrdered[index]++;
                mTotalOrdered++;
                mTotalItemsChangedListener.onTotalItemsChanged(mTotalOrdered);
            } else if(mQuantityOrdered[index] > 0) {
                mQuantityOrdered[index]--;
                mTotalOrdered--;
                mTotalItemsChangedListener.onTotalItemsChanged(mTotalOrdered);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
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

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout pollingView = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.store_item, parent, false);

        return new ViewHolder(pollingView, mCurrency);
    }

    public void launchPurchaseActivityWithCart() {
        // Generating a CartManager using the current currency in my AndroidPayConfiguration
        CartManager cartManager = new CartManager();
        for (int i = 0; i < mQuantityOrdered.length; i++) {
            if (mQuantityOrdered[i] > 0) {
                cartManager.addLineItem(StoreUtils.getEmojiByUnicode(EMOJI_CLOTHES[i]),
                        (double) mQuantityOrdered[i], EMOJI_PRICES[i]);
            }
        }

        try {
            Cart cart = cartManager.buildCart();
            Intent paymentLaunchIntent = PaymentActivity.createIntent(mActivity, cart);
            mActivity.startActivityForResult(
                    paymentLaunchIntent, StoreActivity.PURCHASE_REQUEST);
        } catch (CartContentException unexpected) {
            // There shouldn't be any cart content exceptions because we used the CartManager tools.
        }
    }

    public void clearItemSelections() {
        mQuantityOrdered = new int[EMOJI_CLOTHES.length];
        notifyDataSetChanged();
    }

    public interface TotalItemsChangedListener {
        void onTotalItemsChanged(int totalItems);
    }
}
