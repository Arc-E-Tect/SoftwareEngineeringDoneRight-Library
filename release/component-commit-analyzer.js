// The version of a module, from the commits that change it: see component-commits.js.
// Each module's release.config.js names this file where it named
// @semantic-release/commit-analyzer, with the same options; this hands those options, and a
// context holding only the module's own commits, to the real analyzer.
//
// Resolved from process.cwd() rather than from this file: semantic-release runs in the
// module's directory, and the dependency is installed in the module's own node_modules, not
// beside this file.
const commitAnalyzer = require(require.resolve('@semantic-release/commit-analyzer', { paths: [process.cwd()] }));
const { forComponent } = require('./component-commits');

module.exports = {
  analyzeCommits: async (pluginConfig, context) => commitAnalyzer.analyzeCommits(pluginConfig, forComponent(context))
};
