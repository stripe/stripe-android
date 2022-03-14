package com.stripe.android.connections.example

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetResult
import com.stripe.android.connections.example.ConnectionsViewEffect.OpenConnectionsSheet
import com.stripe.android.connections.example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<ConnectionsViewModel>()

    private lateinit var connectionsSheet: ConnectionsSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(ActivityMainBinding.inflate(layoutInflater)) {
            setContentView(root)
            setupViews()
            observeViews()
            observeState()
        }
    }

    private fun ActivityMainBinding.setupViews() {
        setSupportActionBar(toolbar)
        connectionsSheet = ConnectionsSheet(
            activity = this@MainActivity,
            callback = viewModel::onConnectionsSheetResult
        )
    }

    private fun ActivityMainBinding.observeViews() {
        launchConnectionsSheet.setOnClickListener { viewModel.startLinkAccountSession() }
    }

    private fun ActivityMainBinding.observeState() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect {
                bindState(it, this@observeState)
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect {
                when (it) {
                    is OpenConnectionsSheet -> connectionsSheet.present(it.configuration)
                }
            }
        }
    }

    private fun bindState(
        connectionsState: ConnectionsState,
        viewBinding: ActivityMainBinding
    ) {

        viewBinding.status.text = connectionsState.status
        viewBinding.launchConnectionsSheet.isEnabled = connectionsState.loading.not()
    }
}
