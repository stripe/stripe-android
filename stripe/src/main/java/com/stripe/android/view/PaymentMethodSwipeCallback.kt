package com.stripe.android.view

import android.content.Context
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod

/**
 * A [ItemTouchHelper.SimpleCallback] subclass for enabling swiping on Payment Methods in
 * [PaymentMethodsActivity]'s list
 */
internal class PaymentMethodSwipeCallback(
    context: Context,
    private val adapter: PaymentMethodsAdapter,
    private val listener: Listener
) : ItemTouchHelper.SimpleCallback(
    0, ItemTouchHelper.RIGHT
) {
    private val trashIcon = ContextCompat.getDrawable(context, R.drawable.ic_trash)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val paymentMethod = adapter.paymentMethods[viewHolder.adapterPosition]
        listener.onSwiped(paymentMethod)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        viewHolder.itemView
    }

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return if (viewHolder is PaymentMethodsAdapter.PaymentMethodViewHolder) {
            // only allow swiping on Payment Method items
            super.getSwipeDirs(recyclerView, viewHolder)
        } else {
            0
        }
    }

    interface Listener {
        fun onSwiped(paymentMethod: PaymentMethod)
    }
}
