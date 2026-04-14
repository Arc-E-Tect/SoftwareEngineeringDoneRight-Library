# Publishing Jacoco Exclusion Report Plugin to Gradle Plugin Marketplace

This guide lists the step-by-step process to publish the `jacoco-exclusion-report` Gradle plugin to the [Gradle Plugin Portal](https://plugins.gradle.org/).

### Registration and Authentication
[X] 1. Create an account on the [Gradle Plugin Portal](https://plugins.gradle.org/user/register).
[X] 2. Log in and navigate to your user profile to find your API Keys.
[X] 3. Copy the `gradle.publish.key` and `gradle.publish.secret`.
[X] 4. Store these keys securely. It is recommended to add them to your `~/.gradle/gradle.properties` file:
   ```properties
   gradle.publish.key=<your-key>
   gradle.publish.secret=<your-secret>
   ```
   *Note: Do not commit these keys to the repository.*

### Project Configuration
[X] 5. Open `sedr_utils/jacoco-exclusion-report/build.gradle`.
[X] 6. Add the Gradle Plugin Publish plugin to the `plugins` block:
   ```gradle
   plugins {
       id 'java-gradle-plugin'
       id 'maven-publish'
       id 'com.gradle.plugin-publish' version '1.2.1' // Check for the latest version
   }
   ```
[X] 7. Enhance the `gradlePlugin` block with mandatory metadata:
   ```gradle
   gradlePlugin {
       website = 'https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle'
       vcsUrl = 'https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Gradle.git'
       plugins {
           jacocoExclusionReport {
               id = 'com.arc-e-tect.jacoco-exclusion-report'
               displayName = 'JaCoCo Exclusion Report'
               description = 'Scans sources for @ExcludeFromJacocoGeneratedCodeCoverage and generates an HTML+XML report.'
               tags = ['jacoco', 'coverage', 'report', 'exclusion']
           }
       }
   }
   ```
[X] 8. Ensure the version and group are correctly set in the project (either in `build.gradle` or `settings.gradle`).

### Publication
[X] 9. Run the validation task to ensure the plugin meets the requirements:
   ```bash
   ./gradlew :jacoco-exclusion-report:validatePlugins
   ```
[ ] 10. Publish the plugin to the marketplace:
   ```bash
   ./gradlew :jacoco-exclusion-report:publishPlugins
   ```
[ ] 11. Wait for the Gradle team to review and approve the plugin (usually takes a few days for the first version).
