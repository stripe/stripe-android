const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { test } = require('node:test');

const workflow = readFileSync(`${__dirname}/../workflows/stack-ci.yml`, 'utf8');
const script = workflow.split('          script: |\n')[1]
  .split('\n').map(line => line.replace(/^ {12}/, '')).join('\n');
const execute = new (Object.getPrototypeOf(async function () {}).constructor)(
  'github', 'context', 'core', script,
);
const block = '<!-- stack-ci:start -->\n[skip ci]\n' +
  'Bitrise is deferred until this PR is the first layer of its stack.\n<!-- stack-ci:end -->';
const override = '<!-- stack-ci:run -->';

function pr(number, position, size, body = 'Description') {
  return { number, state: 'open', body, stack: position === null ? null : { position, size } };
}

async function reconcile(prs, payload = {}, fresh = prs) {
  const updates = [];
  const github = {
    paginate: async () => prs,
    rest: { pulls: {
      list: () => {},
      get: async ({ pull_number }) => ({ data: fresh.find(p => p.number === pull_number) }),
      update: async update => { updates.push(update); },
    } },
  };
  await execute(github, { repo: { owner: 'stripe', repo: 'stripe-android' }, payload }, { info() {} });
  return updates.map(({ pull_number, body }) => ({ number: pull_number, body }));
}

test('only position one stays eligible in a three-layer stack', async () => {
  assert.deepEqual(await reconcile([pr(1, 1, 3), pr(2, 2, 3), pr(3, 3, 3)]), [
    { number: 2, body: `Description\n\n${block}` },
    { number: 3, body: `Description\n\n${block}` },
  ]);
});

test('promotion removes the managed marker while the remaining child stays skipped', async () => {
  assert.deepEqual(await reconcile([
    pr(2, 1, 2, `Description\n\n${block}`),
    pr(3, 2, 2, `Description\n\n${block}`),
  ]), [{ number: 2, body: 'Description' }]);
});

test('detachment removes only the owned block and preserves manual skip markers', async () => {
  const body = 'Merchant notes\n\n[ci skip]';
  assert.deepEqual(await reconcile([pr(1, null, null, `${body}\n\n${block}`)]), [
    { number: 1, body },
  ]);
});

test('non-stack PRs, including feature-branch PRs, retain their descriptions', async () => {
  assert.deepEqual(await reconcile([pr(1, null, null, '[skip ci]\nMy notes'), pr(2, null, null)]), []);
});

test('manual skips and explicit run overrides are preserved on child PRs', async () => {
  assert.deepEqual(await reconcile([
    pr(1, 2, 2, '[skip ci]\nMy notes'),
    pr(2, 2, 2, `My notes\n\n${override}`),
  ]), []);
});

test('intentional marker removal persists a run override for future reconciliations', async () => {
  const payload = { pull_request: { number: 2, body: 'Description' }, changes: { body: { from: `Description\n\n${block}` } } };
  const updates = await reconcile([pr(2, 2, 2)], payload);
  assert.deepEqual(updates, [{ number: 2, body: `Description\n\n${override}` }]);
  assert.deepEqual(await reconcile([pr(2, 2, 2, updates[0].body)]), []);
});

test('latest metadata and description win over the initial listing', async () => {
  assert.deepEqual(await reconcile([pr(2, 2, 2)], {}, [pr(2, 1, 2, 'New notes')]), []);
});

test('invalid metadata fails before any write', async () => {
  await assert.rejects(reconcile([pr(1, 2, 2), pr(2, 0, 2)]), /Invalid stack metadata/);
});

test('empty descriptions receive a block without extra spacing', async () => {
  assert.deepEqual(await reconcile([pr(2, 2, 2, null)]), [{ number: 2, body: block }]);
});

test('a queued ordinary edit does not turn later promotion into a manual override', async () => {
  const payload = {
    pull_request: { number: 2, body: `Updated notes\n\n${block}` },
    changes: { body: { from: `Description\n\n${block}` } },
  };
  assert.deepEqual(await reconcile([pr(2, 1, 2, 'Updated notes')], payload), []);
});
