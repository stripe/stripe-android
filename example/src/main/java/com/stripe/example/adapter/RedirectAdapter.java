package com.stripe.example.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stripe.android.model.Source;
import com.stripe.example.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link RecyclerView} implementation to hold our data.
 */
public class RedirectAdapter extends RecyclerView.Adapter<RedirectAdapter.ViewHolder> {
    private List<ViewModel> mDataset = new ArrayList<>();

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        private TextView mFinalStatusView;
        private TextView mRedirectStatusView;
        private TextView mSourceIdView;
        private TextView mSourceTypeView;
        ViewHolder(LinearLayout pollingLayout) {
            super(pollingLayout);
            mFinalStatusView = (TextView) pollingLayout.findViewById(R.id.tv_ending_status);
            mRedirectStatusView = (TextView) pollingLayout.findViewById(R.id.tv_redirect_status);
            mSourceIdView = (TextView) pollingLayout.findViewById(R.id.tv_source_id);
            mSourceTypeView = (TextView) pollingLayout.findViewById(R.id.tv_source_type);
        }

        public void setFinalStatus(String finalStatus) {
            mFinalStatusView.setText(finalStatus);
        }

        public void setSourceId(String sourceId) {
            String last6 = sourceId == null || sourceId.length() < 6
                    ? sourceId
                    : sourceId.substring(sourceId.length() - 6);
            mSourceIdView.setText(last6);
        }

        public void setSourceType(String sourceType) {
            String viewableType = sourceType;
            if (Source.THREE_D_SECURE.equals(sourceType)) {
                viewableType = "3DS";
            }
            mSourceTypeView.setText(viewableType);
        }

        public void setRedirectStatus(String redirectStatus) {
            mRedirectStatusView.setText(redirectStatus);
        }
    }

    public static class ViewModel {
        public final String mFinalStatus;
        public final String mRedirectStatus;
        public final String mSourceId;
        public final String mSourceType;

        public ViewModel(String finalStatus, String redirectStatus, String sourceId, String sourceType) {
            mFinalStatus = finalStatus;
            mRedirectStatus = redirectStatus;
            mSourceId = sourceId;
            mSourceType = sourceType;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RedirectAdapter() {}

    // Create new views (invoked by the layout manager)
    @Override
    public RedirectAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
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
        holder.setRedirectStatus(model.mRedirectStatus);
        holder.setSourceId(model.mSourceId);
        holder.setSourceType(model.mSourceType);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void addItem(String finalStatus, String redirectStatus, String sourceId, String sourceType) {
        mDataset.add(new ViewModel(finalStatus, redirectStatus, sourceId, sourceType));
        notifyDataSetChanged();
    }
}
