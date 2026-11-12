"""Real test-mode order-summary probes; no native UI orchestration.

Run python3 bug-bash/order-summary/live_probe.py [details|tax|currency|boundaries].
Reuses the prior topic's redacting transport without modifying its evidence.
"""
import importlib.util
import json
from pathlib import Path
import sys
import urllib.parse

OUT = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('email_transport', OUT.parent / 'email-collection/live_probe.py')
transport = importlib.util.module_from_spec(spec)
spec.loader.exec_module(transport)
BASE, PACKAGE = transport.BASE, transport.PACKAGE
request, ApiError = transport.request, transport.ApiError
SF = {'country': 'US', 'line1': '510 Townsend St', 'city': 'San Francisco', 'state': 'CA', 'postal_code': '94103'}
OR = {'country': 'US', 'line1': '1120 SW 5th Ave', 'city': 'Portland', 'state': 'OR', 'postal_code': '97204'}
IMAGE = 'https://stripe.com/img/v3/home/twitter.png'


def save(name, value):
    (OUT / (name + '.json')).write_text(json.dumps(transport.redact(value), indent=2) + '\n')


def backend(params, merchant):
    return request(BASE + '/create_checkout_session', {'merchant': merchant,
        'stripe_version': '2026-08-26.preview', 'request_params': params}, json_body=True)


def client(secret, endpoint, key, params=None):
    return transport.client(secret, endpoint, key, {'mobile_session_id': 'order-summary-bash', **(params or {})})


def key_for(merchant):
    key = request(BASE + '/publishable_key?' + urllib.parse.urlencode({'merchant': merchant}))['publishable_key']
    assert key.startswith('pk_test_'), 'Only test mode is authorized'
    return key


def item(name, value, qty, currency='usd', decimal=False, behavior=None, product=None, **extra):
    price = {'currency': currency, 'unit_amount_decimal' if decimal else 'unit_amount': value,
             'product_data': {'name': name, **(product or {})}}
    if behavior:
        price['tax_behavior'] = behavior
    return {'price_data': price, 'quantity': qty, **extra}


def group(items, **extra):
    return {'type': 'one_time_price', 'one_time_price': {'items': items, **extra}}


def public_amounts(details):
    return {**details, 'tax_amounts': [
        {'amount': t['amount'], 'inclusive': t['inclusive'],
         'display_name': t['tax_rate']['display_name'], 'percentage': t['tax_rate']['percentage']}
        for t in details['tax_amounts']]}


def summary(response):
    assert response['livemode'] is False
    rows = [i for g in response['items'] for i in g['one_time_price']['items']]
    totals = response['total_summary']
    for field in ('total', 'subtotal', 'tax_inclusive', 'tax_exclusive'):
        assert sum(i['amount_details'][field] for i in rows) == totals[field], (field, totals)
    assert totals['total'] == totals['subtotal'] + totals['tax_exclusive']
    return {'status': response['status'], 'currency': response['presentment_currency'], 'totals': totals,
            'groups': [g['key'] for g in response['items']], 'items': [
                {'key': i['inner_item_key'], 'name': i['price']['product']['name'], 'quantity': i['quantity'],
                 'unit_amount': i['unit_amount'], 'unit_amount_decimal': i['unit_amount_decimal'],
                 'unit_label': i.get('unit_label'), 'images': i['price']['product']['images'],
                 'product_description': i['price']['product'].get('description'),
                 'adjustable_quantity': i.get('adjustable_quantity'), 'amount_details': public_amounts(i['amount_details'])}
                for i in rows]}


def create(name, key, groups, currency='usd', merchant='US', **extra):
    params = {'ui_mode': 'mobile_elements', 'currency': currency, 'allowed_payment_method_types': ['card'],
              'automatic_tax': {'enabled': False}, 'adaptive_pricing': {'enabled': False},
              'customer_email': 'order-summary-bash@example.com', 'items': groups, **extra}
    save(name + '-creation', params)
    try:
        secret = backend(params, merchant)['client_secret']
    except ApiError as error:
        save(name + '-error', error.body)
        raise
    initial = client(secret, 'init', key)
    save(name + '-init', initial)
    before = summary(initial)
    fetched = client(secret, 'retrieve', key)
    save(name + '-retrieve', fetched)
    assert summary(fetched) == before
    print(name + ' init/retrieve: ' + json.dumps(before), flush=True)
    return secret, initial


def update(name, secret, key, address, previous):
    params = {'tax_region[' + k + ']': v for k, v in address.items()}
    save(name + '-request', params)
    current = client(secret, 'update', key, params)
    save(name, current)
    current_summary = summary(current)
    fetched = client(secret, 'retrieve', key)
    save(name + '-retrieve', fetched)
    assert summary(fetched) == current_summary
    assert current_summary['groups'] == summary(previous)['groups']
    assert [i['key'] for i in current_summary['items']] == [i['key'] for i in summary(previous)['items']]
    print(name + ': ' + json.dumps(current_summary['totals']), flush=True)
    return current


