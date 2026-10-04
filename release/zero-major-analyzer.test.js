// Run with: node --test release/*.test.js
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const { analyzeCommits, releaseType } = require('./zero-major-analyzer');

test('before 1.0.0 a breaking change releases a minor version', () => {
    assert.equal(releaseType('major', '0.12.0'), 'minor');
    assert.equal(releaseType('major', '0.0.1'), 'minor');
});

test('from 1.0.0 on a breaking change releases a major version', () => {
    assert.equal(releaseType('major', '1.0.0'), 'major');
    assert.equal(releaseType('major', '3.12.24'), 'major');
    assert.equal(releaseType('major', '10.0.0'), 'major');
});

test('the first release, with no version before it, is left as the commits say', () => {
    assert.equal(releaseType('major', undefined), 'major');
});

test('anything short of a breaking change releases as the commits say, before 1.0.0 and after', () => {
    for (const version of ['0.12.0', '1.0.0']) {
        assert.equal(releaseType('minor', version), 'minor');
        assert.equal(releaseType('patch', version), 'patch');
        assert.equal(releaseType(null, version), null);
    }
});

test('as a plugin it hands its options and context to the commit analyzer, and applies the rule to its answer', async () => {
    // A commit analyzer installed where semantic-release runs, that releases a major version.
    const cwd = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'zero-major-analyzer-')));
    const analyzer = path.join(cwd, 'node_modules', '@semantic-release', 'commit-analyzer');
    fs.mkdirSync(analyzer, { recursive: true });
    fs.writeFileSync(path.join(analyzer, 'package.json'), '{"name":"@semantic-release/commit-analyzer","main":"index.js"}');
    fs.writeFileSync(path.join(analyzer, 'index.js'),
        'module.exports = { analyzeCommits: async (config, context) => { global.seen = { config, context }; return "major"; } };');
    const before = process.cwd();
    process.chdir(cwd);
    try {
        const context = { lastRelease: { version: '0.4.1' }, commits: [] };
        assert.equal(await analyzeCommits({ preset: 'angular' }, context), 'minor');
        assert.deepEqual(global.seen, { config: { preset: 'angular' }, context });
        assert.equal(await analyzeCommits({}, { lastRelease: { version: '1.2.3' } }), 'major');
        assert.equal(await analyzeCommits({}, {}), 'major');
    } finally {
        process.chdir(before);
    }
});
