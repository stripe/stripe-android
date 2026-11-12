"""Live test-mode automatic-tax controls. Run from any directory with Python 3.

Uses the SDK's Checkout client request shapes, not native UI orchestration.
Credentials stay in memory and retained responses are sanitized.
"""

import json
from pathlib import Path
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

BASE = "https://stp-mobile-playground-backend-v7.stripedemos.com"
OUT = Path(__file__).resolve().parent
PACKAGE = "com.stripe.android.paymentsheet.example"
ADDRESSES = {
    "sf": {"country": "US", "line1": "510 Townsend St", "line2": "Suite 42", "city": "San Francisco", "state": "CA", "postal_code": "94103"},
    "oregon": {"country": "US", "line1": "1120 SW 5th Ave", "city": "Portland", "state": "OR", "postal_code": "97204"},
    "canada": {"country": "CA", "line1": "250 Front St W", "city": "Toronto", "state": "ON", "postal_code": "M5V 3G5"},
    "france": {"country": "FR"},
    "zip_only": {"country": "US", "postal_code": "94103"},
    "invalid_country": {"country": "XX"},
}


def redact(value):
    if isinstance(value, dict):
        return {k: None if k == "passive_captcha" else "[REDACTED]" if isinstance(v, str) and
                any(part in k.lower() for part in ("secret", "token", "api_key")) else redact(v)
                for k, v in value.items()}
    if isinstance(value, list):
        return [redact(v) for v in value]
    if isinstance(value, str):
        return re.sub(r"(?:pk|sk)_(?:test|live)_\w+|\w+_secret_\w+", "[REDACTED]", value)
    return value


class ApiError(RuntimeError):
    def __init__(self, body):
        self.body = redact(body)
        super().__init__(json.dumps(self.body))


def request(url, params=None, key=None, json_body=False):
    headers = {}
    data = None
    if params is not None:
        data = (json.dumps(params).encode() if json_body else urllib.parse.urlencode(params).encode())
        if json_body:
            headers["Content-Type"] = "application/json"
    if key:
        headers["Authorization"] = "Bearer " + key
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=headers), timeout=45) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise ApiError(json.loads(error.read())) from None


def save(name, data):
    (OUT / (name + ".json")).write_text(json.dumps(redact(data), indent=2) + "\n")


def backend(endpoint, params):
    return request(BASE + "/" + endpoint, {"merchant": "us_tax", "request_params": params,
                   "stripe_version": "2026-08-26.preview"}, json_body=True)


def client(secret, endpoint, key, params=None):
    payload = {"client_secret": secret, "locale": "en-US", "mobile_session_id": "automatic-tax-bug-bash",
               "mobile_app_id": PACKAGE, **(params or {})}
    url = "https://api.stripe.com/v1/checkout_client/" + secret.split("_secret_")[0] + "/" + endpoint
    if endpoint in ("init", "retrieve"):
        payload["timezone"] = "America/New_York"
    if endpoint == "retrieve":
        return request(url + "?" + urllib.parse.urlencode(payload), key=key)
    return request(url, payload, key)


def creation(**extra):
    return {"ui_mode": "mobile_elements", "currency": "usd", "allowed_payment_method_types": ["card"],
            "automatic_tax": {"enabled": True}, "items": [{"type": "one_time_price", "one_time_price": {"items": [
                {"price_data": {"currency": "usd", "unit_amount": amount, "tax_behavior": "exclusive",
                                "product_data": {"name": name, "tax_code": "txcd_99999999"}}, "quantity": quantity}
                for name, amount, quantity in (("Classic T-Shirt", 3500, 2), ("Zip-Up Hoodie", 5000, 1))
            ]}}], **extra}