def confirm(name, secret, key, before, shipping=None):
    pm = request('https://api.stripe.com/v1/payment_methods', {'type': 'card', 'card[token]': 'tok_visa',
        'billing_details[name]': 'Order Summary Test',
        **{'billing_details[address][' + k + ']': v for k, v in SF.items()}}, key)
    assert pm['livemode'] is False
    params = {'payment_method': pm['id'], 'expected_payment_method_type': 'card',
              'expected_amount': before['total_summary']['total'], 'return_url': 'stripesdk://payment_return_url/' + PACKAGE}
    if shipping:
        params['shipping[name]'] = 'Order Summary Recipient'
        params.update({'shipping[address][' + k + ']': v for k, v in shipping.items()})
    save(name + '-confirm-request', params)
    result = client(secret, 'confirm', key, params)
    save(name + '-confirm', result)
    expected = summary(before)
    expected['status'] = 'complete'
    assert summary(result) == expected
    assert result['status'] == 'complete' and result['payment_status'] == 'paid'
    ref = result['payment_response']['payment_intent']
    pi = request('https://api.stripe.com/v1/payment_intents/' + ref['id'] + '?' +
                 urllib.parse.urlencode({'client_secret': ref['client_secret']}), key=key)
    save(name + '-payment-intent', pi)
    assert pi['status'] == 'succeeded' and pi['amount'] == expected['totals']['total']
    assert pi['currency'] == before['presentment_currency']
    print(name + ' paid: ' + str(pi['amount']) + ' ' + pi['currency'], flush=True)
    return {'case': name, **expected, 'payment_intent_status': pi['status'], 'payment_intent_amount': pi['amount']}


def details():
    key = key_for('US')
    rows = [item('Café shirt — blue & white 100%', 1299, 3, product={
        'description': 'Soft cotton\nBlue edition', 'images': [IMAGE], 'unit_label': 'shirt'},
        adjustable_quantity={'enabled': True, 'minimum': 0, 'maximum': 12}),
        item('Ribbon', '12.345678901234', 100, decimal=True, product={'unit_label': 'meter'}),
        item('Complimentary gift', 0, 1)]
    secret, before = create('details', key, [group(rows)])
    save('details-results', confirm('details', secret, key, before))


def tax():
    key = key_for('us_tax')
    rows = [item('Tax included item', 1000, 2, behavior='inclusive', product={'tax_code': 'txcd_99999999'}),
            item('Second inclusive item', 2500, 1, behavior='inclusive', product={'tax_code': 'txcd_99999999'})]
    secret, before = create('inclusive-tax', key, [group(rows)], merchant='us_tax', automatic_tax={'enabled': True},
                             shipping_address_collection={'allowed_countries': ['US']})
    assert before['automatic_tax']['address_source'] == 'shipping'
    current = update('inclusive-tax-sf', secret, key, SF, before)
    assert current['total_summary']['tax_inclusive'] > 0 and current['total_summary']['tax_exclusive'] == 0
    current = update('inclusive-tax-or', secret, key, OR, current)
    assert current['total_summary']['tax_inclusive'] == 0 and current['total_summary']['tax_exclusive'] == 0
    current = update('inclusive-tax-sf-restored', secret, key, SF, current)
    save('inclusive-tax-results', confirm('inclusive-tax', secret, key, current, SF))


def currency():
    key = key_for('US')
    rows = [item('Icelandic item', 12300, 2, currency='isk'),
            item('Icelandic second item', 5000, 1, currency='isk')]
    secret, before = create('isk', key, [group(rows)], currency='isk')
    save('isk-results', confirm('isk', secret, key, before))


def boundaries():
    key = key_for('US')
    results = []
    for name, groups in (
        ('multiple-groups', [group([item('Group one', 1000, 2)]), group([item('Group two', 500, 3)])]),
        ('group-description', [group([item('Product', 1000, 1)], description='Group description')]),
        ('zero-quantity', [group([item('Control', 1000, 1), item('Optional product', 500, 0,
            adjustable_quantity={'enabled': True, 'minimum': 0, 'maximum': 10})])]),
    ):
        try:
            _, before = create(name, key, groups)
            results.append({'case': name, 'accepted': True, **summary(before)})
        except ApiError as error:
            save(name + '-error', error.body)
            results.append({'case': name, 'accepted': False, 'error': error.body})
        save('boundary-results', results)
        print(json.dumps(results[-1]), flush=True)


if __name__ == '__main__':
    {'details': details, 'tax': tax, 'currency': currency, 'boundaries': boundaries}[sys.argv[1]]()
