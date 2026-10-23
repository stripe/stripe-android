-- Inspect table schemas with LIMIT 0 before running against another Perfetto version.
WITH boundary AS (
    SELECT s.ts AS loaded_at, s.ts + s.dur AS drawn_at, p.upid
    FROM slice s
    JOIN process_track t ON t.id = s.track_id
    JOIN process p USING (upid)
    WHERE s.name = 'PaymentSheet#loadToFirstDraw'
        AND p.name = 'com.stripe.android.paymentsheet.benchmarkapp'
        AND s.dur > 0
)
SELECT p.pid, th.tid, th.name AS thread_name, s.name,
    (s.ts - b.loaded_at) / 1000000.0 AS start_after_load_ms,
    s.dur / 1000000.0 AS duration_ms,
    (b.drawn_at - b.loaded_at) / 1000000.0 AS first_draw_after_load_ms
FROM slice s
JOIN thread_track t ON t.id = s.track_id
JOIN thread th USING (utid)
JOIN process p USING (upid)
JOIN boundary b ON b.upid = p.upid
WHERE s.dur > 0 AND s.name IN (
    'performCreate:com.stripe.android.challenge.passive.warmer.activity.PassiveChallengeWarmerActivity',
    'WebViewChromiumFactoryProvider.initialize',
    'WebViewChromiumAwInit.startChromiumLocked',
    'WebViewChromium.init'
)
ORDER BY s.ts;
