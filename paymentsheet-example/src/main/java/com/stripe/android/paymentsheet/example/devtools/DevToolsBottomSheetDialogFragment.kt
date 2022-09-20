package com.stripe.android.paymentsheet.example.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.LeadingIconTab
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.android.core.networking.StripeNetworkClientInterceptor
import com.stripe.android.paymentsheet.PaymentSheetFeatures
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

private enum class DevToolsTab {
    Network,
    FeatureFlags;
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
                text = { Text(tab.name) },
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
