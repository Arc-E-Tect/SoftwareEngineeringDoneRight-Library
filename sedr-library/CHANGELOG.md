# [1.0.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/sedr-library-v0.5.2...sedr-library-v1.0.0) (2026-08-21)


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

* **hexagonal-spring-rules:** align dependency-check config with sibling projects ([#71](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/71)) ([769f76a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/769f76a9434bd11469189eab2c8d8dbc74112a12)), closes [#71](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/71)
* **ci:** detect first semantic release version in calculate workflow ([#61](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/61)) ([3a80fe1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/3a80fe1389591e9bff33b7aae6f81c5c1dad3e3a)), closes [#61](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/61)
* **workflow:** fix tag checking for project-specific tag formats ([#54](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/54)) ([ce8f57e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ce8f57eee20c97f727f068b8404f22fe2dbe4f6b)), closes [#54](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/54)
* **hexagonal-spring-rules:** give module its own semantic-release tag namespace ([#67](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/67)) ([613a69a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/613a69ac3caeb5c372ead3529ed13107e1b04320)), closes [#67](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/67)
* **sedr-library:** give module its own semantic-release tag namespace ([#68](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/68)) ([ff2eb0d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ff2eb0d44c14d245cb6d8ab7075de29044dc209f)), closes [#68](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/68) [#67](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/67)
* tag checking for project-specific tag formats in workflow ([#55](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/55)) ([9e9255e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9e9255eaed5bd16c4a83377073b444c73f5a89cd)), closes [#55](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/55)
* **api-detector-core:** update swagger-parser dependency and document OpenAPI v3 support ([#70](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/70)) ([352b03e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/352b03e3d4881b653fd7d394a3eaf3d9d25da7bd)), closes [#70](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/70)
* **geo-tracker-lens-pack:** use https scm connection for Maven Central metadata ([#63](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/63)) ([5bb98c0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/5bb98c0a5404cfc73ab24e240faa14cdd55955ec)), closes [#63](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/63)


### 📝 Documentation

* **api-detector-core:** update README version to 0.1.0 [skip ci] ([14c5f16](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/14c5f165dbcdb7cf5347cef29ba8708c93eb3444))
* **api-detector-core:** update README version to 0.2.0 [skip ci] ([2f68414](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/2f68414924323241bc547ad74669a2fc2daa1b71))
* **api-detector-core:** update README version to 0.3.0 [skip ci] ([9c63eac](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9c63eacd0b067ac5cec8f8178a56ae13da1ce414))
* **sedr-library:** update README version to 0.5.2 [skip ci] ([8320bb8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/8320bb800eca06faa82eb5b412b70b50f75158eb))
* **api-detector-core:** update README version to 1.0.0 [skip ci] ([ebb1c5f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ebb1c5f34010a47c92ee94e1536c43d89092686f))
* **geo-tracker-lens-pack:** update README version to 1.0.0 [skip ci] ([ab46583](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ab4658331fd2ad7a5e6c2ea0553caf3ff2976eb2))
* **hexagonal-spring-rules:** update README version to 1.0.0 [skip ci] ([140934e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/140934e3826c25a66f46a2023e9234830e69c191))
* **geo-tracker-lens-pack:** update README version to 1.0.1 [skip ci] ([1ac70e4](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/1ac70e48d1bbce838c4d2ff5f5f16de66ff384ab))
* **hexagonal-spring-rules:** update README version to 1.0.1 [skip ci] ([0c115a7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/0c115a740e22ca7151034c2e31e58e98511e4702))
* **api-detector-core:** update README version to 1.1.0 [skip ci] ([1680e7b](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/1680e7b6bdb4233a3c056711fa2777412017b3ac))
* **api-detector-core:** update README version to 1.2.0 [skip ci] ([6656c7f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/6656c7f2ee73c09c083fb62cc25a953f2c874234))
* **api-detector-core:** update README version to 1.2.1 [skip ci] ([b4230b2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/b4230b254bf6fe7b1114844a639e127235b0499b))


### 🔧 Misc

* dependency updates for Library projects ([#58](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/58)) ([83dff89](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/83dff89406e7e025a7b96f3d62ac2c2006458de2)), closes [#58](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/58)
* dependency updates for Library projects ([#69](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/69)) ([d46b898](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/d46b8989dc5df17e9b8baf946a5bff48a26c6a10)), closes [#69](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/69)
* dependency updates for Library projects ([#72](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/72)) ([2f7c387](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/2f7c38719bcb98ae9f4cff9fc0f5dd0ef6db7969)), closes [#72](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/72)
* Make workflow triggers more specific and add progress reporter ([#57](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/57)) ([10c64dc](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/10c64dc344e4e9c25886b80781d3661111d42a72)), closes [#57](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/57)
* **ci:** remove redundant Monday NVD cache refresh schedule ([#51](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/51)) ([ffd937f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ffd937ff02ddb20a4eff7235fb155a3d431d1460)), closes [#51](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/51)
* **geo-tracker-lens-pack:** trigger release pipeline after CI fix ([b2440cc](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/b2440cc8d036a5cddcbba0d5a6c6f8cec62db039))


### BREAKING CHANGE

* **api-detector-core:** contractHistoryFile written by a previous version of this
library (9 fields, no stubbedAt) is no longer readable by
ContractHistoryStore.load() - it now throws
LegacyContractHistoryFormatException instead of loading it under the old,
now-ambiguous implementedAt semantics. Consumers must migrate existing
history files (mirage-api-detector's new migrateContractHistory task) or
start a fresh history file before upgrading.

## [0.5.2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.5.1...v0.5.2) (2026-08-08)


### 📝 Documentation

* **sedr-library:** update README version to 0.5.1 [skip ci] ([bb4becf](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/bb4becf8f5ec15770f765b80182cadb1cd933915))


### 🔧 Misc

* dependency updates for Library projects ([#50](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/50)) ([2d5d3ce](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/2d5d3ce836654222630c503b76a11a7d59499cb7)), closes [#50](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/50)

## [0.5.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.5.0...v0.5.1) (2026-08-08)


### 🐛 Bug Fixes

* **examples:** remove mavenLocal now that dependencies are released ([#42](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/42)) ([9b20913](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9b209137b7e4f16dc64b7980f52338b80fec9404)), closes [#42](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/42)
* **ci:** stabilize security scan reporting and SEDR scan scope ([#49](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/49)) ([f5c7df2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/f5c7df2ce7911c241abd859e1cb9a2a55379c8fa)), closes [#49](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/49)
* **CI:** stop the NVD cache refresh from timing out on every cold sync ([#46](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/46)) ([cbaebf3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/cbaebf318f235d49b2c2b9e4b2b8cbd97efcc27e)), closes [#46](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/46)


### 📝 Documentation

* **hexagonal-spring-rules:** update README version to 0.5.0 [skip ci] ([5c8b553](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/5c8b553fc89232747372f771e3faa2ac0b79316a))


### 🔧 Misc

* Change NVD cache refresh schedule to weekly ([#45](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/45)) ([83c3552](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/83c3552048f491c02ab4f0bc8faf824145496675)), closes [#45](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/45)
* dependency updates for Library projects ([#43](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/43)) ([fa70e5e](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/fa70e5e4f460e585169560ff1de8934ab988ee82)), closes [#43](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/43)
* dependency updates for Library projects ([#47](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/47)) ([398933c](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/398933c66f272be3598471f31ffb5d108be685bd)), closes [#47](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/47)
* dependency updates for Library projects ([#48](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/48)) ([105111d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/105111d488070ee639077d28c89bfdeec592840a)), closes [#48](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/48)
* **Sedr Library:** update npm packages ([#44](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/44)) ([821da37](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/821da37c9407fcc7010614271f10420665abfc43)), closes [#44](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/44)

## [0.4.7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.4.6...v0.4.7) (2026-07-18)


### 📝 Documentation

* Update READMEs and refine workflows for better maintainability ([#40](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/40)) ([c56cc7c](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/c56cc7c59c5478149e5a3186235d8a2a75983bff)), closes [#40](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/40)
* **readme:** update version to 0.4.6 [skip ci] ([db25786](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/db25786e19c4624a789ec692f0212be9da8bf408))


### 🔧 Misc

* **docs:** update README with project structure, usage details, and latest version info ([#39](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/39)) ([3c69e89](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/3c69e89e1d964eb5a6780d9245dfbbcba08daf2c)), closes [#39](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/39)

## [0.4.6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.4.5...v0.4.6) (2026-07-12)


### 📝 Documentation

* **readme:** update version to 0.4.5 [skip ci] ([cac98d6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/cac98d6bf3172a08db03acb32b58fbb552bb2f58))


### 🔧 Misc

* **workflows:** align NVD cache and action pinning policies ([#32](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/32)) ([869930d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/869930d102ec4368a3ee728d24537938747ef3db)), closes [#32](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/32)
* dependency updates for Library projects ([#33](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/33)) ([1706662](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/17066622985c9bcf233593d7020121612be68099)), closes [#33](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/33)
* dependency updates for Library projects ([#38](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/38)) ([325d587](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/325d5871f2624d840401db6f045e894467ea66d7)), closes [#38](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/38)
* Update documentation and Java toolchain version ([#37](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/37)) ([93996b0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/93996b0bcc977ec202884443e85ab4c5cffae48b)), closes [#37](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/37)
* **Single Module Spring:** update Java toolchain version and enforce architecture rules ([#36](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/36)) ([12dda27](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/12dda27605a55248ea07388dc309ffbdfc02d075)), closes [#36](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/36)

## [0.4.5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.4.4...v0.4.5) (2026-07-01)


### 📝 Documentation

* **readme:** update version to 0.4.4 [skip ci] ([ede1fb8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/ede1fb8fc15c372da228f0ace0719f65c0cb22d6))


### 🔧 Misc

* dependency updates for Library projects ([#31](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/31)) ([241de96](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/241de96276fbbea8cd4737c6517beb074e0627f4)), closes [#31](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/31)
* remove NVD cache update from the standard security scan and move it to a weekly process. ([#29](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/29)) ([14b1c72](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/14b1c7288d3283cfc4a5ed5bd8e06c6dc30e8e16)), closes [#29](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/29)
* Update NVD cache refresh process and adjust workflows ([#30](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/30)) ([fe48966](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/fe48966fc1e475a419f24098b51b6bce80ae98ad)), closes [#30](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/30)

## [0.4.4](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.4.3...v0.4.4) (2026-06-28)


### 📝 Documentation

* **readme:** update version to 0.4.3 [skip ci] ([9dc98f7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9dc98f7d345040d4d77ebf938c3383d4cdca755d))


### 🔧 Misc

* dependency updates for Library projects ([#28](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/28)) ([9c26433](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9c26433c54b0cf52c275c9264a556ad80381be42)), closes [#28](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/28)

## [0.4.3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.4.2...v0.4.3) (2026-06-21)


### 📝 Documentation

* **readme:** update version to 0.4.2 [skip ci] ([18162dd](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/18162dde30e26651d58a072d627b41d070996559))


### 🔧 Misc

* dependency updates for Library projects ([#27](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/27)) ([8e736e5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/8e736e51348b7a5b1175cd46f94a64a4b15f2bd4)), closes [#27](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/27)

## [0.4.2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.4.1...v0.4.2) (2026-06-21)


### 🔧 Misc

* dependency updates for Library projects ([#25](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/25)) ([a32470f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/a32470fa265f76519e5ad66cf4553dba10358203)), closes [#25](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/25)
* enhance JaCoCo integration and documentation for exclusion annotation ([#26](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/26)) ([90ee826](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/90ee82696e1de3ce9b062296ce9b3e3002d3a033)), closes [#26](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/26)

## [0.3.4](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.3.3...v0.3.4) (2026-05-25)


### 📝 Documentation

* **readme:** update version to 0.3.3 [skip ci] ([9a081ba](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9a081bac6b33468b670063ececd636874610071f))


### 🔧 Misc

* **Sedr Library:** update java dependency versions ([#19](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/19)) ([cc20654](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/cc20654095ee56d41cdfb70db4ff2243e8843d84)), closes [#19](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/19)

## [0.3.3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.3.2...v0.3.3) (2026-05-18)


### 📝 Documentation

* **readme:** update version to 0.3.2 [skip ci] ([f40df48](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/f40df48b8793a2bcf8b96b3922b4353e1ac0ca79))


### 🔧 Misc

* **Sedr Library:** update gradle wrapper to 9.5.1 ([#18](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/18)) ([cf4a5d0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/cf4a5d0b45403c083c81d6b45b3ed2a219384eb9)), closes [#18](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/18)
* update npm packages ([#17](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/17)) ([9de53bb](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/9de53bb0c39ac5826a101ac15658642b31b16cc0)), closes [#17](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/17)
* **workflow:** update paths for sedr-library release workflow ([0d56ca3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/0d56ca381b6fecfd17a3e31299f158b780acb9de))

## [0.3.2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.3.1...v0.3.2) (2026-05-04)


### 🐛 Bug Fixes

* **workflow:** implement concurrency control for semantic version calculation ([#16](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/16)) ([dae8349](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/dae83497179ef908ed0dd6742e8a769aa94efc68)), closes [#16](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/16)


### 📝 Documentation

* **readme:** update version to 0.3.1 [skip ci] ([c564217](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/c56421708f55fcfdf945ef059ab2702c0cc0a9a4))

## [0.3.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.3.0...v0.3.1) (2026-05-04)


### 📝 Documentation

* **readme:** update version to 0.3.0 [skip ci] ([0d59afb](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/0d59afbf0fbb5bb93d5687ee71a1a7e2451e348b))


### 🔧 Misc

* dependency updates for sedr-library ([#15](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/15)) ([788e251](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/788e251a5e6584e17dc270159db95ce795bccd03)), closes [#15](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/15)

# [0.3.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.2.6...v0.3.0) (2026-05-04)


### ✨ New and updated features

* Add reusable ArchUnit rule and example project for coverage annotations ([#14](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/14)) ([f797cc8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/f797cc8e0dadfab567b8da3e6708eadacb73a410)), closes [#14](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/14)


### 📝 Documentation

* **readme:** update version to 0.2.6 [skip ci] ([0e6915f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/0e6915f520e8553bb6150f89e699ce04f8598444))

## [0.2.6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.2.5...v0.2.6) (2026-05-01)


### 📝 Documentation

* **readme:** update version to 0.2.5 [skip ci] ([d99dc87](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/d99dc87b379ff08d14dbfc0b7fd1f1d8a80db097))


### 🔧 Misc

* dependency updates for sedr-library ([#12](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/12)) ([71cc489](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/71cc4898a450403973090fb3f75b8d078d118423)), closes [#12](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/12)

## [0.2.5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.2.4...v0.2.5) (2026-04-30)


### 🐛 Bug Fixes

* **security-scan:** enhance NVD database update and vulnerability scan with timeout handling and error reporting ([#11](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/11)) ([155167a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/155167af8600dbdedc2d440a9db25b5a3b8eb162)), closes [#11](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/11)


### 📝 Documentation

* **readme:** update version to 0.2.4 [skip ci] ([d8a95a2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/d8a95a24dcfee6cf4a5d7f7d9a9fc551d057cbe7))


### 🔧 Misc

* **Sedr Library:** update java dependency versions ([#9](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/9)) ([91b0ab2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/91b0ab229b5d867e2e34e488c1d617b09544a6d4)), closes [#9](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/9)


### security

* harden repository supply chain and access controls ([#8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/8)) ([e36bd0a](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/e36bd0aae70cbf416c8c72ebca453cd301e6915a)), closes [#8](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/8)

## [0.2.4](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.2.3...v0.2.4) (2026-04-15)


### Bug Fixes

* **publish:** set jReleaser GPG signing mode to MEMORY for env var keys ([#7](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/7)) ([8e5af82](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/8e5af8203316254d9f3a58a65e9070204fba97f9))

## [0.2.3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.2.2...v0.2.3) (2026-04-15)


### Bug Fixes

* add jsLibraryMappings.xml to exclude worktree node_modules ([6c6d30d](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/6c6d30ddbc0917b9d96332725e5269c9383137cd))
* add jsLibraryMappings.xml to exclude worktree node_modules ([fac12bf](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/fac12bfc11385cc1c19d8529eeba88db886953d0))
* Update .gitignore and exclude worktree node_modules ([#6](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/6)) ([206216f](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/206216fdb19f460c39d047d5a164f3beba3ce564))

## [0.2.2](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.2.1...v0.2.2) (2026-04-14)


### Bug Fixes

* Update README documentation and automate version updates ([#5](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/5)) ([5393640](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/539364075ef9a2c71d3d474f3935398959fde4fb))

## [0.2.1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.2.0...v0.2.1) (2026-04-14)


### Bug Fixes

* Update CI/CD workflows and fix publication configurations ([#4](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/4)) ([1249bdb](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/1249bdb1b7b968f5eac027cab25bfae98dc5872d))

# [0.2.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.1.0...v0.2.0) (2026-04-14)


### Features

* Add OWASP scan, improve workflow with release notes extraction ([#3](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/3)) ([1c81eff](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/1c81efffc3f02f8000186f3d913707ca8ea1561d))

# [0.1.0](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/compare/v0.0.0...v0.1.0) (2026-04-14)


### Features

* Set up initial project structure for sedr-library ([#1](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/issues/1)) ([f57a258](https://github.com/Arc-E-Tect/SoftwareEngineeringDoneRight-Library/commit/f57a258ed3d21badfe843d47722250d004ddad88))

# Changelog
