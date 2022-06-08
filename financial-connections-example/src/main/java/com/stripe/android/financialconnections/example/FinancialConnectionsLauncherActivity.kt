package com.stripe.android.financialconnections.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.financialconnections.example.databinding.ActivityFinancialconnectionsLauncherBinding

class FinancialConnectionsLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = ActivityFinancialconnectionsLauncherBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val linearLayoutManager = LinearLayoutManager(this)
            .apply {
                orientation = LinearLayoutManager.VERTICAL
            }

        viewBinding.examples.run {
            setHasFixedSize(true)
            addItemDecoration(
                DividerItemDecoration(
                    this@FinancialConnectionsLauncherActivity,
                    linearLayoutManager.orientation
                )
            )
            layoutManager = linearLayoutManager
            adapter = ExamplesAdapter(this@FinancialConnectionsLauncherActivity)
        }
    }

    private class ExamplesAdapter constructor(
        private val activity: Activity
    ) : RecyclerView.Adapter<ExamplesAdapter.ExamplesViewHolder>() {
        private val items = listOf(
            Item(
                activity.getString(R.string.collect_bank_account_for_data),
                FinancialConnectionsDataExampleActivity::class.java
            ),
            Item(
                activity.getString(R.string.collect_bank_account_for_bank_account_token),
                FinancialConnectionsBankAccountTokenExampleActivity::class.java
            ),
            Item(
                activity.getString(R.string.collect_bank_account_for_data_compose),
                FinancialConnectionsComposeExampleActivity::class.java
            ),
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
