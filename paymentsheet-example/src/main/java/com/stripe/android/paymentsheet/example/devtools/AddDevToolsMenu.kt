package com.stripe.android.paymentsheet.example.devtools

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import com.stripe.android.paymentsheet.example.R

fun AppCompatActivity.addDevToolsMenu() {
    addMenuProvider(
        object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_dev_tools -> {
                        val devTools = DevToolsBottomSheetDialogFragment.newInstance()
                        devTools.show(supportFragmentManager, devTools.tag)
                        true
                    }
                    else -> true
                }
            }
        }
    )
}
