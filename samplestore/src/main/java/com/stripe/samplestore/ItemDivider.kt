package com.stripe.samplestore

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Custom divider will be used in the list.
 */
internal class ItemDivider(
    context: Context,
    @DrawableRes resId: Int
) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

    private val divider: Drawable = ContextCompat.getDrawable(context, resId)!!

    override fun onDraw(c: Canvas, parent: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State) {
        val start = parent.paddingStart
        val end = parent.width - parent.paddingEnd

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as androidx.recyclerview.widget.RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight

            divider.setBounds(start, top, end, bottom)
            divider.draw(c)
        }
    }
}
