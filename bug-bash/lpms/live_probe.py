"""Test-mode Checkout LPM controls. Run with `python3 .../live_probe.py PHASE`.

PHASE is methods, outcomes, or redisplay. Credentials stay in memory, and retained
responses are redacted. This direct API probe does not exercise native UI.
"""
import importlib.util
import json
from pathlib import Path
import sys
import urllib.parse

sys.dont_write_bytecode = True
OUT = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("email_probe", OUT.parent / "email-collection/live_probe.py")
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)
api.OUT = OUT
request, backend = api.request, api.backend


def save(name, value):
    def clean(item):
        if isinstance(item, dict):
            return {k: "https://example.com/redacted-redirect" if k == "url" and isinstance(v, str)
                    and urllib.parse.urlparse(v).netloc.endswith("stripe.com") else clean(v)
                    for k, v in item.items()}
        if isinstance(item, list):
            return [clean(v) for v in item]
        return item
    api.save(name, clean(value))


def client(secret, endpoint, key, params=None):
    if endpoint == "poll":
        url = "https://api.stripe.com/v1/checkout_client/" + secret.split("_secret_")[0] + "/poll?"
        return request(url + urllib.parse.urlencode({"client_secret": secret}), key=key)
    return api.client(secret, endpoint, key, params)


def create(name, key, types, currency="eur", **extra):
    params = {"ui_mode": "mobile_elements", "currency": currency,
              "customer_email": "lpm-bash@example.com", "automatic_tax": {"enabled": False},
              "adaptive_pricing": {"enabled": False},
              "items": [{"type": "one_time_price", "one_time_price": {"items": [{
                  "price_data": {"currency": currency, "unit_amount": 12000,
                                 "product_data": {"name": "LPM Test"}}, "quantity": 1}]}}], **extra}
    if types is not None:
        params["allowed_payment_method_types"] = types
    if "customer" in extra:
        params.pop("customer_email", None)
    save(name + "-creation", params)
    secret = backend("create_checkout_session", params)["client_secret"]
    response = client(secret, "init", key)
    save(name + "-init", response)
    assert response["livemode"] is False and response["status"] == "open"
    assert response["total_summary"]["total"] == 12000
    return secret, response


def payment_method(key, method, redisplay="unspecified", token="tok_visa"):
    params = {"type": method, "allow_redisplay": redisplay,
              "billing_details[name]": "LPM Test", "billing_details[email]": "lpm-bash@example.com"}
    if method == "card":
        params["card[token]"] = token
    elif method == "sepa_debit":
        params["sepa_debit[iban]"] = "DE89370400440532013000"
    pm = request("https://api.stripe.com/v1/payment_methods", params, key)
    assert pm["livemode"] is False
    return pm


def summary(response):
    customer = response.get("customer") or {}
    return {"status": response.get("status"), "payment_status": response.get("payment_status"),
            "ordered_types": response.get("ordered_payment_method_types_and_wallets"),
            "automatic_payment_method_types": response.get("automatic_payment_method_types"),
            "setup_future_usage_for_payment_method_type": response.get("setup_future_usage_for_payment_method_type"),
            "total": response.get("total_summary", {}).get("total"),
            "customer_id": customer.get("id"),
            "saved_methods": [{k: p.get(k) for k in ("id", "type", "allow_redisplay", "card", "sepa_debit")}
                              for p in customer.get("payment_methods", [])]}


