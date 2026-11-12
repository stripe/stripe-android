"""Verify the recorded live controls without provisioning more backend objects."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def read(name):
    return json.loads((ROOT / (name + ".json")).read_text())


methods = {row["case"]: row for row in read("methods-results")}
assert methods["methods-card-only"]["response"]["ordered_types"] == ["card", "apple_pay", "google_pay"]
assert methods["methods-sepa-only"]["response"]["ordered_types"] == ["sepa_debit"]
assert methods["methods-bancontact-only"]["response"]["ordered_types"] == ["bancontact"]
assert methods["methods-mixed"]["response"]["ordered_types"] == ["card", "apple_pay", "google_pay", "sepa_debit", "bancontact"]
assert methods["methods-automatic"]["response"]["ordered_types"] == methods["methods-empty"]["response"]["ordered_types"]
assert methods["methods-sepa-only"]["disallowed_card_error"]["error"]["extra_fields"]["confirm_error_reason"] == "payment_method_types_mismatch"
assert "Invalid currency options `usd`" in methods["methods-sepa-usd"]["error"]["error"]

redisplay = {row["case"]: row for row in read("redisplay-results")}
for name, state in (("default", "always"), ("limited", "limited"), ("unspecified", "unspecified")):
    rows = redisplay["redisplay-" + name]["response"]["saved_methods"]
    assert {row["type"] for row in rows} == {"card", "sepa_debit"}
    assert {row["allow_redisplay"] for row in rows} == {state}
rows = redisplay["redisplay-all"]["response"]["saved_methods"]
assert len(rows) == 4
assert {row["allow_redisplay"] for row in rows} == {"always", "limited", "unspecified"}
assert {row["card"]["brand"] for row in rows if row["type"] == "card"} == {"visa", "mastercard", "amex"}
for name, expected in (("sepa-only", "sepa_debit"), ("card-only", "card")):
    assert {row["type"] for row in redisplay["redisplay-" + name]["response"]["saved_methods"]} == {expected}

outcomes = {row["case"]: row for row in read("outcomes-results")}
for name in ("card", "sepa_debit-save", "sepa_debit-no-save", "bancontact"):
    row = outcomes["outcome-" + name]
    assert row["confirm"]["total"] == 12000
    before = read("outcome-" + name + "-poll-before")
    assert before["confirmation_status"] == "not_started"
    for poll in row["polls"]:
        assert poll["confirmation_status"] == ("pending" if name == "bancontact" else "succeeded")
for name in ("sepa_debit-save", "sepa_debit-no-save"):
    row = outcomes["outcome-" + name]
    assert row["intent_status"] == "processing"
    assert row["confirm"]["status"] == "complete" and row["confirm"]["payment_status"] == "unpaid"
saved = outcomes["outcome-sepa_debit-save"]
assert saved["intent_sfu"] == "off_session"
assert saved["returning"]["saved_methods"][0]["id"] == saved["new_method_id"]
assert saved["returning"]["saved_methods"][0]["allow_redisplay"] == "always"
declined_save = outcomes["outcome-sepa_debit-no-save"]
assert declined_save["intent_sfu"] is None and declined_save["returning"]["saved_methods"] == []
assert outcomes["outcome-card"]["intent_status"] == "succeeded"
assert outcomes["outcome-card"]["confirm"]["payment_status"] == "paid"
assert outcomes["outcome-bancontact"]["intent_status"] == "requires_action"
assert outcomes["outcome-bancontact"]["next_action_type"] == "redirect_to_url"
assert outcomes["outcome-bancontact"]["confirm"]["status"] == "open"
print("Verified 7 method configurations, 6 redisplay configurations, and 4 payment outcomes.")
