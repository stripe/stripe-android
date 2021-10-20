package com.stripe.android.cardverificationsheet.cardverifyui.analyzer

import com.stripe.android.cardverificationsheet.cardverifyui.SavedFrame
import com.stripe.android.cardverificationsheet.framework.Analyzer
import com.stripe.android.cardverificationsheet.framework.AnalyzerFactory
import kotlinx.coroutines.supervisorScope

internal class CompletionLoopAnalyzer private constructor() :
    Analyzer<Collection<SavedFrame>, Unit, Unit> {

    override suspend fun analyze(
        data: Collection<SavedFrame>,
        state: Unit,
    ): Unit = supervisorScope {
        // TODO: make a network call for verify
    }

    class Factory() : AnalyzerFactory<Collection<SavedFrame>, Unit, Unit, CompletionLoopAnalyzer> {
        override suspend fun newInstance() = CompletionLoopAnalyzer()
    }
}