def amounts(response, source):
    assert response["livemode"] is False
    assert response["automatic_tax"]["address_source"] == source, response["automatic_tax"]
    summary = response["total_summary"]
    items = [item for group in response["items"] for item in group["one_time_price"]["items"]]
    assert summary["subtotal"] == 12000
    assert sum(item["total"] for item in items) == summary["total"]
    assert sum(item["tax_exclusive"] for item in items) == summary["tax_exclusive"]
    assert summary["total"] == 12000 + summary["tax_exclusive"]
    return {"tax_source": source, "tax_status": response["automatic_tax"]["status"],
            "tax": summary["tax_exclusive"], "total": summary["total"]}


def step(name, secret, key, source, address):
    params = {"tax_region": ""} if address is None else {"tax_region[" + k + "]": v for k, v in address.items()}
    save(name + "-request", params)
    response = client(secret, "update", key, params)
    save(name, response)
    current = amounts(response, source)
    fetched = client(secret, "retrieve", key)
    save(name + "-retrieve", fetched)
    assert amounts(fetched, source) == current
    print(name, json.dumps(current), flush=True)
    return response


def confirm(name, secret, key, source, response, billing, shipping=None):
    pm = request("https://api.stripe.com/v1/payment_methods", {"type": "card", "card[token]": "tok_visa",
                 "billing_details[name]": "Automatic Tax Test",
                 **{"billing_details[address][" + k + "]": v for k, v in billing.items()}}, key)
    assert pm["livemode"] is False
    params = {"payment_method": pm["id"], "expected_payment_method_type": "card",
              "expected_amount": response["total_summary"]["total"],
              "collected_information[email]": "automatic-tax-bash@example.com",
              "return_url": "stripesdk://payment_return_url/" + PACKAGE}
    if shipping:
        params["shipping[name]"] = "Automatic Tax Recipient"
        params.update({"shipping[address][" + k + "]": v for k, v in shipping.items()})
    save(name + "-confirm-request", params)
    completed = client(secret, "confirm", key, params)
    save(name + "-confirm", completed)
    assert completed["status"] == "complete" and completed["payment_status"] == "paid"
    assert amounts(completed, source) == amounts(response, source)
    ref = completed["payment_response"]["payment_intent"]
    pi = request("https://api.stripe.com/v1/payment_intents/" + ref["id"] + "?" +
                 urllib.parse.urlencode({"client_secret": ref["client_secret"]}), key=key)
    save(name + "-payment-intent", pi)
    assert pi["status"] == "succeeded" and pi["amount"] == params["expected_amount"]
    print(name + ": confirmed", pi["amount"], flush=True)
    return {**amounts(completed, source), "confirmed": True, "payment_intent_amount": pi["amount"]}


def run():
    key = request(BASE + "/publishable_key?merchant=us_tax")["publishable_key"]
    assert key.startswith("pk_test_"), "Only test mode is authorized"
    results = []
    for name, source, extra, steps, final_billing, final_shipping in (
        ("billing-auto", "billing", {}, ["sf", "oregon", "canada", "france", None, "sf"], "sf", None),
        ("billing-required", "billing", {"billing_address_collection": "required"}, ["sf", None, "oregon"], "oregon", None),
        ("shipping", "shipping", {"shipping_address_collection": {"allowed_countries": ["US", "CA", "IE", "GB"]},
                                    "billing_address_collection": "required"}, ["sf", "canada", None, "oregon"], "sf", "oregon"),
    ):
        params = creation(**extra)
        save(name + "-creation", params)
        secret = backend("create_checkout_session", params)["client_secret"]
        response = client(secret, "init", key)
        save(name + "-init", response)
        assert amounts(response, source)["tax_status"] == "requires_address"
        observations = [amounts(response, source)]
        for index, address_name in enumerate(steps):
            response = step(name + "-" + str(index) + "-" + (address_name or "clear"), secret, key, source,
                            ADDRESSES[address_name] if address_name else None)
            current = amounts(response, source)
            assert current["tax_status"] == ("ready" if address_name else "requires_address")
            if address_name is None or address_name == "oregon":
                assert current["tax"] == 0
            if address_name == "sf":
                assert current["tax"] == 1035
            observations.append(current)
        result = confirm(name, secret, key, source, response, ADDRESSES[final_billing],
                         ADDRESSES[final_shipping] if final_shipping else None)
        results.append({"case": name, "steps": observations, **result})
        save("backend-results", results)

    customer = backend("create_customer", {"email": "automatic-tax-bash@example.com", "address": ADDRESSES["sf"]})
    for name, source, extra in (("customer-source", "customer", {}),
                                ("customer-billing-source", "billing", {"customer_update": {"address": "auto"}})):
        params = creation(customer=customer["id"], **extra)
        save(name + "-creation", params)
        secret = backend("create_checkout_session", params)["client_secret"]
        response = client(secret, "init", key)
        save(name + "-init", response)
        current = amounts(response, source)
        print(name, json.dumps(current), flush=True)
        if source == "billing":
            response = step(name + "-oregon", secret, key, source, ADDRESSES["oregon"])
            current = amounts(response, source)
            assert current["tax"] == 0
        results.append({"case": name, **current, "confirmed": False})
        save("backend-results", results)


