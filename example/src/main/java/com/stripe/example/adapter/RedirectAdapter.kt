package com.stripe.example.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

import com.stripe.android.model.Source
import com.stripe.example.R

import java.util.ArrayList

/**
 * A simple [RecyclerView] implementation to hold our data.
 */
// Provide a suitable constructor (depends on the kind of dataset)
class RedirectAdapter : RecyclerView.Adapter<RedirectAdapter.ViewHolder>() {
    private val mDataset = ArrayList<ViewModel>()

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder(pollingLayout: ViewGroup) : RecyclerView.ViewHolder(pollingLayout) {
        // each data item is just a string in this case
        private val mFinalStatusView: TextView = pollingLayout.findViewById(R.id.tv_ending_status)
        private val mRedirectStatusView: TextView = pollingLayout.findViewById(R.id.tv_redirect_status)
        private val mSourceIdView: TextView = pollingLayout.findViewById(R.id.tv_source_id)
        private val mSourceTypeView: TextView = pollingLayout.findViewById(R.id.tv_source_type)

        fun setFinalStatus(finalStatus: String?) {
            mFinalStatusView.text = finalStatus
        }

        fun setSourceId(sourceId: String?) {
            val last6 = if (sourceId == null || sourceId.length < 6)
                sourceId
            else
                sourceId.substring(sourceId.length - 6)
            mSourceIdView.text = last6
        }

        fun setSourceType(sourceType: String?) {
            val viewableType: String? = if (Source.SourceType.THREE_D_SECURE == sourceType) {
                "3DS"
            } else {
                sourceType
            }
            mSourceTypeView.text = viewableType
        }

        fun setRedirectStatus(redirectStatus: String?) {
            mRedirectStatusView.text = redirectStatus
        }
    }

    internal data class ViewModel constructor(
        val mFinalStatus: String?,
        val mRedirectStatus: String?,
        val mSourceId: String?,
        val mSourceType: String?
    )

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create a new view
        val pollingView = LayoutInflater.from(parent.context)
            .inflate(R.layout.polling_list_item, parent, false) as LinearLayout
        return ViewHolder(pollingView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val model = mDataset[position]
        holder.setFinalStatus(model.mFinalStatus)
        holder.setRedirectStatus(model.mRedirectStatus)
        holder.setSourceId(model.mSourceId)
        holder.setSourceType(model.mSourceType)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return mDataset.size
    }

    fun addItem(
        finalStatus: String?,
        redirectStatus: String?,
        sourceId: String?,
        sourceType: String?
    ) {
        mDataset.add(ViewModel(finalStatus, redirectStatus, sourceId, sourceType))
        notifyDataSetChanged()
    }
}
