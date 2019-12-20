package com.stripe.example.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.example.R
import kotlinx.android.synthetic.main.activity_launcher.*

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        val linearLayoutManager = LinearLayoutManager(this)
            .apply {
                orientation = LinearLayoutManager.VERTICAL
            }

        examples.run {
            setHasFixedSize(true)
            layoutManager = linearLayoutManager
            adapter = ExamplesAdapter(this@LauncherActivity)
        }
    }

    private class ExamplesAdapter constructor(
        private val activity: Activity
    ) : RecyclerView.Adapter<ExamplesAdapter.ExamplesViewHolder>() {
        private val items = listOf(
            Item(activity.getString(R.string.payment_auth_example),
                PaymentAuthActivity::class.java),
            Item(activity.getString(R.string.create_card_token),
                CreateCardTokenActivity::class.java),
            Item(activity.getString(R.string.create_card_payment_method),
                CreateCardPaymentMethodActivity::class.java),
            Item(activity.getString(R.string.create_card_source),
                CreateCardSourceActivity::class.java),
            Item(activity.getString(R.string.launch_customer_session),
                CustomerSessionActivity::class.java),
            Item(activity.getString(R.string.launch_payment_session),
                PaymentSessionActivity::class.java),
            Item(activity.getString(R.string.launch_payment_session_from_fragment),
                FragmentExamplesActivity::class.java),
            Item(activity.getString(R.string.launch_pay_with_google),
                PayWithGoogleActivity::class.java),
            Item(activity.getString(R.string.launch_create_pm_sepa_debit),
                CreateSepaDebitActivity::class.java),
            Item(activity.getString(R.string.fpx_payment_example),
                FpxPaymentActivity::class.java),
            Item(activity.getString(R.string.klarna_source_example),
                KlarnaSourceActivity::class.java),
            Item(activity.getString(R.string.card_brands),
                CardBrandsActivity::class.java)
        )

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ExamplesViewHolder {
            val root = activity.layoutInflater
                .inflate(R.layout.launcher_item, viewGroup, false)
            return ExamplesViewHolder(root)
        }

        override fun onBindViewHolder(examplesViewHolder: ExamplesViewHolder, i: Int) {
            val itemView = examplesViewHolder.itemView
            (itemView as TextView).text = items[i].text
            itemView.setOnClickListener {
                activity.startActivity(Intent(activity, items[i].activityClass))
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        private data class Item constructor(val text: String, val activityClass: Class<*>)

        private class ExamplesViewHolder constructor(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView)
    }
}
