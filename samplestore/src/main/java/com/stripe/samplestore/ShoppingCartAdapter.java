package com.stripe.samplestore;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.utils.CartManager;
import com.stripe.wrap.pay.utils.PaymentUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShoppingCartAdapter extends RecyclerView.Adapter<ShoppingCartAdapter.ViewHolder> {

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mLabel;
        private TextView mQuantityView;
        private TextView mUnitPriceView;
        private TextView mTotalPriceView;

        ViewHolder(View view) {
            super(view);
            mLabel = (TextView) view.findViewById(R.id.tv_cart_emoji);
            mQuantityView = (TextView) view.findViewById(R.id.tv_cart_quantity);
            mUnitPriceView = (TextView) view.findViewById(R.id.tv_cart_unit_price);
            mTotalPriceView = (TextView) view.findViewById(R.id.tv_cart_total_price);
        }

        void setLabelText(String text) {
            mLabel.setText(text);
        }

        void setQuantityText(String text) {
            mQuantityView.setText(text);
        }

        void setUnitPriceText(String text) {
            mUnitPriceView.setText(text);
        }

        void setTotalPriceText(String text) {
            mTotalPriceView.setText(text);
        }
    }

    private CartManager mCartManager;
    private String mCurrencyMarker;
    private int mRegularItemsCount;
    private int mShippingItemsCount;
    private List<String> mRegularItemIds;
    private List<String> mShippingItemIds;
    private boolean mHasTax;

    ShoppingCartAdapter(@NonNull Cart cart) {
        mCartManager = new CartManager(cart, true, true);
        mRegularItemsCount = mCartManager.getLineItemsRegular().size();
        mShippingItemsCount = mCartManager.getLineItemsShipping().size();
        mRegularItemIds = new ArrayList<>();
        mShippingItemIds = new ArrayList<>();
        // Because the CartManager guarantees the ordering of items in its maps,
        // this iteration will always have the same order.
        mRegularItemIds.addAll(mCartManager.getLineItemsRegular().keySet());
        mShippingItemIds.addAll(mCartManager.getLineItemsShipping().keySet());

        mCurrencyMarker = AndroidPayConfiguration.getInstance().getCurrency().getSymbol(Locale.US);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String key;
        LineItem item;
        switch (getItemViewType(position)) {
            case 0: // emoji line item
                key = mRegularItemIds.get(position);
                item = mCartManager.getLineItemsRegular().get(key);
                addLineItemToViewHolder(holder, item);
                break;
            case 1:
                key = mShippingItemIds.get(position - mRegularItemsCount);
                item = mCartManager.getLineItemsShipping().get(key);
                addLineItemToViewHolder(holder, item);
                break;
            case 2:
                addLineItemToViewHolder(holder, mCartManager.getLineItemTax());
                break;
            case 3:
                String totalPriceString = PaymentUtils.getPriceString(
                        mCartManager.getTotalPrice(),
                        AndroidPayConfiguration.getInstance().getCurrency());
                holder.setLabelText("Total:");
                holder.setTotalPriceText(mCurrencyMarker + totalPriceString);
                break;
        }
    }

    private void addLineItemToViewHolder(ViewHolder holder, LineItem item) {
        holder.setLabelText(item.getDescription());
        if (!TextUtils.isEmpty(item.getQuantity())) {
            holder.setQuantityText("X " + item.getQuantity() + " @");
        }

        if (!TextUtils.isEmpty(item.getUnitPrice())) {
            holder.setUnitPriceText(mCurrencyMarker + item.getUnitPrice());
        }

        holder.setTotalPriceText(mCurrencyMarker + item.getTotalPrice());
    }

    public CartManager getCartManager() {
        return mCartManager;
    }

    @Override
    public int getItemCount() {
        int taxBump = mHasTax ? 1 : 0;
        return mRegularItemsCount + mShippingItemsCount + taxBump + 1;
    }

    @Override
    public int getItemViewType(int position) {
        int taxBump = mHasTax ? 1 : 0;
        if (position < mRegularItemsCount) {
            return 0;
        } else if (position < mRegularItemsCount + mShippingItemsCount) {
            return 1;
        } else if (position < mRegularItemsCount + mShippingItemsCount + taxBump) {
            return 2;
        } else {
            return 3;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cart_item, parent, false);
        return new ViewHolder(view);
    }
}
