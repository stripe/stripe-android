package com.stripe.android.googlepaylauncher.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

internal enum class LauncherIntegrationType {
    Activity,
    Compose,
    BuilderActivity,
    BuilderFragment,
    BuilderCompose,
}

internal class GooglePayTestFragment(
    private val initializeInOnViewCreated: Boolean,
    private val createLauncher: (Fragment) -> Unit,
) : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!initializeInOnViewCreated) {
            createLauncher(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = View(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (initializeInOnViewCreated) {
            createLauncher(this)
        }
    }
}
