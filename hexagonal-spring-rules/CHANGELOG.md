# [1.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/hexagonal-spring-rules-v0.5.0...hexagonal-spring-rules-v1.0.0) (2026-08-17)


### ✨ New and updated features

* **api-detector-core:** add ContractProgressTableWriter for shared Progress Over Time reporting ([#59](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/59)) ([dd4d36a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/dd4d36a66b9368db625ae60f3076c55a437f2417)), closes [#59](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/59)
* add geo-tracker-lens-pack library with independent release pipeline ([#60](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/60)) ([1730931](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/1730931112ca9f592c26ae3352e0557dc2b7a6c7)), closes [#60](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/60)
* **api-detector-core:** add PathTemplates.stripBasePath and OpenApiEndpointCollector.firstServerBasePath ([#66](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/66)) ([865e711](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/865e71197d01e32ee8d96e52eae9c661fbe9501f)), closes [#66](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/66)
* **api-detector-core:** add published shared library for the SEDR API detector plugins ([#52](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/52)) ([059ee4d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/059ee4d07b3f7a4aac359087046105a89a9afc4f)), closes [#52](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/52)
* **api-detector-core:** add ScanProgressReporter for visible long-scan progress ([#56](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/56)) ([9357bc9](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9357bc90a5726e792100c96d3c01e4bb0ed5efee)), closes [#56](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/56)
* **api-detector-core:** add shared contract-lifecycle progress history framework ([#53](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/53)) ([0bb104e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/0bb104ea3b6ea4fee11299000a725ce57cff365d)), closes [#53](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/53)
* **api-detector-core:** separate real implementation evidence from stub evidence in contract history ([#64](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/64)) ([0775933](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/0775933ed474bd56960f8adc4e1810f35c0f402e)), closes [#64](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/64)
* **api-detector-core:** write and tolerate a schema-version marker ([#65](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/65)) ([e27d53f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/e27d53f35342df40c9473e7ec5a08e0dc55d6e56)), closes [#65](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/65)


### 🐛 Bug Fixes

* **ci:** detect first semantic release version in calculate workflow ([#61](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/61)) ([3a80fe1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/3a80fe1389591e9bff33b7aae6f81c5c1dad3e3a)), closes [#61](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/61)
* **workflow:** fix tag checking for project-specific tag formats ([#54](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/54)) ([ce8f57e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ce8f57eee20c97f727f068b8404f22fe2dbe4f6b)), closes [#54](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/54)
* **hexagonal-spring-rules:** give module its own semantic-release tag namespace ([#67](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/67)) ([613a69a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/613a69ac3caeb5c372ead3529ed13107e1b04320)), closes [#67](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/67)
* **examples:** remove mavenLocal now that dependencies are released ([#42](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/42)) ([9b20913](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9b209137b7e4f16dc64b7980f52338b80fec9404)), closes [#42](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/42)
* **ci:** stabilize security scan reporting and SEDR scan scope ([#49](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/49)) ([f5c7df2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/f5c7df2ce7911c241abd859e1cb9a2a55379c8fa)), closes [#49](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/49)
* **CI:** stop the NVD cache refresh from timing out on every cold sync ([#46](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/46)) ([cbaebf3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/cbaebf318f235d49b2c2b9e4b2b8cbd97efcc27e)), closes [#46](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/46)
* tag checking for project-specific tag formats in workflow ([#55](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/55)) ([9e9255e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9e9255eaed5bd16c4a83377073b444c73f5a89cd)), closes [#55](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/55)
* **geo-tracker-lens-pack:** use https scm connection for Maven Central metadata ([#63](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/63)) ([5bb98c0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/5bb98c0a5404cfc73ab24e240faa14cdd55955ec)), closes [#63](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/63)


### 📝 Documentation

* **api-detector-core:** update README version to 0.1.0 [skip ci] ([14c5f16](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/14c5f165dbcdb7cf5347cef29ba8708c93eb3444))
* **api-detector-core:** update README version to 0.2.0 [skip ci] ([2f68414](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/2f68414924323241bc547ad74669a2fc2daa1b71))
* **api-detector-core:** update README version to 0.3.0 [skip ci] ([9c63eac](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9c63eacd0b067ac5cec8f8178a56ae13da1ce414))
* **hexagonal-spring-rules:** update README version to 0.5.0 [skip ci] ([5c8b553](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/5c8b553fc89232747372f771e3faa2ac0b79316a))
* **sedr-library:** update README version to 0.5.1 [skip ci] ([bb4becf](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/bb4becf8f5ec15770f765b80182cadb1cd933915))
* **sedr-library:** update README version to 0.5.2 [skip ci] ([8320bb8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/8320bb800eca06faa82eb5b412b70b50f75158eb))
* **api-detector-core:** update README version to 1.0.0 [skip ci] ([ebb1c5f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ebb1c5f34010a47c92ee94e1536c43d89092686f))
* **geo-tracker-lens-pack:** update README version to 1.0.0 [skip ci] ([ab46583](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ab4658331fd2ad7a5e6c2ea0553caf3ff2976eb2))
* **geo-tracker-lens-pack:** update README version to 1.0.1 [skip ci] ([1ac70e4](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/1ac70e48d1bbce838c4d2ff5f5f16de66ff384ab))
* **api-detector-core:** update README version to 1.1.0 [skip ci] ([1680e7b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/1680e7b6bdb4233a3c056711fa2777412017b3ac))
* **api-detector-core:** update README version to 1.2.0 [skip ci] ([6656c7f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/6656c7f2ee73c09c083fb62cc25a953f2c874234))


### 🔧 Misc

* Change NVD cache refresh schedule to weekly ([#45](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/45)) ([83c3552](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/83c3552048f491c02ab4f0bc8faf824145496675)), closes [#45](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/45)
* dependency updates for Library projects ([#43](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/43)) ([fa70e5e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/fa70e5e4f460e585169560ff1de8934ab988ee82)), closes [#43](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/43)
* dependency updates for Library projects ([#47](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/47)) ([398933c](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/398933c66f272be3598471f31ffb5d108be685bd)), closes [#47](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/47)
* dependency updates for Library projects ([#48](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/48)) ([105111d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/105111d488070ee639077d28c89bfdeec592840a)), closes [#48](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/48)
* dependency updates for Library projects ([#50](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/50)) ([2d5d3ce](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/2d5d3ce836654222630c503b76a11a7d59499cb7)), closes [#50](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/50)
* dependency updates for Library projects ([#58](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/58)) ([83dff89](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/83dff89406e7e025a7b96f3d62ac2c2006458de2)), closes [#58](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/58)
* Make workflow triggers more specific and add progress reporter ([#57](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/57)) ([10c64dc](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/10c64dc344e4e9c25886b80781d3661111d42a72)), closes [#57](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/57)
* **ci:** remove redundant Monday NVD cache refresh schedule ([#51](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/51)) ([ffd937f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ffd937ff02ddb20a4eff7235fb155a3d431d1460)), closes [#51](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/51)
* **geo-tracker-lens-pack:** trigger release pipeline after CI fix ([b2440cc](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/b2440cc8d036a5cddcbba0d5a6c6f8cec62db039))
* **Sedr Library:** update npm packages ([#44](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/44)) ([821da37](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/821da37c9407fcc7010614271f10420665abfc43)), closes [#44](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/44)


### BREAKING CHANGE

* **api-detector-core:** contractHistoryFile written by a previous version of this
library (9 fields, no stubbedAt) is no longer readable by
ContractHistoryStore.load() - it now throws
LegacyContractHistoryFormatException instead of loading it under the old,
now-ambiguous implementedAt semantics. Consumers must migrate existing
history files (mirage-api-detector's new migrateContractHistory task) or
start a fresh history file before upgrading.

# [0.5.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.4.7...v0.5.0) (2026-07-18)


### ✨ New and updated features

* **hexagonal-spring-rules:** add new architecture rules and examples ([#41](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/41)) ([8a0a226](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/8a0a2262f0d74f9d646ab1c0711e297865556cff)), closes [#41](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/41)


### 📝 Documentation

* **sedr-library:** update README version to 0.4.7 [skip ci] ([4b590b1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/4b590b1e1d6c24b54a7499b90341d3e7c9eb2267))

## [0.4.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.4.0...v0.4.1) (2026-06-12)


### 🐛 Bug Fixes

* example package names ([#24](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/24)) ([4adef98](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/4adef9809a6278adeac29ca344a843f408a9c94a)), closes [#24](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/24)


### 🔧 Misc

* update architecture-validator-hexagonal-spring-rules-iff version to 0.4.0 ([#23](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/23)) ([119287c](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/119287c7cb3a574616b58c4dfa912d7a2fbb0116)), closes [#23](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/23)

# [0.4.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.3.4...v0.4.0) (2026-06-11)


### ✨ New and updated features

* add Hexagonal Architecture Spring Rules validation ([#22](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/22)) ([569dabc](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/569dabcd0b6b124a9af15ad248a550f504956cf1)), closes [#22](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/22)
* **examples:** add refreshVersions and libs.versions.toml to jacoco-marker example ([#21](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/21)) ([659e206](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/659e20660fe5ee6aa210fc63c03752f3050ed8aa)), closes [#21](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/21)


### 📝 Documentation

* **readme:** update version to 0.3.4 [skip ci] ([6ceaee8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/6ceaee819b38ebde1858cc3fb8df73ce68d62ca1))


### 🔧 Misc

* **dependencyUpdate:** update Library repo workflow references using the Updater ([#20](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/20)) ([1d176ad](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/1d176ad72c686265146a8fe22b0f4931a1451d88)), closes [#20](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/20)

# Changelog
