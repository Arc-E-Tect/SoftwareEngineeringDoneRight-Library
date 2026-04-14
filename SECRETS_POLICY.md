# Secrets Management Policy

## Overview

This project enforces strict policies to prevent secrets from being committed to version control. Secrets include passwords, API keys, tokens, private keys, and other sensitive credentials.

## Pre-Commit Hook Protection

The repository includes a pre-commit hook (`.githooks/pre-commit`) that automatically scans staged files for potential secrets before allowing a commit.

### What is Detected

The hook scans for common secret patterns including:
- `password`, `passwd`, `pwd`
- `secret`, `api_key`, `api-key`
- `auth_token`, `access_token`, `bearer_token`
- `private_key`, `client_secret`, `oauth_token`
- `session_secret`, `encryption_key`, `signing_key`
- `jwt_secret`

### What is Allowed

The following patterns are **allowed** and will not trigger the hook:

#### 1. Placeholders
```yaml
password: <your-password>
api_key: <YOUR_API_KEY>
```

#### 2. Environment Variables
```yaml
password: ${PASSWORD}
secret: ${API_SECRET}
```

#### 3. Security Documentation Files Only
- Git hooks: `.githooks/*` (may contain pattern examples)
- Copilot instructions: `*copilot-instructions.md` (contains security policy)
- Security-related markdown/asciidoc files: `*SECURITY*.md`, `*POLICY*.md`, `*SECRET*.md`, `*.adoc`
- Example: `SECRETS_POLICY.md`, `SECRETS_AUDIT.md`, `SECURITY.md`, `ZERO_CREDENTIALS_POLICY.md`
- Files ending in `.example` (e.g., `.env.example`)
- Files ending in `.sample` (e.g., `config.sample`)
- Files ending in `.template` (e.g., `secrets.template`)

### What is Blocked

Any file with actual secret values will be rejected:

```yaml
# ❌ BLOCKED - hardcoded password
password: EXAMPLE_PASSWORD

# ❌ BLOCKED - hardcoded API key
api_key: EXAMPLE_API_KEY_VALUE

# ❌ BLOCKED - hardcoded token
bearer_token: EXAMPLE_TOKEN_eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
```

## How to Store Secrets Properly

### For Local Development

1. Create a `.env` file (already in `.gitignore`):
   ```bash
   cp .env.example .env
   ```

2. Add your actual secrets to `.env`:
   ```bash
   DATABASE_PASSWORD=your_real_password
   API_KEY=your_real_api_key
   ```

3. Reference them as environment variables in configuration:
   ```yaml
   database:
     password: ${DATABASE_PASSWORD}
   api:
     key: ${API_KEY}
   ```

### For Production

- Use environment variables injected by your deployment platform
- Use secret management services (AWS Secrets Manager, Azure Key Vault, etc.)
- Use CI/CD secret storage (GitHub Secrets, GitLab CI/CD Variables, etc.)

## Setting Up the Pre-Commit Hook

The hook is activated when you first clone the repository and run:

```bash
./scripts/setup-hooks.sh
```

This configures git to use `.githooks/` for all git hooks.

## Testing the Hook

To verify the hook is working:

### Test 1: Try to commit a file with a secret
```bash
# Create a test file with a secret
echo "password: secretValue123" > test-secret.txt
git add test-secret.txt
git commit -m "Test secret detection"
```

**Expected Result**: Commit is blocked with an error message.

### Test 2: Try to commit with a placeholder
```bash
# Create a file with a placeholder
echo "password: <your-password>" > test-placeholder.txt
git add test-placeholder.txt
git commit -m "Test placeholder"
```

**Expected Result**: Commit succeeds.

### Test 3: Try to commit with an environment variable
```bash
# Create a file with an environment variable
echo "password: \${PASSWORD}" > test-envvar.txt
git add test-envvar.txt
git commit -m "Test environment variable"
```

**Expected Result**: Commit succeeds.

## Bypassing the Hook (Emergency Only)

If you absolutely must bypass the hook (not recommended):

```bash
git commit --no-verify -m "Emergency commit"
```

**⚠️ Warning**: This bypasses all security checks. Only use in genuine emergencies and ensure no secrets are committed.

## False Positives

If the hook incorrectly flags something as a secret:

1. **Use placeholders**: `password: <your-password>`
2. **Use environment variables**: `password: ${PASSWORD}`
3. **Use example files**: Rename to `.env.example` or `config.sample`
4. **Add inline comment**: `password: # Set via environment variable`

## Reporting Issues

If you find a legitimate case that should be allowed but is being blocked, or vice versa, please:

1. Document the specific case
2. Explain why it should/shouldn't be blocked
3. Open an issue or submit a PR to update `.githooks/pre-commit`
