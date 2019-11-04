package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.PaymentMethod

internal class PaymentMethodsRecyclerView @JvmOverloads internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    internal var listener: Listener? = null
    internal var tappedPaymentMethod: PaymentMethod? = null

    init {
        setHasFixedSize(false)
        layoutManager = LinearLayoutManager(context)

        itemAnimator = object : DefaultItemAnimator() {
            override fun onAnimationFinished(viewHolder: ViewHolder) {
                super.onAnimationFinished(viewHolder)

                // wait until post-tap animations are completed before finishing activity
                tappedPaymentMethod?.let { listener?.onPaymentMethodSelected(it) }
                tappedPaymentMethod = null
            }
        }
    }

    @JvmSynthetic
    internal fun attachItemTouchHelper(callback: ItemTouchHelper.SimpleCallback) {
        ItemTouchHelper(callback).attachToRecyclerView(this)
    }

    internal interface Listener {
        fun onPaymentMethodSelected(paymentMethod: PaymentMethod)
    }
}
