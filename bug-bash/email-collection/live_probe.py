"""Live email controls using Checkout client parameter shapes, not native UI.

Run: python3 bug-bash/email-collection/live_probe.py
Only test-mode objects and synthetic email addresses are used. Credentials stay
in memory; requests and responses on disk are redacted.
"""

import json
from pathlib import Path
import re
import urllib.error
import urllib.parse
import urllib.request

BASE = "https://stp-mobile-playground-backend-v7.stripedemos.com"
OUT = Path(__file__).resolve().parent
PACKAGE = "com.stripe.android.paymentsheet.example"


def redact(value):
    if isinstance(value, dict):
        return {key: None if key == "passive_captcha" else "[REDACTED]"
                if isinstance(item, str) and any(part in key.lower() for part in ("secret", "token", "api_key"))
                else redact(item) for key, item in value.items()}
    if isinstance(value, list):
        return [redact(item) for item in value]
    if isinstance(value, str):
        return re.sub(r"(?:pk|sk)_(?:test|live)_\w+|\w+_secret_\w+", "[REDACTED]", value)
    return value


def save(name, data):
    (OUT / (name + ".json")).write_text(json.dumps(redact(data), indent=2) + "\n")


class ApiError(RuntimeError):
    def __init__(self, body):
        self.body = redact(body)
        super().__init__(json.dumps(self.body))


def request(url, params=None, key=None, json_body=False):
    headers = {"Authorization": "Bearer " + key} if key else {}
    data = None
    if params is not None:
        data = json.dumps(params).encode() if json_body else urllib.parse.urlencode(params).encode()
        if json_body:
            headers["Content-Type"] = "application/json"
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=headers), timeout=45) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise ApiError(json.loads(error.read())) from None


def backend(endpoint, params):
    body = {"merchant": "US", "request_params": params}
    if endpoint == "create_checkout_session":
        body["stripe_version"] = "2026-08-26.preview"
    return request(BASE + "/" + endpoint, body, json_body=True)


def client(secret, endpoint, key, params=None):
    payload = {"client_secret": secret, "locale": "en-US", "mobile_session_id": "email-collection-bash",
               "mobile_app_id": PACKAGE, **(params or {})}
    url = "https://api.stripe.com/v1/checkout_client/" + secret.split("_secret_")[0] + "/" + endpoint
    if endpoint in ("init", "retrieve"):
        payload["timezone"] = "America/New_York"
    if endpoint == "retrieve":
        return request(url + "?" + urllib.parse.urlencode(payload), key=key)
    return request(url, payload, key)


def create(name, key, **extra):
    params = {"ui_mode": "mobile_elements", "currency": "usd", "allowed_payment_method_types": ["card"],
              "automatic_tax": {"enabled": False}, "adaptive_pricing": {"enabled": False},
              "items": [{"type": "one_time_price", "one_time_price": {"items": [
                  {"price_data": {"currency": "usd", "unit_amount": 12000,
                                  "product_data": {"name": "Email Collection Test"}}, "quantity": 1}
              ]}}], **extra}
    save(name + "-creation", params)
    secret = backend("create_checkout_session", params)["client_secret"]
    response = client(secret, "init", key)
    save(name + "-init", response)
    assert response["livemode"] is False and response["status"] == "open"
    assert response["total_summary"]["total"] == 12000
    return secret, response


def confirm_params(key, email, billing_email=None):
    pm_params = {
        "type": "card", "card[token]": "tok_visa", "billing_details[name]": "Email Collection Test",
    }
    if billing_email is not None:
        pm_params["billing_details[email]"] = billing_email
    pm = request("https://api.stripe.com/v1/payment_methods", pm_params, key)
    assert pm["livemode"] is False
    params = {"payment_method": pm["id"], "expected_payment_method_type": "card", "expected_amount": 12000,
              "return_url": "stripesdk://payment_return_url/" + PACKAGE}
    if email is not None:
        params["collected_information[email]"] = email
    return params


def complete(name, secret, key, params, expected_email):
    save(name + "-confirm-request", params)
    response = client(secret, "confirm", key, params)
    save(name + "-confirm", response)
    assert response["status"] == "complete" and response["payment_status"] == "paid"
    ref = response["payment_response"]["payment_intent"]
    pi = request("https://api.stripe.com/v1/payment_intents/" + ref["id"] + "?" +
                 urllib.parse.urlencode({"client_secret": ref["client_secret"]}), key=key)
    save(name + "-payment-intent", pi)
    assert pi["status"] == "succeeded" and pi["amount"] == 12000
    customer_id = (response.get("customer") or {}).get("id")
    returning_email = None
    if customer_id:
        _, returning = create(name + "-returning", key, customer=customer_id)
        assert returning["customer"]["id"] == customer_id
        returning_email = returning.get("customer_email") or returning["customer"].get("email")
        assert returning_email == expected_email, (returning_email, expected_email)
    result = {"case": name, "confirmed": True, "payment_intent_status": pi["status"],
              "customer_id": customer_id, "returning_email": returning_email,
              "customer_persistence_checked": customer_id is not None}
    print(json.dumps(result), flush=True)
    return result


def run():
    key = request(BASE + "/publishable_key?merchant=US")["publishable_key"]
    assert key.startswith("pk_test_"), "Only test mode is authorized"
    results = []
    secret, initial = create("guest-validation", key)
    assert initial.get("customer_email") is None and initial.get("customer") is None
    params = confirm_params(key, None)
    for name, email in (("missing", None), ("empty", ""), ("whitespace", "   "), ("invalid", "not-an-email")):
        attempted = {**params}
        if email is not None:
            attempted["collected_information[email]"] = email
        save("guest-validation-" + name + "-request", attempted)
        try:
            response = client(secret, "confirm", key, attempted)
            save("guest-validation-" + name + "-unexpected-success", response)
            raise AssertionError("Invalid or missing email unexpectedly confirmed: " + name)
        except ApiError as error:
            save("guest-validation-" + name + "-error", error.body)
            current = client(secret, "retrieve", key)
            save("guest-validation-" + name + "-retrieve", current)
            assert current["status"] == "open" and current["payment_status"] == "unpaid"
            results.append({"case": name, "accepted": False, "session_open": True, "error": error.body})
            print(json.dumps(results[-1]), flush=True)
    valid_email = "email-collection+recovered@example.com"
    params["collected_information[email]"] = valid_email
    results.append(complete("guest-validation", secret, key, params, valid_email))
    save("backend-results", results)

    cases = [("guest-fixed", {"customer_email": "fixed-session@example.com"}, None, "fixed-session@example.com"),
             ("guest-create-customer", {"customer_creation": "always"},
              "Email.Collection+Created@example.com", "Email.Collection+Created@example.com"),
             ("guest-billing-fallback", {}, None, "card-billing@example.com")]
    fixed_customer = backend("create_customer", {"email": "fixed-customer@example.com"})
    empty_customer = backend("create_customer", {})
    cases += [("customer-fixed", {"customer": fixed_customer["id"]}, None, "fixed-customer@example.com"),
              ("customer-collect", {"customer": empty_customer["id"]},
               "email-collection+customer@example.com", "email-collection+customer@example.com")]
    for name, options, email, expected in cases:
        secret, initial = create(name, key, **options)
        fixed = initial.get("customer_email") or (initial.get("customer") or {}).get("email")
        assert fixed == (expected if name in ("guest-fixed", "customer-fixed") else None), (name, fixed)
        results.append(complete(name, secret, key, confirm_params(key, email, "card-billing@example.com"), expected))
        save("backend-results", results)


if __name__ == "__main__":
    run()
