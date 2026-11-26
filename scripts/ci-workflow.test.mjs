import assert from 'node:assert/strict';
import { readFileSync, mkdtempSync, writeFileSync, chmodSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';

const workflow = readFileSync(new URL('../.github/workflows/ci.yml', import.meta.url), 'utf8');
const group = workflow.match(/^  group: (.+)$/m)[1];
// Evaluate the workflow's actual expression for both pre-merge and post-merge refs.
function key(event, ref, number) {
  const github = { event_name: event, ref, event: { pull_request: { number } } };
  return group.replace(/\$\{\{(.*?)\}\}/g, (_, expression) =>
    Function('github', `return (${expression});`)(github));
}

test('merged PR runs cannot cancel target branch pushes', () => {
  for (const branch of ['main', 'develop']) {
    const ref = `refs/heads/${branch}`;
    assert.notEqual(key('push', ref), key('pull_request', ref, 107));
  }
});

test('PR actions share a stable group across merge, distinct PRs and branches stay separate', () => {
  assert.equal(key('pull_request', 'refs/pull/107/merge', 107), key('pull_request', 'refs/heads/main', 107));
  assert.notEqual(key('pull_request', 'refs/heads/main', 107), key('pull_request', 'refs/heads/main', 108));
  assert.notEqual(key('push', 'refs/heads/main'), key('push', 'refs/heads/develop'));
  assert.match(workflow, /cancel-in-progress: true/);
  assert.match(workflow, /types: \[opened, synchronize, reopened, edited, labeled, unlabeled\]/);
});

const plan = workflow.match(/        run: \|\n([\s\S]*?)\n\n  build:/)[1]
  .split('\n').map(line => line.slice(10)).join('\n');

function runPlan(action, result, extra = {}) {
  const dir = mkdtempSync(join(tmpdir(), 'ci-plan-'));
  try {
    const output = join(dir, 'output');
    const calls = join(dir, 'calls');
    writeFileSync(join(dir, 'gh'), `#!/bin/sh
echo "$*" >> "$CALLS"
case "$*" in
  *'head_sha=fixture-head&event=pull_request&status=completed'*) echo 123 ;;
  *'/runs/123/jobs?'*) echo "$RESULT" ;;
  *) exit 1 ;;
esac
`);
    chmodSync(join(dir, 'gh'), 0o755);
    writeFileSync(output, '');
    const process = spawnSync('bash', ['-e', '-c', plan], { encoding: 'utf8', env: {
      ...globalThis.process.env, PATH: `${dir}:${globalThis.process.env.PATH}`,
      EVENT: 'pull_request', ACTION: action, BASE_CHANGED: 'false',
      HEAD_SHA: 'fixture-head', RUN_ID: '456', REPO: 'example/project',
      RESULT: result, CALLS: calls, GITHUB_OUTPUT: output, ...extra,
    } });
    assert.equal(process.status, 0, process.stderr);
    return readFileSync(output, 'utf8');
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
}

test('metadata actions reuse completed success and failure for the same head', () => {
  for (const action of ['edited', 'labeled', 'unlabeled']) {
    for (const result of ['success', 'failure']) {
      assert.equal(runPlan(action, result), `run-build=false\nreused-result=${result}\n`);
    }
  }
});

test('missing, cancelled, skipped builds and base changes require a fresh build', () => {
  for (const result of ['', 'cancelled', 'skipped']) {
    assert.equal(runPlan('edited', result), 'run-build=true\n');
  }
  assert.equal(runPlan('edited', 'success', { BASE_CHANGED: 'true' }), 'run-build=true\n');
  for (const action of ['opened', 'synchronize', 'reopened']) {
    assert.equal(runPlan(action, 'success'), 'run-build=true\n');
  }
  assert.equal(runPlan('', 'success', { EVENT: 'push' }), 'run-build=true\n');
});
