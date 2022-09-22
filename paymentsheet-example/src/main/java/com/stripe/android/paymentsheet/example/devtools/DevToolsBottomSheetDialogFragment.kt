package com.stripe.android.paymentsheet.example.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LeadingIconTab
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class DevToolsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DevTools()
            }
        }
    }

    companion object {
        fun newInstance() = DevToolsBottomSheetDialogFragment()
    }
}

private enum class DevToolsTab(val title: String) {
    Network(title = "Failing endpoints"),
    FeatureFlags(title = "Feature flags");
}

@Composable
private fun DevToolsTab.Content() {
    when (this) {
        DevToolsTab.Network -> DevToolsNetwork()
        DevToolsTab.FeatureFlags -> DevToolsFeatureFlags()
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun DevTools() {
    val tabs = DevToolsTab.values()
    val pagerState = rememberPagerState()

    Scaffold(
        topBar = {
            Surface(elevation = 4.dp) {
                Column {
                    TopAppBar(
                        title = { Text("DevTools") },
                        elevation = 0.dp
                    )
                    DevToolsTabRow(
                        tabs = tabs,
                        pagerState = pagerState
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            count = tabs.size,
            modifier = Modifier.padding(padding)
        ) { page ->
            tabs[page].Content()
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun DevToolsTabRow(
    tabs: Array<DevToolsTab>,
    pagerState: PagerState,
) {
    val scope = rememberCoroutineScope()
    TabRow(
        selectedTabIndex = pagerState.currentPage
    ) {
        for ((index, tab) in tabs.withIndex()) {
            LeadingIconTab(
                text = { Text(tab.title) },
                icon = {},
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    }
}
