package com.stripe.android.paymentsheet.example.devtools

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import com.stripe.android.paymentsheet.example.R

fun AppCompatActivity.addDevToolsMenu() {
    val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_dev_tools -> {
                    openDevTools()
                    true
                }
                else -> true
            }
        }
    }

    addMenuProvider(menuProvider)
}

private fun AppCompatActivity.openDevTools() {
    if (DevToolsStore.failed) {
        Toast.makeText(
            this,
            "Failed to load DevTools. Check the logs.",
            Toast.LENGTH_LONG
        ).show()
    } else {
        val devTools = DevToolsBottomSheetDialogFragment.newInstance()
        devTools.show(supportFragmentManager, devTools.tag)
    }
}
