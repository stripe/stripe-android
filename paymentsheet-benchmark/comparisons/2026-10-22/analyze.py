"""Recompute exploratory median differences from the preserved Macrobenchmark JSON.

Run: python3 analyze.py
Positive reduction means the isolated fix was faster. Intervals resample each
arm independently; iterations are not paired. These are descriptive intervals,
not adjusted for 16 comparisons, and cannot correct host drift or block order.
"""

import csv
import json
import random
import statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parent
METRICS = (
    "loadToFirstDrawMs",
    "loadToFirstPresentedFrameMs",
    "presentToLoadMs",
    "presentToFirstDrawMs",
    "presentToFirstPresentedFrameMs",
    "firstPresentedFrameOverrunMs",
)


def bootstrap_interval(before, after):
    rng = random.Random(160016)
    differences = sorted(
        statistics.median(rng.choices(before, k=len(before)))
        - statistics.median(rng.choices(after, k=len(after)))
        for _ in range(10_000)
    )
    return [differences[249], differences[9749]]


def main():
    results = []
    samples = []
    comparisons = [(fix, f"{fix:02}") for fix in range(1, 17)] + [(14, "14-repeat")]
    for fix, comparison in comparisons:
        arms = []
        metadata = []
        for arm in ("baseline", "fix"):
            folder = ROOT / "raw" / f"{comparison}-{arm}"
            document = json.loads((folder / "benchmarkData.json").read_text())
            assert len(document["benchmarks"]) == 1
            benchmark = document["benchmarks"][0]
            assert benchmark["repeatIterations"] == 10
            assert benchmark["name"] == "coldWithBaselineProfile"
            arms.append(benchmark)
            metadata.append(json.loads((folder / "metadata.json").read_text()))
        assert metadata[0]["workload"] == metadata[1]["workload"]
        assert metadata[0]["baseCommit"] == metadata[1]["baseCommit"]
        result = {"comparison": comparison, "id": fix, "workload": metadata[0]["workload"], "metrics": {}}
        for metric in METRICS:
            runs = [benchmark["metrics"][metric]["runs"] for benchmark in arms]
            assert all(len(values) == 10 for values in runs)
            before, after = map(statistics.median, runs)
            result["metrics"][metric] = {
                "baselineMedian": before,
                "fixMedian": after,
                "reductionMs": before - after,
                "bootstrap95CI": bootstrap_interval(*runs),
                "baselineRange": [min(runs[0]), max(runs[0])],
                "fixRange": [min(runs[1]), max(runs[1])],
            }
            for arm, values in zip(("baseline", "fix"), runs):
                for iteration, value in enumerate(values):
                    samples.append((comparison, fix, result["workload"], arm, iteration, metric, value))
        result["journeyFrameTiming"] = {}
        for arm, benchmark in zip(("baseline", "fix"), arms):
            overrun_runs = benchmark["sampledMetrics"]["frameOverrunMs"]["runs"]
            fractions = [sum(value > 0 for value in run) / len(run) for run in overrun_runs]
            result["journeyFrameTiming"][arm] = {
                "medianFrameCount": benchmark["metrics"]["frameCount"]["median"],
                "frameOverrunP50Ms": benchmark["sampledMetrics"]["frameOverrunMs"]["P50"],
                "frameOverrunP90Ms": benchmark["sampledMetrics"]["frameOverrunMs"]["P90"],
                "frameOverrunP95Ms": benchmark["sampledMetrics"]["frameOverrunMs"]["P95"],
                "medianPositiveOverrunFraction": statistics.median(fractions),
                "positiveOverrunFractions": fractions,
            }
        results.append(result)
    (ROOT / "results.json").write_text(json.dumps(results, indent=2) + "\n")
    with (ROOT / "samples.csv").open("w", newline="") as output:
        writer = csv.writer(output, lineterminator="\n")
        writer.writerow(("comparison", "fix", "workload", "arm", "iteration", "metric", "milliseconds"))
        writer.writerows(samples)
    print("| Fix | Load → draw baseline / fix | Reduction ms [95% interval] | Present → frame reduction |")
    print("| --- | --- | --- | --- |")
    for result in results:
        metric = result["metrics"]["loadToFirstDrawMs"]
        low, high = metric["bootstrap95CI"]
        total = result["metrics"]["presentToFirstPresentedFrameMs"]["reductionMs"]
        print(
            f"| {result['comparison']} | {metric['baselineMedian']:.2f} / {metric['fixMedian']:.2f} "
            f"| {metric['reductionMs']:+.2f} [{low:+.2f}, {high:+.2f}] | {total:+.2f} |"
        )


if __name__ == "__main__":
    main()
