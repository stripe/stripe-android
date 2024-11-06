package com.stripe.android.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.annotation.ColorInt
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
    0,
    ItemTouchHelper.RIGHT
) {
    private val trashIcon =
        ContextCompat.getDrawable(context, R.drawable.stripe_ic_trash)!!
    private val swipeStartColor =
        ContextCompat.getColor(context, R.color.stripe_swipe_start_payment_method)
    private val swipeThresholdColor =
        ContextCompat.getColor(context, R.color.stripe_swipe_threshold_payment_method)
    private val background = ColorDrawable(swipeStartColor)
    private val itemViewStartPadding = trashIcon.intrinsicWidth / 2
    private val iconStartOffset = context.resources.getDimensionPixelSize(R.dimen.stripe_list_row_start_padding)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val paymentMethod = adapter.getPaymentMethodAtPosition(viewHolder.bindingAdapterPosition)
        listener.onSwiped(paymentMethod)
    }

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return if (viewHolder is PaymentMethodsAdapter.ViewHolder.PaymentMethodViewHolder) {
            // only allow swiping on Payment Method items
            super.getSwipeDirs(recyclerView, viewHolder)
        } else {
            0
        }
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        if (viewHolder is PaymentMethodsAdapter.ViewHolder.PaymentMethodViewHolder) {
            val itemView = viewHolder.itemView

            val startTransition = itemView.width * START_TRANSITION_THRESHOLD
            val endTransition = itemView.width * END_TRANSITION_THRESHOLD

            // calculate the transition fraction to animate the background color of the swipe
            val transitionFraction: Float =
                when {
                    dX < startTransition ->
                        0F
                    dX >= endTransition ->
                        1F
                    else ->
                        ((dX - startTransition) / (endTransition - startTransition))
                }

            updateSwipedPaymentMethod(
                itemView,
                dX.toInt(),
                transitionFraction,
                canvas
            )
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return END_TRANSITION_THRESHOLD
    }

    private fun updateSwipedPaymentMethod(
        itemView: View,
        dX: Int,
        transitionFraction: Float,
        canvas: Canvas
    ) {
        val iconTop = itemView.top + (itemView.height - trashIcon.intrinsicHeight) / 2
        val iconBottom = iconTop + trashIcon.intrinsicHeight

        when {
            // swipe right
            dX > 0 -> {
                val iconLeft = itemView.left + iconStartOffset
                val iconRight = iconLeft + trashIcon.intrinsicWidth

                // hide the icon until the swipe distance is enough that it won't clash
                // with the view
                if (dX > iconRight) {
                    trashIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                } else {
                    trashIcon.setBounds(0, 0, 0, 0)
                }

                background.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX + itemViewStartPadding,
                    itemView.bottom
                )
                background.color = when {
                    transitionFraction <= 0.0F ->
                        swipeStartColor
                    transitionFraction >= 1.0F ->
                        swipeThresholdColor
                    else ->
                        calculateTransitionColor(
                            transitionFraction,
                            swipeStartColor,
                            swipeThresholdColor
                        )
                }
            }
            else -> {
                // reset when done swiping
                trashIcon.setBounds(0, 0, 0, 0)
                background.setBounds(0, 0, 0, 0)
            }
        }

        background.draw(canvas)
        trashIcon.draw(canvas)
    }

    internal companion object {
        // calculate the background color while transitioning from start to end threshold
        internal fun calculateTransitionColor(
            fraction: Float,
            @ColorInt startValue: Int,
            @ColorInt endValue: Int
        ): Int {
            val startAlpha = Color.alpha(startValue)
            val startRed = Color.red(startValue)
            val startGreen = Color.green(startValue)
            val startBlue = Color.blue(startValue)
            val deltaAlpha = (Color.alpha(endValue) - startAlpha) * fraction
            val deltaRed = (Color.red(endValue) - startRed) * fraction
            val deltaGreen = (Color.green(endValue) - startGreen) * fraction
            val deltaBlue = (Color.blue(endValue) - startBlue) * fraction
            return Color.argb(
                (startAlpha + deltaAlpha).toInt(),
                (startRed + deltaRed).toInt(),
                (startGreen + deltaGreen).toInt(),
                (startBlue + deltaBlue).toInt()
            )
        }

        private const val START_TRANSITION_THRESHOLD = 0.25F
        private const val END_TRANSITION_THRESHOLD = 0.5F
    }

    interface Listener {
        fun onSwiped(paymentMethod: PaymentMethod)
    }
}
