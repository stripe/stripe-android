package com.stripe.android.financialconnections.lite

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs

class FinancialConnectionsSheetLiteActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // render some loading state
        // Create a TextView instance
        val textView = TextView(this).apply {
            text = "Hello, Programmatic UI!"
            textSize = 24f
            setTextColor(Color.BLACK)

            // Center the text within the TextView
            gravity = Gravity.CENTER

            // Set the layout parameters for the TextView
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        }

        // Set the TextView as the content view
        setContentView(textView)
    }

    companion object {

        private const val EXTRA_ARGS = "FinancialConnectionsSheetActivityArgs"
        fun intent(context: Context, args: FinancialConnectionsSheetActivityArgs): Intent {
            return Intent(context, FinancialConnectionsSheetLiteActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(EXTRA_ARGS, args)
            }
        }

        fun getArgs(savedStateHandle: SavedStateHandle): FinancialConnectionsSheetActivityArgs? {
            return savedStateHandle.get<FinancialConnectionsSheetActivityArgs>(EXTRA_ARGS)
        }

        fun getArgs(intent: Intent): FinancialConnectionsSheetActivityArgs? {
            return intent.getParcelableExtra(EXTRA_ARGS)
        }
    }
}
