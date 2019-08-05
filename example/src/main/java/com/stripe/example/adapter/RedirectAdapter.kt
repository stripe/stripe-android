package com.stripe.example.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.stripe.android.model.Source
import com.stripe.example.R
import java.util.ArrayList

/**
 * A simple [RecyclerView] implementation to hold our data.
 */
// Provide a suitable constructor (depends on the kind of dataset)
class RedirectAdapter : RecyclerView.Adapter<RedirectAdapter.ViewHolder>() {
    private val data = ArrayList<ViewModel>()

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder(pollingLayout: View) : RecyclerView.ViewHolder(pollingLayout) {
        // each data item is just a string in this case
        private val finalStatusView: TextView = pollingLayout.findViewById(R.id.tv_ending_status)
        private val redirectStatusView: TextView = pollingLayout.findViewById(R.id.tv_redirect_status)
        private val sourceIdView: TextView = pollingLayout.findViewById(R.id.tv_source_id)
        private val sourceTypeView: TextView = pollingLayout.findViewById(R.id.tv_source_type)

        fun setFinalStatus(finalStatus: String?) {
            finalStatusView.text = finalStatus
        }

        fun setSourceId(sourceId: String?) {
            val last6 = if (sourceId == null || sourceId.length < 6)
                sourceId
            else
                sourceId.substring(sourceId.length - 6)
            sourceIdView.text = last6
        }

        fun setSourceType(sourceType: String?) {
            val viewableType: String? = if (Source.SourceType.THREE_D_SECURE == sourceType) {
                "3DS"
            } else {
                sourceType
            }
            sourceTypeView.text = viewableType
        }

        fun setRedirectStatus(redirectStatus: String?) {
            redirectStatusView.text = redirectStatus
        }
    }

    internal data class ViewModel constructor(
        val finalStatus: String?,
        val redirectStatus: String?,
        val sourceId: String?,
        val sourceType: String?
    )

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create a new view
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.polling_list_item, parent, false))
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val model = data[position]
        holder.setFinalStatus(model.finalStatus)
        holder.setRedirectStatus(model.redirectStatus)
        holder.setSourceId(model.sourceId)
        holder.setSourceType(model.sourceType)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return data.size
    }

    fun addItem(
        finalStatus: String?,
        redirectStatus: String?,
        sourceId: String?,
        sourceType: String?
    ) {
        data.add(ViewModel(finalStatus, redirectStatus, sourceId, sourceType))
        notifyDataSetChanged()
    }
}
