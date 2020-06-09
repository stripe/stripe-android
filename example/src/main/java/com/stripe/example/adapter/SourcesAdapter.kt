package com.stripe.example.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.Source
import com.stripe.android.model.SourceTypeModel
import com.stripe.example.databinding.SourcesListItemBinding

internal class SourcesAdapter : RecyclerView.Adapter<SourcesAdapter.ViewHolder>() {
    private val sources: MutableList<Source> = mutableListOf()

    init {
        setHasStableIds(true)
        notifyDataSetChanged()
    }

    internal class ViewHolder(
        private val viewBinding: SourcesListItemBinding
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(source: Source) {
            viewBinding.status.text = source.status?.toString()
            viewBinding.redirectStatus.text = getRedirectStatus(source)
            viewBinding.sourceId.text = source.id?.let { sourceId ->
                sourceId.substring(sourceId.length - 6)
            }
            viewBinding.sourceType.text = if (Source.SourceType.THREE_D_SECURE == source.type) {
                "3DS"
            } else {
                source.type
            }
        }

        private fun getRedirectStatus(source: Source): String? {
            return source.redirect?.status?.toString()
                ?: (source.sourceTypeModel as SourceTypeModel.Card).threeDSecureStatus.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            SourcesListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
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
