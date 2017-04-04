package com.stripe.samplestore;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Currency;
import java.util.Locale;

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
        private Currency mCurrency;
        private TextView mEmojiTextView;
        private TextView mPriceTextView;

        ViewHolder(final LinearLayout pollingLayout, Currency currency) {
            super(pollingLayout);
            mEmojiTextView = (TextView) pollingLayout.findViewById(R.id.tv_emoji);
            mPriceTextView = (TextView) pollingLayout.findViewById(R.id.tv_price);
            mCurrency = currency;
            pollingLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    launchPurchaseActivityForIndex(v.getContext(), getAdapterPosition());
                }
            });
        }

        void setEmoji(int emojiUnicode) {
            mEmojiTextView.setText(getEmojiByUnicode(emojiUnicode));
        }

        void setPrice(int price) {
            mPriceTextView.setText(getPriceString(price, mCurrency));
        }
    }

    private Currency mCurrency;

    public StoreAdapter(String currencyCode) {
        try {
            mCurrency = Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException badCurrencyCode) {
            mCurrency = Currency.getInstance(Locale.US);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setEmoji(EMOJI_CLOTHES[position]);
        holder.setPrice(EMOJI_PRICES[position]);
    }

    @Override
    public int getItemCount() {
        return EMOJI_CLOTHES.length;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout pollingView = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.store_item, parent, false);

        ViewHolder vh = new ViewHolder(pollingView, mCurrency);
        return vh;
    }

    static void launchPurchaseActivityForIndex(Context context, int index) {
        // TODO: complete
    }

    static String getEmojiByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }

    static String getPriceString(int price, Currency currency) {
        int fractionDigits = currency.getDefaultFractionDigits();
        int totalLength = String.valueOf(price).length();
        StringBuilder builder = new StringBuilder();
        builder.append('\u00A4');
        builder.append(' ');

        if (fractionDigits == 0) {
            for (int i = 0; i < totalLength; i++) {
                builder.append('#');
            }
            DecimalFormat noDecimalCurrencyFormat = new DecimalFormat(builder.toString());
            noDecimalCurrencyFormat.setCurrency(currency);
            return noDecimalCurrencyFormat.format(price);
        }

        int beforeDecimal = totalLength - fractionDigits;
        for (int i = 0; i < beforeDecimal; i++) {
            builder.append('#');
        }
        // So we display "$0.55" instead of "$.55"
        if (totalLength <= fractionDigits) {
            builder.append('0');
        }
        builder.append('.');
        for (int i = 0; i < fractionDigits; i++) {
            builder.append('0');
        }
        double modBreak = Math.pow(10, fractionDigits);
        double decimalPrice = price / modBreak;

        DecimalFormat decimalFormat = new DecimalFormat(builder.toString());
        decimalFormat.setCurrency(currency);

        return decimalFormat.format(decimalPrice);
    }
}
