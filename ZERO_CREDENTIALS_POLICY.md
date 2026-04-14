# Zero Credentials in Repository Policy

**Effective Date**: 2026-01-23  
**Policy Level**: Mandatory - No Exceptions

## Policy Statement

**No credentials of any kind may be committed to version control, including test credentials.**

This zero-tolerance policy eliminates the entire class of vulnerabilities related to:
- Accidental production deployment of test configurations
- Credential leakage through configuration file copying
- Security risks from "safe" test passwords being similar to production patterns

## Rationale

Even test credentials introduce vulnerabilities when:
1. Configuration files are accidentally deployed to production
2. Test configurations are copied and reused in production contexts
3. Developers use similar patterns for both test and production credentials
4. Configuration files are shared across environments

**The only safe credential is one that doesn't exist in version control.**

## Implementation

### Configuration Files

All configuration files use environment variable placeholders:

```yaml
# ✅ CORRECT - Uses environment variables
spring:
  datasource:
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}

# ❌ WRONG - Hardcoded test credentials (will be blocked by pre-commit hook)
spring:
  datasource:
    username: testuser
    password: testpassword
```

### Gradle Build Script

Test credentials are passed through from environment variables with **NO defaults**:

```gradle
tasks.withType(Test).configureEach {
    // Environment variables - MUST be set via .env file or shell environment
    // NO defaults to enforce zero-credentials policy
    environment 'SSL_KEYSTORE_PASSWORD', System.getenv('SSL_KEYSTORE_PASSWORD')
    environment 'POSTGRES_DB', System.getenv('POSTGRES_DB')
    environment 'POSTGRES_USER', System.getenv('POSTGRES_USER')
    environment 'POSTGRES_PASSWORD', System.getenv('POSTGRES_PASSWORD')
}
```

**Why NO defaults:**
- Even test defaults in build.gradle create security risks
- Forces explicit credential management from the start
- Prevents accidental use of weak test credentials in production-like environments
- CI/CD must explicitly configure credentials (no hidden defaults)

### Pre-Commit Hook Enforcement

The pre-commit hook blocks ALL credentials in ALL files:

```bash
# Pattern matches any credential-like key-value pair
secret_pattern='(password|passwd|pwd|secret|api[-_]?key|...)[[:space:]]*[:=][[:space:]]*[^<$#][^$#]{2,}'

# Only documentation files are exempt (.example, .sample, .template)
# Test configuration files are NOT exempt
```

## Allowed Patterns

### Documentation Only

These patterns are allowed **ONLY** in:
- Git hooks (`.githooks/*`) - may contain pattern examples in comments
- Copilot instructions (`*copilot-instructions.md`) - contains security policy documentation
- Security documentation files (matching `*SECURITY*.md`, `*POLICY*.md`, `*SECRET*.md`, `*.adoc`)
- Template files (`.example`, `.sample`, `.template`)

```yaml
# ✅ Placeholder syntax
password: <your-password>

# ✅ Environment variable syntax  
password: ${PASSWORD}

# ✅ Comment documentation
password: # Set this via environment variable

# ✅ Clear example (security docs only)
password: EXAMPLE_PASSWORD
```

## Development Workflow

### Running Tests Locally

Tests require credentials from a `.env` file or environment variables:

```bash
# Option 1: Use setup script (recommended - prompts for passwords or generates strong ones)
./setup-test-env.sh

# Option 2: Use setup script with environment variables (no prompts)
export H2_PASSWORD="your-strong-password"
export POSTGRES_PASSWORD="your-strong-password"
./setup-test-env.sh

# Option 3: Export environment variables directly
export SSL_KEYSTORE_PASSWORD=sedr-book
export H2_USERNAME=sa
export H2_PASSWORD="your-strong-password"
export POSTGRES_DB=familyties
export POSTGRES_USER=familyties
export POSTGRES_PASSWORD="your-strong-password"

# Option 4: Pass via Gradle CLI
./gradlew testE2E \
  -DSSL_KEYSTORE_PASSWORD=sedr-book \
  -DH2_PASSWORD=your-password \
  -DPOSTGRES_DB=familyties \
  -DPOSTGRES_USER=familyties \
  -DPOSTGRES_PASSWORD=your-password
```

The setup script (`./setup-test-env.sh`):
- Uses environment variables if already set
- Prompts for passwords if not set (secure, hidden input)
- Generates strong random passwords if you press Enter
- Creates `.env` file in the project root (gitignored)
- Never stores passwords in the git repository

### Setting Custom Credentials

For production-like testing, modify your `.env` file or environment variables:

```bash
export POSTGRES_PASSWORD=my-secure-password
./gradlew testE2E
```

Or create/edit the `.env` file (gitignored):

```bash
# .env (local only, never committed)
POSTGRES_PASSWORD=my-secure-password
SSL_KEYSTORE_PASSWORD=my-keystore-password
```

Then use with your test framework or Docker Compose.

### CI/CD Configuration

CI/CD systems should inject credentials as environment variables:

```yaml
# GitHub Actions example
- name: Run E2E Tests
  env:
    POSTGRES_PASSWORD: ${{ secrets.TEST_DB_PASSWORD }}
    SSL_KEYSTORE_PASSWORD: ${{ secrets.TEST_SSL_PASSWORD }}
  run: ./gradlew testE2E
```

## Consequences of Violation

### Pre-Commit Hook

Commits with credentials will be **automatically blocked**:

```
Error: Potential secret detected in app/src/testE2E/resources/application-e2e.yml
Matched lines:
5:    password: familyties

Secrets must not be committed. Use environment variables or .env files instead.
```

### Manual Override

Bypassing the hook is **strongly discouraged**:

```bash
git commit --no-verify  # ⚠️ DO NOT DO THIS
```

If you believe you have a legitimate case, discuss with the security team first.

## Exceptions

**There are no exceptions to this policy.**

- ❌ Not for test credentials
- ❌ Not for "safe" passwords
- ❌ Not for temporary configurations
- ❌ Not for development environments

The only allowed credentials in the repository are:
- ✅ Placeholders in template files (`.example`, `.sample`, `.template`)
- ✅ Environment variable references (e.g., `${PASSWORD}`)
- ✅ Examples in security documentation (`*SECURITY*.md`, `*POLICY*.md`, `*SECRET*.md`, `*.adoc`)
- ✅ Pattern examples in git hooks (`.githooks/*`)
- ✅ Policy documentation in Copilot instructions (`*copilot-instructions.md`)

## Benefits

1. **Zero production risk**: Test configurations cannot be accidentally deployed with credentials
2. **Simplified compliance**: Clear yes/no policy, no judgment calls required
3. **Better security hygiene**: Forces proper credential management from the start
4. **Easier auditing**: grep for credentials in repo should return zero results
5. **CI/CD ready**: Forces teams to use proper secret management systems

## Migration Guide

### For Existing Configuration Files

1. **Identify hardcoded credentials**:
   ```bash
   grep -r "password:" app/src/
   ```

2. **Replace with environment variables**:
   ```yaml
   # Before
   password: testpassword
   
   # After
   password: ${POSTGRES_PASSWORD}
   ```

3. **Add defaults to build.gradle**:
   ```gradle
   tasks.withType(Test).configureEach {
       environment 'POSTGRES_PASSWORD', System.getenv('POSTGRES_PASSWORD') ?: 'testpassword'
   }
   ```

4. **Test locally**:
   ```bash
   ./gradlew testE2E --no-daemon --no-build-cache
   ```

5. **Verify pre-commit hook**:
   ```bash
   git add .
   git commit -m "Remove hardcoded credentials"
   # Should succeed (no credentials detected)
   ```

## Comparison: Old vs New Approach

| Aspect | Old Approach (Exemptions) | New Approach (Zero Tolerance) |
|--------|--------------------------|-------------------------------|
| Test config files | Allowed test credentials | No credentials allowed |
| Security risk | Configuration files could be deployed | Zero deployment risk |
| Pre-commit hook | Exempted test/* directories | No exemptions |
| Credential location | Configuration files | Build scripts only |
| Auditability | Need to check exceptions | Simple: zero credentials |
| Developer experience | Works by default | Works by default (via Gradle) |

## Related Documentation

- [SECRETS_POLICY.md](SECRETS_POLICY.md) - How to use the secret management system
- [SECRETS_AUDIT.md](SECRETS_AUDIT.md) - Complete audit and implementation details
- [E2E_TESTING.md](E2E_TESTING.md) - E2E testing with proper secret management

## Questions?

### "How do I run tests without setting up environment variables?"

Tests use default values from `build.gradle` automatically. Just run `./gradlew testE2E`.

### "What if I need different credentials for different test scenarios?"

Override via environment variables:
```bash
POSTGRES_PASSWORD=scenario1 ./gradlew testE2E
```

### "Aren't build scripts also committed to git?"

Yes, but:
- Build scripts contain **code** (including default test values)
- Build scripts are **not deployed** (only artifacts are)
- This is fundamentally different from configuration files that could be deployed

### "What about local development?"

Default test credentials in `build.gradle` work for local development. For custom setups, use `.env` files (gitignored).

### "Can I use any password values I want?"

**YES! All passwords can be freely chosen.**

- ✅ **H2_PASSWORD**: Any value - H2 is in-memory, password just needs to match in config
- ✅ **POSTGRES_PASSWORD**: Any value - Docker Compose creates the database with your password
- ✅ **SSL_KEYSTORE_PASSWORD**: Any value - keystores are **generated locally** with your chosen password

**Important**: The SSL keystore files (`keystore.p12`) are:
- **NOT in git** (gitignored via `*.p12` pattern)
- **Generated locally** by `./setup-test-env.sh` or manually with `keytool`
- **Generated in CI/CD** workflows with secure environment variables
- Created with whatever `SSL_KEYSTORE_PASSWORD` you provide

To generate keystores with a specific password:

```bash
# Option 1: Let setup script generate them
export SSL_KEYSTORE_PASSWORD="MySecurePassword123"
./setup-test-env.sh

# Option 2: Generate manually
keytool -genkeypair -alias familyties -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -storepass YOUR_PASSWORD \
  -keypass YOUR_PASSWORD -validity 3650 \
  -dname "CN=localhost, OU=Test, O=FamilyTies, L=City, ST=State, C=US"

# Copy to test resource directories
cp keystore.p12 app/src/testE2E/resources/ssl/
cp keystore.p12 app/src/testSystem/resources/ssl/
cp keystore.p12 app/src/main/resources/
```

**Note**: If you set `SSL_KEYSTORE_PASSWORD` as an environment variable before running `setup-test-env.sh`, the script will use that value and add a comment in the `.env` file warning you not to change it (since the keystores were already generated with that password).

---

**Remember**: The safest credential is the one that never makes it into version control.
