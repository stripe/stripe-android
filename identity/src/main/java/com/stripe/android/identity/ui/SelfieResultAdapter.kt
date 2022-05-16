package com.stripe.android.identity.ui

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.identity.databinding.SelfieItemBinding

internal class SelfieResultAdapter :
    ListAdapter<Bitmap, SelfieViewHolder>(SelfieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelfieViewHolder {
        return SelfieViewHolder(SelfieItemBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: SelfieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

internal class SelfieViewHolder(private val selfieItemBinding: SelfieItemBinding) :
    RecyclerView.ViewHolder(selfieItemBinding.root) {
    fun bind(bm: Bitmap) {
        selfieItemBinding.selfieItem.setImageBitmap(bm)
    }
}

internal class SelfieDiffCallback : DiffUtil.ItemCallback<Bitmap>() {
    override fun areItemsTheSame(oldItem: Bitmap, newItem: Bitmap): Boolean {
        return oldItem.sameAs(newItem)
    }

    override fun areContentsTheSame(oldItem: Bitmap, newItem: Bitmap): Boolean {
        return oldItem.sameAs(newItem)
    }
}
