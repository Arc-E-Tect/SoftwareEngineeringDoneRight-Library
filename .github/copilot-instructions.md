# Project Overview

This project contains the source code for useful and usable software products around Gradle.
The source code is predominantly in Java and uses Gradle as the build tool.
Documentation is in asciidoc format.
The project is structured to support modularity and maintainability.

In the root of the repository, there are several PowerShell scripts that automate the process of maintaining the repository, including updating dependencies for the Gradle projects, updating the NPM packages that are used for the project.

The project is hosted on GitHub and several workflows are defined in the .github/workflows directory.

## Folder Structure

## Security: zero credentials policy

No passwords, secrets, private keys, tokens, credentials, or sensitive production data may be committed to version control, including test credentials.
`ZERO_CREDENTIALS_POLICY.md` is the policy, and `SECURITY.md` lists the controls that enforce it.

- Use environment variables or a deployment secret manager for all secrets.
- Use placeholders such as `${API_TOKEN}` or `<API_TOKEN_DESCRIPTION>` in documentation and configuration templates.
- Never assign secret-like configuration values literal defaults, including in tests.
- Keep `.env` files local and ignored. Only sanitized `.env.example`, `.env.sample`, or `.env.template` files may be committed.
- Never disclose a credential in source, documentation, logs, issues, pull requests, or generated artifacts.
- Store CI/CD secrets in GitHub Actions secrets. The credentials for the Gradle Plugin Portal and Maven Central, and the signing keys, belong in the `release` environment.

When a new secret is required:

1. Document its name and purpose in a sanitized example file.
2. Read it through an environment variable or a secret-manager integration.
3. Add the relevant local file pattern to `.gitignore`.
4. Verify the staged-content checks in `.githooks/pre-commit` reject a hardcoded value.
5. Update `SECURITY.md`, `ZERO_CREDENTIALS_POLICY.md` or other operational documentation when the handling policy changes.

## Git hooks

Install the repository hooks before contributing:

```bash
./scripts/setup-hooks.sh
```

The hooks provide local defense in depth by rejecting keystores and likely hardcoded secrets, linting modified GitHub Actions workflows, rejecting npm manifests with a UTF-8 byte order mark, and preventing pushes to branches with closed or merged pull requests. They do not replace server-side controls and must not be bypassed except for a documented, reviewed reason. `.githooks/README.md` describes each check.

## GitHub Actions

- Keep the default `GITHUB_TOKEN` read-only. Every workflow declares `permissions: contents: read` at the top, and a job raises only what its own use of `GITHUB_TOKEN` needs. A job that authenticates with a personal access token gets no write grant.
- All external actions in `uses:` must be pinned to a full 40-character commit SHA, which the repository enforces. Include a version comment beside the SHA.
- Use only GitHub-owned or verified-creator actions unless the repository's allowed-actions policy is deliberately extended and documented.
- Every job that runs on a runner declares `timeout-minutes`. Every workflow that is not only reusable declares a `concurrency` group: builds cancel superseded runs, releases queue.
- Every `actions/checkout` sets `persist-credentials: false`, unless a later step in the same job pushes with git; such a checkout carries a comment that says so.
- A job that uses the Gradle Plugin Portal, Maven Central or signing secrets declares `environment: release`.
- Run `actionlint` after changing a workflow, and fix all reported errors before committing.
- Do not use `pull_request_target` with untrusted pull-request code. Do not expose secrets to pull-request workflows from forks.
- Use immutable action, container, and dependency versions. Do not use mutable `latest` tags.

## Commits and pull requests

- Commits follow Conventional Commits and are signed; `.github/git-commit-instructions.md` gives the types and scopes.
- When a commit contains code that Claude changed, its message ends with the trailer `Co-authored-by: Claude <noreply@anthropic.com>`.
- Work on a branch and merge to `main` through a pull request, which is squash-merged. Do not bypass the repository ruleset.
- Create Git worktrees in `../worktrees-<repo name>/`, never inside the checkout.
- Before pushing an existing branch, check that its pull request is still open. Do not push additional commits to a merged or closed pull-request branch.

## Environments

## Libraries and Frameworks

## Versioning

- Semantic Versioning is used for versioning the projects using the `angular` preset.

## Build and test
- Always verify with `./gradlew clean build --no-daemon --no-build-cache --info`; rerun suites that are touched if the config cache is invalidated.
- Keep the Gradle `testing { suites { ... } }` DSL as the single source of truth for test tasks (unit, component, contract, system, e2e, architecture). New suites must be added there, not with ad-hoc `Test` tasks.
- Ensure Cucumber suites (for example, `testSystem`, `testE2E`, but there may be more) keep producing HTML + JSON outputs under `build/reports/cucumber/<suite>/` using the configured `cucumber.plugin` system property.
- Honor the existing logging setup (FAILED/SKIPPED with full exceptions and standard streams shown).
- Never disclose secrets, when a secret or password is required, assume it is defined as an environment variable.