def boundaries():
    key = request(BASE + "/publishable_key?merchant=us_tax")["publishable_key"]
    assert key.startswith("pk_test_")
    params = creation()
    save("boundaries-creation", params)
    secret = backend("create_checkout_session", params)["client_secret"]
    response = client(secret, "init", key)
    save("boundaries-init", response)
    results = []
    for name, address in (("sf", ADDRESSES["sf"]), ("invalid-country", ADDRESSES["invalid_country"]),
                          ("zip-only", ADDRESSES["zip_only"]), ("country-only", {"country": "US"}),
                          ("invalid-postal", {"country": "US", "postal_code": "not-a-postal"}),
                          ("oregon", ADDRESSES["oregon"])):
        payload = {"tax_region[" + k + "]": v for k, v in address.items()}
        save("boundaries-" + name + "-request", payload)
        try:
            response = client(secret, "update", key, payload)
            save("boundaries-" + name, response)
            current = amounts(response, "billing")
            results.append({"step": name, "accepted": True, **current})
        except ApiError as error:
            save("boundaries-" + name + "-error", error.body)
            results.append({"step": name, "accepted": False, "error": error.body})
        fetched = client(secret, "retrieve", key)
        save("boundaries-" + name + "-retrieve", fetched)
        assert amounts(fetched, "billing") == amounts(response, "billing")
        print("boundaries-" + name, json.dumps(results[-1]), flush=True)
        save("boundary-results", results)
    pm = request("https://api.stripe.com/v1/payment_methods", {"type": "card", "card[token]": "tok_visa",
                 "billing_details[name]": "Automatic Tax Test",
                 **{"billing_details[address][" + k + "]": v for k, v in ADDRESSES["oregon"].items()}}, key)
    assert pm["livemode"] is False
    stale = {"payment_method": pm["id"], "expected_payment_method_type": "card", "expected_amount": 13035,
             "collected_information[email]": "automatic-tax-bash@example.com",
             "return_url": "stripesdk://payment_return_url/" + PACKAGE}
    save("boundaries-stale-confirm-request", stale)
    try:
        response = client(secret, "confirm", key, stale)
        save("boundaries-stale-confirm", response)
        results.append({"step": "stale-confirm", "accepted": True, **amounts(response, "billing")})
    except ApiError as error:
        save("boundaries-stale-confirm-error", error.body)
        results.append({"step": "stale-confirm", "accepted": False, "error": error.body})
        response = client(secret, "retrieve", key)
        save("boundaries-after-stale-confirm", response)
        results.append({"step": "correct-confirm", **confirm("boundaries", secret, key, "billing", response,
                                                             ADDRESSES["oregon"])})
    save("boundary-results", results)


if __name__ == "__main__":
    boundaries() if "--boundaries" in sys.argv else run()
