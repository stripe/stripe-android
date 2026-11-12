"""Read-only assertions over retained redacted live SFU evidence."""
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent


def read(name):
    return json.loads((ROOT / (name + ".json")).read_text())


results = [result for phase in ("sfu", "global", "pmo", "sepa", "precedence", "save-none", "mixed")
           for result in read(phase + "-results") if "error" not in result]
assert len(results) == 19, len(results)
for result in results:
    name = result["case"]
    initial = read(name + "-init")
    confirm = read(name + "-confirm")
    pi = read(name + "-payment-intent")
    assert initial["livemode"] is False and pi["livemode"] is False
    assert confirm["status"] == "complete"
    assert pi["status"] == ("succeeded" if result["method"] == "card" else "processing")
    assert result["payment_method_id"] == pi["payment_method"]["id"]
    effective = result["merchant_pmo_sfu"] or result["merchant_sfu"]
    assert result["effective_initial_sfu"] == effective
    expected_sfu = result["merchant_sfu"] or ("off_session" if result["save_choice"] else None)
    assert result["intent_sfu"] == expected_sfu, name
    expected_attached = result["merchant_pmo_sfu"] != "none" and bool(effective or result["save_choice"])
    assert (result["payment_method_customer"] == result["customer_id"]) == expected_attached, name
    assert bool(result["returning_all"]) == expected_attached, name
    assert bool(result["returning_default"]) == (expected_attached and result["save_choice"] is True), name
    for method in result["returning_all"]:
        assert method["id"] == result["payment_method_id"]

control, none_case = read("save-none-results")
assert control["returning_default"] and control["payment_method_customer"]
assert none_case["save_choice"] is True and none_case["payment_method_redisplay"] == "always"
assert none_case["intent_method_options"]["sepa_debit"]["setup_future_usage"] == "none"
assert none_case["returning_all"] == [] and none_case["payment_method_customer"] is None

for path in ROOT.glob("*.json"):
    body = path.read_text()
    json.loads(body)
    assert not re.search(r"(?:pk|sk)_(?:test|live)_[A-Za-z0-9]+|\b\w+_secret_\w+", body), path.name
    assert not re.search(r"https://(?:hooks|checkout|buy|pm-redirects)\.stripe\.com", body), path.name

print(f"PASS: {len(results)} live confirmations, SFU/PMO precedence, attachment, redisplay, and redaction checks")
