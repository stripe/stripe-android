package com.stripe.android.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.R
import kotlinx.android.synthetic.main.activity_stripe.*

/**
 * Provides a toolbar, save button, and loading states for the save button.
 */
abstract class StripeActivity : AppCompatActivity() {

    internal val progressBar: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.progress_bar)
    }

    internal val viewStub: ViewStub by lazy {
        findViewById<ViewStub>(R.id.widget_viewstub_as)
    }

    protected var isProgressBarVisible: Boolean = false
        set(value) {
            progressBar.visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
            invalidateOptionsMenu()

            onProgressBarVisibilityChanged(value)

            field = value
        }

    private val alertDisplayer: AlertDisplayer by lazy {
        AlertDisplayer.DefaultAlertDisplayer(this)
    }

    private val stripeColorUtils: StripeColorUtils by lazy {
        StripeColorUtils(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stripe)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isProgressBarVisible = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_payment_method, menu)
        menu.findItem(R.id.action_save).isEnabled = !isProgressBarVisible
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_save) {
            onActionSave()
            true
        } else {
            val handled = super.onOptionsItemSelected(item)
            if (!handled) {
                onBackPressed()
            }
            handled
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val saveItem = menu.findItem(R.id.action_save)
        val tintedIcon = stripeColorUtils.getTintedIconWithAttribute(
            theme,
            R.attr.titleTextColor,
            R.drawable.stripe_ic_checkmark
        )
        saveItem.icon = tintedIcon
        return super.onPrepareOptionsMenu(menu)
    }

    protected abstract fun onActionSave()

    protected open fun onProgressBarVisibilityChanged(visible: Boolean) {
    }

    protected fun showError(error: String) {
        alertDisplayer.show(error)
    }
}
