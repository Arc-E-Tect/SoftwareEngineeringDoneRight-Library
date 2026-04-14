module.exports = {
    branches: ["main"],
    tagFormat: "v${version}",
    initialVersion: "0.0.0",
    plugins: [
        "@semantic-release/commit-analyzer",
        "@semantic-release/release-notes-generator",
        ["@semantic-release/exec", {
            prepareCmd: "sed -i.bak 's/^version=.*/version=${nextRelease.version}/' gradle.properties && rm -f gradle.properties.bak"
        }],
        ["@semantic-release/changelog", { changelogFile: "CHANGELOG.md" }],
        ["@semantic-release/git", {
            assets: ["CHANGELOG.md", "gradle.properties"],
            message: "chore(release): release ${nextRelease.version} [skip ci]"
        }],
        "@semantic-release/github"
    ]
};
