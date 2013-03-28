package com.stripe.example.fragment;

import android.support.v4.app.ListFragment;
import android.widget.SimpleAdapter;
import com.stripe.example.TokenList;
import com.stripe.example.R;
import com.stripe.android.model.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenListFragment extends ListFragment implements TokenList {
    private List<Map<String, String>> mListItems = new ArrayList<Map<String, String>>();
    private SimpleAdapter mAdapter;

    @Override
    public void onViewCreated(android.view.View view, android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new SimpleAdapter(getActivity(),
                mListItems,
                R.layout.list_item_layout,
                new String[]{ "last4", "tokenId" },
                new int[]{ R.id.last4, R.id.tokenId });
        setListAdapter(mAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setListAdapter(null);
    }

    @Override
    public void addToList(Token token) {
        String endingIn = getResources().getString(R.string.endingIn);
        Map<String, String> map = new HashMap<String, String>();
        map.put("last4", endingIn + " " + token.getCard().getLast4());
        map.put("tokenId", token.getId());
        mListItems.add(map);
        mAdapter.notifyDataSetChanged();
    }
}