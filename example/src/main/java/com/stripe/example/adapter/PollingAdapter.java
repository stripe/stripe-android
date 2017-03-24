package com.stripe.example.adapter;

import android.support.v4.text.TextUtilsCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stripe.example.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link RecyclerView} implementation to hold our data.
 */
public class PollingAdapter extends RecyclerView.Adapter<PollingAdapter.ViewHolder> {
    private List<ViewModel> mDataset = new ArrayList<>();

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        private TextView mFinalStatusView;
        private TextView mSourceIdView;
        ViewHolder(LinearLayout pollingLayout) {
            super(pollingLayout);
            mFinalStatusView = (TextView) pollingLayout.findViewById(R.id.tv_ending_status);
            mSourceIdView = (TextView) pollingLayout.findViewById(R.id.tv_source_id);
        }

        public void setFinalStatus(String finalStatus) {
            mFinalStatusView.setText(finalStatus);
        }

        public void setSourceId(String sourceId) {
            mSourceIdView.setText(sourceId);
        }
    }

    public static class ViewModel {
        public final String mFinalStatus;
        public final String mSourceId;

        public ViewModel(String finalStatus, String sourceId) {
            mFinalStatus = finalStatus;
            mSourceId = sourceId;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public PollingAdapter() {}

    // Create new views (invoked by the layout manager)
    @Override
    public PollingAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view

        LinearLayout pollingView = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.polling_list_item, parent, false);

        //
        ViewHolder vh = new ViewHolder(pollingView);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        ViewModel model = mDataset.get(position);
        holder.setFinalStatus(model.mFinalStatus);
        holder.setSourceId(model.mSourceId);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void addItem(String finalStatus, String sourceId) {
        mDataset.add(new ViewModel(finalStatus, sourceId));
        notifyDataSetChanged();
    }
}
