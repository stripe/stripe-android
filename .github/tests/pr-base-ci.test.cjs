const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { test } = require('node:test');

const workflow = readFileSync(`${__dirname}/../workflows/pr-base-ci.yml`, 'utf8');
const script = workflow.split('          script: |\n')[1]
  .split('\n').map(line => line.replace(/^ {12}/, '')).join('\n');
const execute = new (Object.getPrototypeOf(async function () {}).constructor)(
  'github', 'context', 'core', script,
);
const block = '<!-- pr-base-ci:start -->\n[skip ci]\n' +
  'Bitrise is deferred until this PR targets master.\n<!-- pr-base-ci:end -->';
const override = '<!-- pr-base-ci:run -->';

function pr(base, body = 'Description', state = 'open') {
  return { number: 123, base: { ref: base }, body, state };
}

async function reconcile(current, event = current, changes = {}) {
  const gets = [];
  const updates = [];
  const github = { rest: { pulls: {
    get: async args => { gets.push(args); return { data: current }; },
    update: async args => { updates.push(args); },
  } } };
  await execute(github, {
    repo: { owner: 'stripe', repo: 'stripe-android' },
    payload: { pull_request: event, changes },
  }, { info() {} });
  assert.deepEqual(gets, [{ owner: 'stripe', repo: 'stripe-android', pull_number: 123 }]);
  for (const update of updates) {
    assert.equal(update.pull_number, 123);
    assert.equal(update.owner, 'stripe');
    assert.equal(update.repo, 'stripe-android');
  }
  return updates.map(update => update.body);
}

test('a PR targeting a parent branch gets a managed skip block', async () => {
  assert.deepEqual(await reconcile(pr('feature-parent')), [`Description\n\n${block}`]);
});

test('a standalone PR targeting master needs no annotation', async () => {
  assert.deepEqual(await reconcile(pr('master')), []);
});

test('retargeting to master removes only the managed block', async () => {
  assert.deepEqual(await reconcile(pr('master', `Description\n\n${block}`)), ['Description']);
});

test('manual skip annotations survive promotion to master', async () => {
  const body = 'Notes\n\n[ci skip]';
  assert.deepEqual(await reconcile(pr('master', `${body}\n\n${block}`)), [body]);
});

test('explicit run overrides keep CI enabled for a child', async () => {
  assert.deepEqual(await reconcile(pr('feature-parent', `Notes\n\n${override}`)), []);
});

test('intentional removal persists an override across later pushes', async () => {
  const current = pr('feature-parent');
  const result = await reconcile(current, current, { body: { from: `Description\n\n${block}` } });
  assert.deepEqual(result, [`Description\n\n${override}`]);
  assert.deepEqual(await reconcile(pr('feature-parent', result[0])), []);
});

test('a queued ordinary edit does not create an override after promotion', async () => {
  const current = pr('master', 'Updated notes');
  const event = pr('feature-parent', `Updated notes\n\n${block}`);
  assert.deepEqual(await reconcile(current, event, { body: { from: `Description\n\n${block}` } }), []);
});

test('the latest base and notes win over a stale event', async () => {
  assert.deepEqual(await reconcile(pr('master', 'New notes'), pr('feature-parent', 'Old notes')), []);
});

test('repeated reconciliation leaves an existing marker unchanged', async () => {
  assert.deepEqual(await reconcile(pr('feature-parent', `Description\n\n${block}`)), []);
});

test('a PR closed before the workflow runs is not edited', async () => {
  assert.deepEqual(await reconcile(pr('feature-parent', 'Description', 'closed')), []);
});
