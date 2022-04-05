package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.stripe.android.identity.databinding.BaseErrorFragmentBinding

/**
 * Base error fragment displaying error messages and two buttons
 */
internal abstract class BaseErrorFragment : Fragment() {
    protected lateinit var title: TextView
    protected lateinit var message1: TextView
    protected lateinit var message2: TextView
    protected lateinit var topButton: MaterialButton
    protected lateinit var bottomButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = BaseErrorFragmentBinding.inflate(inflater, container, false)
        title = binding.titleText
        message1 = binding.message1
        message2 = binding.message2
        topButton = binding.topButton
        bottomButton = binding.bottomButton
        onCustomizingViews()
        return binding.root
    }

    protected abstract fun onCustomizingViews()
}
