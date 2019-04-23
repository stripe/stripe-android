package com.stripe.example.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    @NonNull private final List<ViewModel> mDataset = new ArrayList<>();

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        @NonNull private final TextView mFinalStatusView;
        @NonNull private final TextView mRedirectStatusView;
        @NonNull private final TextView mSourceIdView;
        @NonNull private final TextView mSourceTypeView;

        ViewHolder(@NonNull final ViewGroup pollingLayout) {
            super(pollingLayout);
            mFinalStatusView = pollingLayout.findViewById(R.id.tv_ending_status);
            mRedirectStatusView = pollingLayout.findViewById(R.id.tv_redirect_status);
            mSourceIdView = pollingLayout.findViewById(R.id.tv_source_id);
            mSourceTypeView = pollingLayout.findViewById(R.id.tv_source_type);
        }

        void setFinalStatus(@Nullable String finalStatus) {
            mFinalStatusView.setText(finalStatus);
        }

        void setSourceId(@Nullable String sourceId) {
            final String last6 = sourceId == null || sourceId.length() < 6
                    ? sourceId
                    : sourceId.substring(sourceId.length() - 6);
            mSourceIdView.setText(last6);
        }

        void setSourceType(@Nullable String sourceType) {
            final String viewableType;
            if (Source.THREE_D_SECURE.equals(sourceType)) {
                viewableType = "3DS";
            } else {
                viewableType = sourceType;
            }
            mSourceTypeView.setText(viewableType);
        }

        void setRedirectStatus(@Nullable String redirectStatus) {
            mRedirectStatusView.setText(redirectStatus);
        }
    }

    private static class ViewModel {
        @Nullable private final String mFinalStatus;
        @Nullable private final String mRedirectStatus;
        @Nullable private final String mSourceId;
        @Nullable private final String mSourceType;

        private ViewModel(@Nullable String finalStatus, @Nullable String redirectStatus,
                          @Nullable String sourceId, @Nullable String sourceType) {
            mFinalStatus = finalStatus;
            mRedirectStatus = redirectStatus;
            mSourceId = sourceId;
            mSourceType = sourceType;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public RedirectAdapter() {}

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public RedirectAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                         int viewType) {
        // create a new view
        final ViewGroup pollingView = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.polling_list_item, parent, false);
        return new ViewHolder(pollingView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final ViewModel model = mDataset.get(position);
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

    public void addItem(@Nullable String finalStatus, @Nullable String redirectStatus,
                        @Nullable String sourceId, @Nullable String sourceType) {
        mDataset.add(new ViewModel(finalStatus, redirectStatus, sourceId, sourceType));
        notifyDataSetChanged();
    }
}
