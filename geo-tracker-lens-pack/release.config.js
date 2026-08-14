/**
 * @type {import('semantic-release').GlobalConfig}
 */

module.exports = {
    branches: ["main"],
    tagFormat: "geo-tracker-lens-pack-v${version}",
    plugins: [
        ["@semantic-release/commit-analyzer", {
            preset: 'angular',
            releaseRules: [
                { type: 'feat', release: 'minor' },
                { type: 'fix', release: 'patch' },
                { type: 'refactor', release: 'patch' },
                { type: 'improvement', release: 'minor' },
                { type: 'scenario', release: 'patch' },
                { type: 'maintenance', release: 'patch' },
                { breaking: true, release: 'major' }
            ],
            parserOpts: {
                noteKeywords: ['BREAKING CHANGE', 'BREAKING CHANGES', 'BREAKING'],
                breakingHeaderPattern: /^(\w*)(?:\((.*)\))?!: (.*)$/
            }
        }],
        ["@semantic-release/exec", {
            prepareCmd: "sed -i.bak 's/^version=.*/version=${nextRelease.version}/' gradle.properties && rm -f gradle.properties.bak"
        }],
        ["@semantic-release/changelog", {
            "changelogFile": "CHANGELOG.md",
            preset: 'angular',
            presetConfig: {
                types: [
                    { type: 'feat', section: '✨ Features', hidden: false },
                    { type: 'fix', section: '🐛 Bug Fixes', hidden: false },
                    { type: 'refactor', section: '♻️  Refactoring', hidden: false },
                    { type: 'improvement', section: '⬆️  Improvements', hidden: false },
                    { type: 'scenario', section: '📋 Scenarios', hidden: false },
                    { type: 'maintenance', section: '🔧 Misc', hidden: false },
                    { type: 'docs', section: '📝 Documentation', hidden: false },
                    { type: 'style', section: '💄 Styling', hidden: false },
                    { type: 'perf', section: '⚡ Performance', hidden: false },
                    { type: 'test', section: '✅ Tests', hidden: false },
                    { type: 'build', section: '🔨 Build System', hidden: true },
                    { type: 'ci', section: '👷 CI/CD', hidden: true },
                    { type: 'chore', section: '🔧 Misc', hidden: true },
                    { type: 'revert', section: '⏪ Reverts', hidden: false }
                ]
            }
        }],
        ["@semantic-release/git", {
            "assets": ["!**/node_modules/**", "!**/build/**", "!**/bin/**", "./CHANGELOG.md", "./gradle.properties"],
            "message": "chore(release): release <%= nextRelease.version %> - <%= new Date().toLocaleDateString('en-US', {year: 'numeric', month: 'short', day: 'numeric', hour: 'numeric', minute: 'numeric' }) %> [skip ci]\n\n<%= nextRelease.notes %>"
        }],
        "@semantic-release/github",
    ],
};