def outcomes(key):
    results = []
    for method, choice in (("card", None), ("sepa_debit", False), ("sepa_debit", True), ("bancontact", None)):
        name = "outcome-" + method + ("-save" if choice else "-no-save" if choice is False else "")
        try:
            opts = {}
            if choice is not None:
                opts["customer"] = backend("create_customer", {"email": "lpm-bash@example.com"})["id"]
                opts["saved_payment_method_options"] = {"payment_method_save": "enabled"}
            secret, initial = create(name, key, [method], **opts)
            save(name + "-poll-before", client(secret, "poll", key))
            pm = payment_method(key, method, "always" if choice else "unspecified")
            save(name + "-created-method", pm)
            params = {"payment_method": pm["id"], "expected_payment_method_type": method,
                      "expected_amount": 12000, "return_url": "stripesdk://payment_return_url/" + api.PACKAGE}
            if choice is not None:
                params["collected_information[save_payment_method]"] = str(choice).lower()
            save(name + "-confirm-request", params)
            confirmed = client(secret, "confirm", key, params)
            save(name + "-confirm", confirmed)
            pi_ref = confirmed["payment_response"]["payment_intent"]
            pi = request("https://api.stripe.com/v1/payment_intents/" + pi_ref["id"] + "?" +
                         urllib.parse.urlencode({"client_secret": pi_ref["client_secret"], "expand[]": "payment_method"}), key=key)
            save(name + "-payment-intent", pi)
            polls = [client(secret, "poll", key) for _ in range(3)]
            save(name + "-polls", polls)
            result = {"case": name, "confirm": summary(confirmed), "intent_status": pi["status"],
                      "intent_sfu": pi.get("setup_future_usage"), "next_action_type": (pi.get("next_action") or {}).get("type"),
                      "polls": polls}
            if choice is not None:
                _, returning = create(name + "-returning", key, ["card", "sepa_debit"], customer=opts["customer"],
                                      saved_payment_method_options={"allow_redisplay_filters": ["always", "limited", "unspecified"]})
                result["returning"] = summary(returning)
                result["new_method_id"] = pm["id"]
            results.append(result)
        except api.ApiError as error:
            save(name + "-error", error.body)
            results.append({"case": name, "error": error.body})
        save("outcomes-results", results)
        print(json.dumps(api.redact(results[-1])), flush=True)


def methods(key):
    results = []
    for name, types, currency in (("card-only", ["card"], "eur"), ("sepa-only", ["sepa_debit"], "eur"),
                                   ("bancontact-only", ["bancontact"], "eur"),
                                   ("mixed", ["card", "sepa_debit", "bancontact"], "eur"),
                                   ("automatic", None, "eur"),
                                   ("sepa-usd", ["sepa_debit"], "usd"), ("empty", [], "eur")):
        name = "methods-" + name
        try:
            secret, response = create(name, key, types, currency)
            result = {"case": name, "requested": types, "response": summary(response)}
            if types == ["sepa_debit"] and currency == "eur":
                pm = payment_method(key, "card")
                params = {"payment_method": pm["id"], "expected_payment_method_type": "card", "expected_amount": 12000,
                          "return_url": "stripesdk://payment_return_url/" + api.PACKAGE}
                try:
                    result["disallowed_card_confirm"] = summary(client(secret, "confirm", key, params))
                except api.ApiError as error:
                    result["disallowed_card_error"] = error.body
        except api.ApiError as error:
            result = {"case": name, "error": error.body}
        results.append(result)
        save("methods-results", results)
        print(json.dumps(api.redact(result)), flush=True)


def redisplay(key):
    customer = backend("create_customer", {"email": "lpm-redisplay@example.com"})["id"]
    inventory = []
    for method, state, token in (("card", "always", "tok_visa"), ("card", "limited", "tok_mastercard"),
                                  ("card", "unspecified", "tok_amex"), ("sepa_debit", "always", None),
                                  ("sepa_debit", "limited", None), ("sepa_debit", "unspecified", None)):
        pm = payment_method(key, method, state, token)
        backend_body = {"merchant": "US", "request_params": {"customer": customer}, "payment_method_id": pm["id"]}
        request(api.BASE + "/attach_payment_method", backend_body, json_body=True)
        inventory.append(pm)
    save("redisplay-provisioned-methods", inventory)
    results = []
    for name, filters, types in (("default", None, ["card", "sepa_debit"]),
                                  ("all", ["always", "limited", "unspecified"], ["card", "sepa_debit"]),
                                  ("limited", ["limited"], ["card", "sepa_debit"]),
                                  ("unspecified", ["unspecified"], ["card", "sepa_debit"]),
                                  ("sepa-only", ["always", "limited", "unspecified"], ["sepa_debit"]),
                                  ("card-only", ["always", "limited", "unspecified"], ["card"])):
        opts = {"payment_method_save": "enabled"}
        if filters is not None:
            opts["allow_redisplay_filters"] = filters
        name = "redisplay-" + name
        try:
            _, response = create(name, key, types, customer=customer, saved_payment_method_options=opts)
            result = {"case": name, "filters": filters, "response": summary(response)}
        except api.ApiError as error:
            result = {"case": name, "error": error.body}
        results.append(result)
        save("redisplay-results", results)
        print(json.dumps(api.redact(result)), flush=True)


if __name__ == "__main__":
    key = request(api.BASE + "/publishable_key?merchant=US")["publishable_key"]
    assert key.startswith("pk_test_"), "Only test mode is authorized"
    {"methods": methods, "outcomes": outcomes, "redisplay": redisplay}[sys.argv[1]](key)