## Dependencies and versions
- All dependencies must come from `gradle/libs.versions.toml` using `version.ref`; no inline versions or direct coordinates in `build.gradle`.
- WireMock, Testcontainers, and any new libs must be added to the catalog first, then referenced via `libs.<alias>`.
- Default dependency aliases in `libs.versions.toml` must include `iff` across all alias categories (`libraries`, `bundles`, `plugins`, and `versions`).
- Subproject-specific aliases must include the subproject name in the alias (for example, `system-admin`).
- Do not target artifact versions that are not published on Maven Central.
- Gradle dependency version management is done by using the Gradle plugin 'refreshVersions'. After applying the plugin, run `./gradlew refreshVersions` to retrieve the latests versions of every dependncy. After retrieving the updated versions, update `libs.versions.toml` to use the latest versions.
- After adding a new dependency to the Gradle build file, always double check that it is defined using `libs.<alias>` in `gradle/libs.versions.toml` and then run `./gradlew refreshVersions` to ensure the latest version is used.

## Testing/runtime behavior

## Project rules
- Maintain configuration cache compatibility when altering Gradle logic.
- Keep README/test READMEs aligned with any change in commands, secrets, or images.
- Prefer good examples over prescriptive snippets: show what to avoid (e.g., inline coordinates, manual Test tasks) and what to adopt (catalog aliases, test suites DSL).

## .gitignore rules for Gradle projects

**CRITICAL: Always ensure `gradle-wrapper.jar` is tracked in git.**

The standard Java `.gitignore` template includes `*.jar`, which silently excludes `gradle-wrapper.jar`. Without this file committed, `./gradlew` fails immediately on CI runners (even after `Setup Gradle` succeeds) because the JVM cannot find `GradleWrapperMain`.

When editing `.gitignore` in any Gradle project, always verify or add the negation exception:
```
*.jar
!**/gradle/wrapper/gradle-wrapper.jar
```

After adding this rule:
1. Run `git check-ignore -v */gradle/wrapper/gradle-wrapper.jar` to confirm it is no longer ignored.
2. Run `git add */gradle/wrapper/gradle-wrapper.jar` to ensure the file is tracked.

## Docker images
When pulling images from Docker Hub, always prefer the latest version available with a version number. When there is an `alpine` variant available, prefer that over the default. Do not use `latest` tags as the version to pull.

For example:
Prefer `wiremock/wiremock:3.13.2-2-alpine` over `wiremock/wiremock:3.13.2-2` and prefer `wiremock/wiremock:3.13.2-2` over `wiremock/wiremock:latest`.

## Branch Push Policy

**CRITICAL: Never push to a branch that has no remote tracking branch without explicit user confirmation.**

Before pushing any commits, always check whether the current branch tracks a remote:
```bash
git branch -vv | grep "^\*"
```

- If the output shows `[origin/<branch>]` — the branch has a remote, safe to push normally.
- If no `[origin/...]` is shown — the branch is **local-only**. Do **not** run `git push` or `git push -u origin <branch>`. Instead, inform the user that no remote exists and ask whether they want to publish the branch.

This prevents accidentally creating unwanted remote branches on shared repositories.

## Pull Request Management

**CRITICAL: Always check if a PR is still open before pushing to its branch.**

When working with feature branches and pull requests:

1. **Before pushing to an existing branch**, check if the PR is still open:
   ```bash
   gh pr view <branch-name> --json state --jq .state
   ```

2. **If the PR is merged or closed**, create a new branch and PR:
   ```bash
   git checkout main
   git pull
   git checkout -b <new-branch-name>
   git cherry-pick <commit-hash>  # If you need to move commits
   git push -u origin <new-branch-name>
   gh pr create
   ```

3. **Never push commits to a branch whose PR has already been merged** - this causes confusion and the commits won't be reviewed.

Example workflow:
```bash
# Check PR status before pushing
PR_STATE=$(gh pr view fix-something --json state --jq .state 2>&1)

if echo "$PR_STATE" | grep -q "MERGED\|CLOSED"; then
  echo "PR is already merged/closed, creating new branch"
  git checkout main && git pull
  git checkout -b fix-something-v2
  # Make changes and push
else
  echo "PR is still open, safe to push"
  git push
fi
```

## Available Tools

The following command-line tools are installed and available for use:

### GitHub CLI (`gh`)
The GitHub CLI is installed and authenticated. Use it for:
- Creating and managing issues, pull requests, and releases
- Querying repository information
- Automating GitHub workflows
- Examples: `gh issue list`, `gh pr create`, `gh release create`

### Node Package Manager (`npm`)
npm is installed and can be used for:
- Installing and managing Node.js packages
- Running npm scripts defined in package.json
- Testing semantic-release locally: `npx semantic-release --dry-run --no-ci`
- Examples: `npm install`, `npm ci`, `npx <package>`

## Automation notes

- Any generated or edited GitHub Actions workflows must pass the pre-commit checks (actionlint with shellcheck). Prefer quoting variables, avoiding inline exports with assignments, and capturing stderr when parsing outputs.
- All shell scripts (`.sh`) must start with `#!/usr/bin/env bash`.
- All PowerShell scripts (`.ps1`) must start with `#!/usr/bin/env pwsh` to be directly executable on macOS/Linux.
- 
