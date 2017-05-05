package com.stripe.example.controller;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.stripe.android.model.Token;
import com.stripe.example.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A controller for the {@link ListView} used to display the results.
 */
public class ListViewController {

    private SimpleAdapter mAdatper;
    private List<Map<String, String>> mCardTokens = new ArrayList<Map<String, String>>();
    private Context mContext;

    public ListViewController(ListView listView) {
        mContext = listView.getContext();
        mAdatper = new SimpleAdapter(
                mContext,
                mCardTokens,
                R.layout.list_item_layout,
                new String[]{"last4", "tokenId"},
                new int[]{R.id.last4, R.id.tokenId});
        listView.setAdapter(mAdatper);
    }

    void addToList(Token token) {
        addToList(token.getCard().getLast4(), token.getId());
    }

    public void addToList(@NonNull String last4, @NonNull String tokenId) {
        String endingIn = mContext.getString(R.string.endingIn);
        Map<String, String> map = new HashMap<>();
        map.put("last4", endingIn + " " + last4);
        map.put("tokenId", tokenId);
        mCardTokens.add(map);
        mAdatper.notifyDataSetChanged();
    }
}
