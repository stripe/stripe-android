"""Exercise test-mode Checkout adaptive pricing against the playground backend.

Run: python3 bug-bash/adaptive-pricing/live_probe.py
Creates fresh test-mode sessions and tokenized Visa PaymentMethods. Credentials
stay in memory; retained responses redact credentials and transient tokens.
See RUN_NOTES.md for actual coverage and limitations.
"""

import json
from pathlib import Path
import re
import urllib.error
import urllib.parse
import urllib.request


BASE = "https://stp-mobile-playground-backend-v7.stripedemos.com"
EVIDENCE = Path(__file__).resolve().parent
PACKAGE = "com.stripe.android.paymentsheet.example"


class ApiError(RuntimeError):
    def __init__(self, body):
        self.body = redact(body)
        super().__init__(json.dumps(self.body))


def redact(value):
    if isinstance(value, dict):
        return {
            k: None if k == "passive_captcha" else
            "[REDACTED]" if isinstance(v, str) and
            any(part in k.lower() for part in ("secret", "token", "api_key")) else redact(v)
            for k, v in value.items()
        }
    if isinstance(value, list):
        return [redact(v) for v in value]
    if isinstance(value, str):
        return re.sub(r"(?:pk|sk)_(?:test|live)_\w+|\w+_secret_\w+", "[REDACTED]", value)
    return value


def request(url, params=None, key=None, json_body=False):
    headers = {}
    data = None
    if params is not None:
        if json_body:
            data = json.dumps(params).encode()
            headers["Content-Type"] = "application/json"
        else:
            data = urllib.parse.urlencode(params).encode()
    if key:
        headers["Authorization"] = "Bearer " + key
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=headers), timeout=45) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        body = json.loads(error.read())
        raise ApiError(body) from None


def save(name, value):
    (EVIDENCE / (name + ".json")).write_text(json.dumps(redact(value), indent=2) + "\n")


def items():
    return [{"type": "one_time_price", "one_time_price": {"items": [
        {"price_data": {"currency": "usd", "unit_amount": amount, "tax_behavior": "exclusive",
                        "product_data": {"name": name}}, "quantity": quantity}
        for name, amount, quantity in (("Classic T-Shirt", 3500, 2), ("Zip-Up Hoodie", 5000, 1))
    ]}}]


def client_params(secret):
    return {"client_secret": secret, "locale": "en-US", "mobile_session_id": "adaptive-pricing-bug-bash",
            "mobile_app_id": PACKAGE}


def client(secret, endpoint, key, params=None):
    url = "https://api.stripe.com/v1/checkout_client/" + secret.split("_secret_")[0] + "/" + endpoint
    payload = {**client_params(secret), **(params or {})}
    if endpoint in ("init", "retrieve"):
        payload["timezone"] = "America/New_York"
    if endpoint == "retrieve":
        return request(url + "?" + urllib.parse.urlencode(payload), key=key)
    return request(url, payload, key)


def check_amounts(response, currency, adaptive):
    assert response["livemode"] is False
    assert response["integration_currency"] == "usd"
    assert response["presentment_currency"] == currency
    total = response["total_summary"]["total"]
    line_items = [item for group in response["items"] for item in group["one_time_price"]["items"]]
    assert all(item["price"]["currency"] == currency for item in line_items)
    assert sum(item["total"] for item in line_items) == total
    assert sum(item["subtotal"] for item in line_items) == response["total_summary"]["subtotal"]
    if adaptive:
        info = response["adaptive_pricing_info"]
        integration = info["integration_currency_option"]
        assert integration["currency"] == "usd"
        assert integration["amount"] == 12000
        options = [integration] + info["local_currency_options"]
        selected = next(option for option in options if option["currency"] == currency)
        assert selected["amount"] == total
    else:
        assert response.get("adaptive_pricing_info") is None
        assert total == 12000
    return total


