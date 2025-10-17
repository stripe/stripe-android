package com.stripe.android.attestation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

internal class AttestationActivity : AppCompatActivity() {
    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = AttestationViewModel.Factory

    private val viewModel by viewModels<AttestationViewModel> {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            viewModel.result.collect { result ->
                dismissWithResult(result)
            }
        }
    }

    private fun dismissWithResult(result: AttestationActivityResult) {
        val bundle = bundleOf(
            AttestationActivityContract.EXTRA_RESULT to result
        )
        setResult(RESULT_COMPLETE, Intent().putExtras(bundle))
        finish()
    }

    companion object {
        internal const val EXTRA_ARGS = "attestation_args"
        internal const val RESULT_COMPLETE = 4851

        internal fun createIntent(
            context: Context,
            args: AttestationArgs
        ): Intent {
            return Intent(context, AttestationActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }

        internal fun getArgs(savedStateHandle: SavedStateHandle): AttestationArgs? {
            return savedStateHandle.get<AttestationArgs>(EXTRA_ARGS)
        }
    }
}
