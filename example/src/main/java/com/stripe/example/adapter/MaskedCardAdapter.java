package com.stripe.example.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.stripe.android.model.Card;
import com.stripe.android.view.MaskedCardView;
import com.stripe.example.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mrmcduff on 8/4/17.
 */

public class MaskedCardAdapter extends RecyclerView.Adapter<MaskedCardAdapter.ViewHolder> {


    static final String VISA_CARD = "{\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": \"Des Moines\",\n" +
            "    \"address_country\": \"US\",\n" +
            "    \"address_line1\": \"123 Any Street\",\n" +
            "    \"address_line1_check\": \"unavailable\",\n" +
            "    \"address_line2\": \"456\",\n" +
            "    \"address_state\": \"IA\",\n" +
            "    \"address_zip\": \"50305\",\n" +
            "    \"address_zip_check\": \"unavailable\",\n" +
            "    \"brand\": \"Visa\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"currency\": \"usd\",\n" +
            "    \"customer\": \"customer77\",\n" +
            "    \"cvc_check\": \"unavailable\",\n" +
            "    \"exp_month\": 8,\n" +
            "    \"exp_year\": 2017,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"fingerprint\": \"abc123\",\n" +
            "    \"last4\": \"4242\",\n" +
            "    \"name\": \"John Cardholder\"\n" +
            "  }";

    static final String MASTERCARD_CARD = "{\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": \"Des Moines\",\n" +
            "    \"address_country\": \"US\",\n" +
            "    \"address_line1\": \"123 Any Street\",\n" +
            "    \"address_line1_check\": \"unavailable\",\n" +
            "    \"address_line2\": \"456\",\n" +
            "    \"address_state\": \"IA\",\n" +
            "    \"address_zip\": \"50305\",\n" +
            "    \"address_zip_check\": \"unavailable\",\n" +
            "    \"brand\": \"Mastercard\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"currency\": \"usd\",\n" +
            "    \"customer\": \"customer77\",\n" +
            "    \"cvc_check\": \"unavailable\",\n" +
            "    \"exp_month\": 8,\n" +
            "    \"exp_year\": 2017,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"fingerprint\": \"abc123\",\n" +
            "    \"last4\": \"4444\",\n" +
            "    \"name\": \"John Cardholder\"\n" +
            "  }";

    static final String DISCOVER_CARD = "{\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": \"Des Moines\",\n" +
            "    \"address_country\": \"US\",\n" +
            "    \"address_line1\": \"123 Any Street\",\n" +
            "    \"address_line1_check\": \"unavailable\",\n" +
            "    \"address_line2\": \"456\",\n" +
            "    \"address_state\": \"IA\",\n" +
            "    \"address_zip\": \"50305\",\n" +
            "    \"address_zip_check\": \"unavailable\",\n" +
            "    \"brand\": \"Discover\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"currency\": \"usd\",\n" +
            "    \"customer\": \"customer77\",\n" +
            "    \"cvc_check\": \"unavailable\",\n" +
            "    \"exp_month\": 8,\n" +
            "    \"exp_year\": 2017,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"fingerprint\": \"abc123\",\n" +
            "    \"last4\": \"3333\",\n" +
            "    \"name\": \"John Cardholder\"\n" +
            "  }";

    static final Card VISA_OBJ = Card.fromString(VISA_CARD);
    static final Card MASTERCARD_OBJ = Card.fromString(MASTERCARD_CARD);
    static final Card DISCOVER_OBJ = Card.fromString(DISCOVER_CARD);

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaskedCardView mMaskedCardView;
        ViewHolder(FrameLayout itemLayout) {
            super(itemLayout);
            mMaskedCardView = itemLayout.findViewById(R.id.mcv);
            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mMaskedCardView.toggleSelected();
                }
            });
        }

        void setMaskedCard(@NonNull Card card) {
            mMaskedCardView.setCard(card);
        }

        void setIsSelected(boolean isSelected) {
            mMaskedCardView.setIsSelected(isSelected);
        }
    }

    private List<Card> mCardList;

    public MaskedCardAdapter() {
        mCardList = new ArrayList<>();
    }

    public void load() {
        mCardList.add(VISA_OBJ);
        mCardList.add(MASTERCARD_OBJ);
        mCardList.add(DISCOVER_OBJ);
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Card card = mCardList.get(position);
        holder.setMaskedCard(card);
        holder.setIsSelected(position % 2 == 1);
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
}