def run_case(name, country, currency, allowed, confirm_currency, key):
    creation = {"ui_mode": "mobile_elements", "currency": "usd", "allowed_payment_method_types": ["card"],
                "customer_email": "test+location_" + country + "@example.com", "items": items()}
    session = request(BASE + "/create_checkout_session", {
        "merchant": "US", "stripe_version": "2026-08-26.preview", "request_params": creation,
    }, json_body=True)
    secret = session["client_secret"]
    save(name + "-creation", creation)
    response = client(secret, "init", key, {"adaptive_pricing[allowed]": str(allowed).lower()})
    save(name + "-init", response)
    total = check_amounts(response, currency if allowed else "usd", allowed)
    print(name + ": init " + response["presentment_currency"] + " " + str(total), flush=True)
    if not allowed:
        return {"case": name, "init_currency": "usd", "init_total": total, "confirmed": False}

    local_total = total
    for label, selected in (("usd", "USD"), ("local", currency.upper())):
        response = client(secret, "update", key, {"adaptive_pricing[currency]": selected})
        save(name + "-update-" + label, response)
        total = check_amounts(response, selected.lower(), True)
        if label == "local":
            assert total == local_total
        fetched = client(secret, "retrieve", key)
        save(name + "-retrieve-" + label, fetched)
        assert check_amounts(fetched, selected.lower(), True) == total
        print(name + ": update/retrieve " + selected + " " + str(total), flush=True)

    if confirm_currency == "usd":
        response = client(secret, "update", key, {"adaptive_pricing[currency]": "USD"})
        save(name + "-before-confirm", response)
        total = check_amounts(response, "usd", True)

    pm = request("https://api.stripe.com/v1/payment_methods", {
        "type": "card", "card[token]": "tok_visa", "billing_details[name]": "Adaptive Pricing Test",
        "billing_details[address][country]": country,
    }, key)
    assert pm["livemode"] is False
    confirmation = {"payment_method": pm["id"], "expected_payment_method_type": "card",
                    "expected_amount": total, "return_url": "stripesdk://payment_return_url/" + PACKAGE}
    save(name + "-confirm-request", confirmation)
    response = client(secret, "confirm", key, confirmation)
    save(name + "-confirm", response)
    check_amounts(response, confirm_currency, True)
    assert response["status"] == "complete"
    assert response["payment_status"] == "paid"
    reference = response["payment_response"]["payment_intent"]
    intent = request("https://api.stripe.com/v1/payment_intents/" + reference["id"] + "?" +
                     urllib.parse.urlencode({"client_secret": reference["client_secret"]}), key=key)
    save(name + "-payment-intent", intent)
    assert intent["status"] == "succeeded"
    try:
        fetched = client(secret, "retrieve", key)
        save(name + "-retrieve-complete", fetched)
        check_amounts(fetched, confirm_currency, True)
        assert fetched["status"] == "complete" and fetched["payment_status"] == "paid"
    except ApiError as error:
        save(name + "-retrieve-complete", error.body)
        assert error.body["error"]["code"] == "checkout_not_active_session"
    print(name + ": paid " + confirm_currency + " " + str(total) +
          "; PaymentIntent " + intent["currency"] + " " + str(intent["amount"]), flush=True)
    return {"case": name, "init_currency": currency, "init_total": local_total,
            "confirm_currency": confirm_currency, "confirm_total": total,
            "payment_intent_currency": intent["currency"], "payment_intent_amount": intent["amount"],
            "presentment_details": intent.get("presentment_details"), "confirmed": True}


if __name__ == "__main__":
    key = request(BASE + "/publishable_key?merchant=US")["publishable_key"]
    assert key.startswith("pk_test_"), "Refuse to create anything with a non-test key"
    results = []
    for case in (("france-disabled", "FR", "eur", False, None),
                 ("france-local", "FR", "eur", True, "eur"),
                 ("japan-local", "JP", "jpy", True, "jpy"),
                 ("japan-usd", "JP", "jpy", True, "usd")):
        results.append(run_case(*case, key))
        save("backend-results", results)
    print("All adaptive pricing backend controls passed.")
