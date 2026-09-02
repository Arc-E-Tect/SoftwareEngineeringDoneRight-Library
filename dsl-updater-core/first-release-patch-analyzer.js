const commitAnalyzer = require(require.resolve('@semantic-release/commit-analyzer', { paths: [process.cwd()] }));

// This module is seeded with a baseline tag (api-detector-core-v0.0.0) specifically so
// semantic-release finds a lastRelease and increments from it instead of falling back to its
// own hardcoded "no prior tag at all -> 1.0.0" behavior. But that seed tag is a placeholder, not
// a real release: once it's present, context.lastRelease is truthy, so without this check the
// real commit-analyzer would run and a feat commit would compute a minor bump (0.1.0) instead of
// the intended first-release 0.0.1. So we force 'patch' both when there's no lastRelease at all
// AND when the only lastRelease is the 0.0.0 seed - after the real 0.0.1 release exists, normal
// analysis takes over again.
const SEED_VERSION = '0.0.0';

module.exports = {
  analyzeCommits: async (pluginConfig, context) => {
    if (!context.lastRelease || !context.lastRelease.version || context.lastRelease.version === SEED_VERSION) {
      return 'patch';
    }
    return commitAnalyzer.analyzeCommits(pluginConfig, context);
  }
};
