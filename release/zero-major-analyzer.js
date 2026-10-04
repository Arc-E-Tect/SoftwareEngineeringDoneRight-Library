// A breaking change releases a minor version before 1.0.0, and a major one from 1.0.0 on.
//
// A 0.x version is still in development: any release may break compatibility without a major
// version bump. semantic-release knows nothing of that, and a breaking change would take a 0.x
// version to 1.0.0. So while the last release is 0.x, a major release becomes a minor one; from
// 1.0.0 on, when compatibility is what a version promises, a breaking change releases a major
// version. Reaching 1.0.0 is a decision, never the side effect of a commit.
//
// releaseType() is the rule, which every analyzer beside this file applies. As a plugin, this
// file stands in for @semantic-release/commit-analyzer in a release.config.js, with the same
// options.
//
// The analyzer is resolved from process.cwd(), where semantic-release runs and its dependencies
// are installed, rather than from this file, and only when commits are analysed, so the rule can
// be tested from anywhere.
const commitAnalyzer = () => require(require.resolve('@semantic-release/commit-analyzer', { paths: [process.cwd()] }));

/** The release `type` an analyzer chose, given the last released version: never major before 1.0.0. */
function releaseType(type, lastVersion) {
  return type === 'major' && Number.parseInt(lastVersion, 10) === 0 ? 'minor' : type;
}

module.exports = {
  analyzeCommits: async (pluginConfig, context) =>
    releaseType(await commitAnalyzer().analyzeCommits(pluginConfig, context), (context.lastRelease || {}).version),
  commitAnalyzer,
  releaseType
};
