package com.stripe.android.identity.states

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.states.IdentityScanState.Found
import com.stripe.android.identity.states.IdentityScanState.Satisfied
import com.stripe.android.identity.states.IdentityScanState.Unsatisfied

/**
 * Keep a look back window of [frameRequired] number of frames, decide transition when the window is full.
 *
 * Stay in [Found] state before required number of frames is processed.
 * When required number of frames is processed,
 *   If equal or more than [hitsRequired] number of frames matches required type, transition to [Satisfied].
 *   If less than [hitsRequired] number of frames matches required type, transition to [Unsatisfied].
 */
internal class LookBackWindowTransitioner(
    private val frameRequired: Int = DEFAULT_FRAMES_REQUIRED,
    private val hitsRequired: Int = DEFAULT_HITS_REQUIRED
) : IdentityFoundStateTransitioner {
    @VisibleForTesting
    internal var hitsCount = 0

    // saves the results of previous certain number of frames
    @VisibleForTesting
    internal val results = ArrayDeque<Boolean>()

    override fun transition(
        foundState: Found,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        val isHit = analyzerOutput.category.matchesScanType(foundState.type)
        if (isHit) {
            hitsCount++
        }
        results.addLast(isHit)
        // only save the last certain number of frames, dropping the first one if it goes beyond
        // If the first result is a hit, then decrease the hitsCount
        if (results.size > frameRequired) {
            val firstResultIsHit = results.removeFirst()
            if (firstResultIsHit) {
                hitsCount--
            }
        }

        return when {
            isUnsatisfied() -> {
                val reason =
                    "hits count below expected: $hitsCount"
                Log.d(
                    TAG,
                    "Satisfaction check fails due to $reason, transition to Unsatisfied."
                )
                Unsatisfied(reason, foundState.type)
            }
            moreResultsRequired() -> {
                Log.d(
                    TAG,
                    "More results needed, stay in Found, currently ${results.size} results are collected"
                )
                foundState
            }
            else -> {
                Log.d(
                    TAG,
                    "Satisfaction check succeeds, transition to Satisfied."
                )
                Satisfied(foundState.type)
            }
        }
    }

    /**
     * Determine if more images should be processed before reaching [Satisfied].
     *
     * Need to collect [frameRequired] results.
     */
    private fun moreResultsRequired(): Boolean {
        return results.size < frameRequired
    }

    /**
     * Determine if satisfaction failed and should transition to [Unsatisfied].
     *
     * Transfers to when the previous [frameRequired] number of frames has hits below
     * [hitsRequired].
     */
    private fun isUnsatisfied(): Boolean {
        return (results.size == frameRequired) && (hitsCount < hitsRequired)
    }

    internal companion object {
        val TAG: String = LookBackWindowTransitioner::class.java.simpleName

        // The number of frames needs to collected to determine if a model has found the
        // correct item.
        const val DEFAULT_FRAMES_REQUIRED = 100

        // The number of hits to determine if the model has found the correct item.
        const val DEFAULT_HITS_REQUIRED = 50
    }
}
