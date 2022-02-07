package com.stripe.android.identity.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.stripe.android.identity.R

class NavHostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_host)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.identity_nav_host) as NavHostFragment
        val navController = navHostFragment.navController

    }
}