# Project Overview

This project contains the source code for useful and usable software products around Gradle.
The source code is predominantly in Java and uses Gradle as the build tool.
Documentation is in asciidoc format.
The project is structured to support modularity and maintainability.

In the root of the repository, there are several PowerShell scripts that automate the process of maintaining the repository, including updating dependencies for the Gradle projects, updating the NPM packages that are used for the project.

The project is hosted on GitHub and several workflows are defined in the .github/workflows directory.

## Folder Structure


## Security - Zero Credentials Policy
No passwords, secrets, or any other sensitive information is stored in the repository.

**Zero-tolerance policy**: No credentials of any kind may be committed to version control, including test credentials.

### Enforcement
- Pre-commit hook in `.githooks/pre-commit` automatically blocks commits containing credentials
- Hook scans for patterns: password, secret, api_key, token, private_key, etc.
- No exceptions for test configuration files
- See `ZERO_CREDENTIALS_POLICY.md` for complete policy details

### Allowed Patterns (Documentation Only)
- **Placeholders**: `password: <your-password>` or `password: <DESCRIPTION>`
- **Environment variables**: `password: ${PASSWORD}`
- **Comments**: `password: # Set via environment variable`
- **Git hooks**: `.githooks/*` files (may contain pattern examples)
- **Copilot instructions**: `*copilot-instructions.md` (this file)
- **Security documentation**: Files matching `*SECURITY*.md`, `*POLICY*.md`, `*SECRET*.md`, `*.adoc`
- **Template files**: `*.example`, `*.sample`, `*.template`

### Configuration Files
All configuration files must use environment variable references:
```yaml
# ✅ CORRECT
spring:
  datasource:
    password: ${POSTGRES_PASSWORD}

# ❌ WRONG - Will be blocked by pre-commit hook
spring:
  datasource:
    password: EXAMPLE_PASSWORD
```

### Test Credentials
Test credentials are passed through from environment variables with **NO defaults**:
```gradle
tasks.withType(Test).configureEach {
    // NO defaults - must be set via .env file or environment
    environment 'POSTGRES_PASSWORD', System.getenv('POSTGRES_PASSWORD')
}
```

This approach ensures:
- Configuration files remain credential-free (no deployment risk)
- Build scripts have NO credential defaults (enforces explicit management)
- Local development uses `.env` file (in `.gitignore`, never committed)
- CI/CD sets credentials via secure secrets management
- No risk of weak test credentials leaking or being reused

### Production Secrets
- Store in GitHub Secrets for CI/CD workflows
- Use Azure Key Vault, AWS Secrets Manager, or similar for deployed applications
- Inject via environment variables at runtime
- Never commit `.env` files (already in `.gitignore`)

### Required Actions When Adding Secrets
1. Add placeholder to documentation: `SECRET_NAME: <description>` in `*.example` files
2. Reference in configuration: `SECRET_NAME: ${SECRET_NAME}`
3. For tests: Pass through from environment in `build.gradle` (NO defaults)
4. Document in `.env.example` file with example values
5. For production: Add to CI/CD secrets or secret management system
6. Update `.gitignore` to exclude `.env` files if not already present
7. Verify pre-commit hook detects hardcoded values before committing

These must be stored locally on the system generated as part of a GitHub workflow run, or in GitHub Secrets.
All secrets must be used through variable names which can be mapped to environment variables.

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

## GitHub Actions Workflows
- After modifying any workflow file in `.github/workflows/`, always run `actionlint` to validate the workflow syntax and dependencies
- Install actionlint: `brew install actionlint` (macOS) or download from https://github.com/rhysd/actionlint
- Run validation: `actionlint .github/workflows/*.yml`
- Fix any errors reported by actionlint before committing
- Common issues to check:
  - Job dependencies (`needs:`) must reference actual job names
  - Outputs must be accessible through the correct `needs.<job-name>.outputs.<output-name>` path
  - Required secrets and inputs must be defined in reusable workflows

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

### Workflow Validation

**CRITICAL: Always run actionlint after modifying GitHub workflow files.**

When creating or modifying any workflow file in `.github/workflows/`:

1. Make your changes to the workflow YAML file
2. Run `actionlint <workflow-file>.yml` to validate syntax and catch errors
3. Fix any errors reported by actionlint before committing
4. Only commit and push once actionlint passes successfully

This prevents broken workflows from being pushed to GitHub and causing CI/CD failures.

Example:
```bash
# After editing a workflow
actionlint .github/workflows/release_book.yml

# If no errors, proceed with commit
git add .github/workflows/release_book.yml
git commit -m "feat: update workflow"
```

ActionLint installation:
- macOS: `brew install actionlint`
- Linux: Download from https://github.com/rhysd/actionlint/releases
- Validates YAML syntax, GitHub Actions syntax, and runs shellcheck on embedded scripts

### Git Hooks

The project includes pre-commit hooks that validate workflow files before committing.

**Setup:**
```bash
./scripts/setup-hooks.sh
```

See [.githooks/README.md](/.githooks/README.md) for details.

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