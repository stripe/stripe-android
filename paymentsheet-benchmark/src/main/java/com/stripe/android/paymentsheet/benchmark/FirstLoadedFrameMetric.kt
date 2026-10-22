package com.stripe.android.paymentsheet.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor

/** Associate the loaded-content draw with its surface frame token, not an earlier loading frame. */
@OptIn(ExperimentalMetricApi::class)
internal class FirstLoadedFrameMetric : TraceMetric() {
    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Measurement> {
        val packageName = captureInfo.targetPackageName.replace("'", "''")
        val rows = traceSession.query(
            """
            WITH boundary AS (
                SELECT s.ts AS loaded_at, s.ts + s.dur AS drawn_at, p.upid
                FROM slice s
                JOIN process_track t ON s.track_id = t.id
                JOIN process p USING (upid)
                WHERE s.name = 'PaymentSheet#loadToFirstDraw'
                    AND s.dur > 0 AND p.name = '$packageName'
            ), draw_frame AS (
                SELECT b.*, CAST(SUBSTR(s.name, LENGTH('Choreographer#doFrame ') + 1) AS INT) AS token
                FROM boundary b
                JOIN thread th ON th.upid = b.upid
                JOIN process p ON p.upid = b.upid AND th.tid = p.pid
                JOIN thread_track tt ON tt.utid = th.utid
                JOIN slice s ON s.track_id = tt.id
                WHERE s.name GLOB 'Choreographer#doFrame [0-9]*'
                    AND s.ts <= b.drawn_at AND s.ts + s.dur >= b.drawn_at
            ), submitted AS (
                SELECT d.*, a.ts AS frame_start, a.layer_name
                FROM draw_frame d
                JOIN actual_frame_timeline_slice a ON a.upid = d.upid AND a.surface_frame_token = d.token
                WHERE a.dur > 0 AND a.layer_name GLOB '*PaymentSheetActivity*'
            ), presented AS (
                SELECT s.loaded_at, s.drawn_at, a.*
                FROM submitted s
                JOIN actual_frame_timeline_slice a ON a.upid = s.upid AND a.layer_name = s.layer_name
                WHERE a.ts >= s.frame_start AND a.dur > 0
                    AND a.present_type IN ('On-time Present', 'Early Present', 'Late Present')
                ORDER BY a.ts + a.dur
                LIMIT 1
            )
            SELECT
                (p.drawn_at - p.loaded_at) / 1000000.0 AS draw_ms,
                (p.ts + p.dur - p.loaded_at) / 1000000.0 AS presented_ms,
                (p.ts + p.dur - e.ts - e.dur) / 1000000.0 AS overrun_ms
            FROM presented p
            JOIN expected_frame_timeline_slice e
                ON e.upid = p.upid AND e.surface_frame_token = p.surface_frame_token
                    AND e.layer_name = p.layer_name
            """.trimIndent()
        ).toList()
        val frame = checkNotNull(rows.singleOrNull()) {
            "Expected one loaded PaymentSheet frame with a completed draw trace and FrameTimeline token. " +
                "Inspect this iteration's Perfetto trace; missing markers/frames must not yield a passing measurement."
        }
        return listOf(
            Measurement("loadToFirstDrawMs", frame.double("draw_ms")),
            Measurement("loadToFirstPresentedFrameMs", frame.double("presented_ms")),
            Measurement("firstPresentedFrameOverrunMs", frame.double("overrun_ms")),
        )
    }
}
