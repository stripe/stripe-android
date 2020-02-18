package com.stripe.example.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.Source
import com.stripe.android.model.SourceTypeModel
import com.stripe.example.R

internal class SourcesAdapter : RecyclerView.Adapter<SourcesAdapter.ViewHolder>() {
    private val sources: MutableList<Source> = mutableListOf()

    init {
        setHasStableIds(true)
        notifyDataSetChanged()
    }

    internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusView: TextView = itemView.findViewById(R.id.tv_ending_status)
        private val redirectStatusView: TextView = itemView.findViewById(R.id.tv_redirect_status)
        private val idView: TextView = itemView.findViewById(R.id.tv_source_id)
        private val typeView: TextView = itemView.findViewById(R.id.tv_source_type)

        fun bind(source: Source) {
            statusView.text = source.status
            redirectStatusView.text = getRedirectStatus(source)
            idView.text = source.id?.let { sourceId ->
                sourceId.substring(sourceId.length - 6)
            }

            typeView.text = if (Source.SourceType.THREE_D_SECURE == source.type) {
                "3DS"
            } else {
                source.type
            }
        }

        private fun getRedirectStatus(source: Source): String? {
            return source.redirect?.status
                ?: (source.sourceTypeModel as SourceTypeModel.Card).threeDSecureStatus
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.sources_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sources[position])
    }

    override fun getItemCount(): Int {
        return sources.size
    }

    override fun getItemId(position: Int): Long {
        return sources[position].id.orEmpty().hashCode().toLong()
    }

    fun addSource(source: Source) {
        sources.add(0, source)
        notifyItemInserted(0)
    }
}
