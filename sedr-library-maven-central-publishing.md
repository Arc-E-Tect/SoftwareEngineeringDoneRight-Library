# Publishing SEDR Library to Maven Central

This guide lists the step-by-step process to migrate `sedr_utils/sedr-library` publishing from GitHub Packages to Maven Central.

### Registration and Authentication
[X] 1. Create/sign in to Sonatype Central and claim ownership of your namespace (`com.arc_e_tect.sedr.utils`): <https://central.sonatype.com>.
[X] 2. Create a Central publishing token (username + password/token pair) in Sonatype Central.
[X] 3. Generate a GPG key pair for artifact signing and publish the public key to a keyserver.
[X] 4. Store all credentials securely in local Gradle properties (never commit credentials):
   ```properties
   # ~/.gradle/gradle.properties
   mavenCentralUsername=<your-central-token-username>
   mavenCentralPassword=<your-central-token-password>

   signingKey=<ascii-armored-private-key>
   signingPassword=<your-signing-key-password>
   ```
   *Note: Keep repository files credential-free. Use environment variables in CI/CD.*

### Project Configuration
[X] 5. Open `sedr_utils/sedr-library/build.gradle` (already has `maven-publish`).
[ ] 6. Add Gradle signing support and publish signed artifacts (`jar`, `sourcesJar`, `javadocJar`).
[ ] 7. Add a complete `pom { ... }` block to the `MavenPublication` with required Maven Central metadata:
   - `name`, `description`, `url`
   - `licenses`
   - `developers`
   - `scm`
[ ] 8. Replace the GitHub Packages repository in `publishing.repositories` with Maven Central deployment endpoint and credentials sourced from env vars/properties.
[X] 9. Keep `group` and `version` valid for public release in `sedr_utils/sedr-library/gradle.properties` (`group=com.arc_e_tect.sedr.utils` is already set).

### GitHub Actions Migration
[ ] 10. Update `.github/workflows/sedr-library-release.yml`:
   - Rename publish step from GitHub Packages to Maven Central.
   - Replace `GITHUB_ACTOR` / `GITHUB_TOKEN` usage with Maven Central + signing environment variables.
   - Keep zero-credentials policy: no inline secrets, no defaults in committed files.
[X] 11. Add required repository secrets in GitHub:
   - `MAVEN_CENTRAL_USERNAME`
   - `MAVEN_CENTRAL_PASSWORD`
   - `SIGNING_KEY`
   - `SIGNING_PASSWORD`
[ ] 12. Validate workflow syntax after editing workflow files:
   ```powershell
   actionlint .github/workflows/sedr-library-release.yml
   ```

### Validation and Publication
[ ] 13. Run a full build for the subproject:
   ```powershell
   Push-Location /Users/ieising/IdeaProjects/Book/SoftwareEngineeringDoneRight-Gradle/sedr_utils/sedr-library
   ./gradlew clean build --no-daemon --no-build-cache --info
   Pop-Location
   ```
[ ] 14. Validate publish artifacts locally first:
   ```powershell
   Push-Location /Users/ieising/IdeaProjects/Book/SoftwareEngineeringDoneRight-Gradle/sedr_utils/sedr-library
   ./gradlew publishToMavenLocal --no-daemon --no-build-cache --info
   Pop-Location
   ```
[ ] 15. Publish to Maven Central from CI (recommended) or locally:
   ```powershell
   Push-Location /Users/ieising/IdeaProjects/Book/SoftwareEngineeringDoneRight-Gradle/sedr_utils/sedr-library
   ./gradlew publish --no-daemon --no-build-cache --info
   Pop-Location
   ```
[ ] 16. Confirm release appears on Sonatype Central, then verify availability on Maven Central search and dependency resolution in a consumer project.

### Cutover and Cleanup
[ ] 17. Update any documentation that currently references GitHub Packages for `sedr-library` consumption.
[ ] 18. Keep GitHub Packages publishing disabled for this library after successful Maven Central migration to avoid split artifact sources.
[ ] 19. Tag/release as usual using your semantic version workflows once Maven Central publication is verified.

