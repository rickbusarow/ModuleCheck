# 0.12.1-SNAPSHOT (unreleased)

### 🗑 Deprecations

- The names of all findings have been updated/standardized. Any declarations which were suppressing
  a finding with the old ID (via `@Suppress("someFinding")` or `//suppress=someFinding`) will still
  work, but they should be updated to use the new names.

  | old name                      | new name                         |
  |:------------------------------|:---------------------------------|
  | depth                         | project-depth                    |
  | disableAndroidResources       | disable-android-resources        |
  | disableViewBinding            | disable-view-binding             |
  | inheritedDependency           | inherited-dependency             |
  | mustBeApi                     | must-be-api                      |
  | overshot                      | overshot-dependency              |
  | redundant                     | redundant-dependency             |
  | unsortedDependencies          | sort-dependencies                |
  | unsortedPlugins               | sort-plugins                     |
  | useAnvilFactories             | use-anvil-factory-generation     |
  | unused                        | unused-dependency                |
  | unusedKaptProcessor           | unused-kapt-processor            |
  | unusedKotlinAndroidExtensions | unused-kotlin-android-extensions |

### ℹ️ Website

- add `google()` repository to config
  docs [@RBusarow](https://github.com/RBusarow) ([#559](https://github.com/rbusarow/ModuleCheck/pull/559))
- add missing docs for
  rules [@RBusarow](https://github.com/RBusarow) ([#555](https://github.com/rbusarow/ModuleCheck/pull/555))

# 0.12.0

### 💥 Breaking Changes

- The `autoCorrect` property in the Gradle settings DSL has been removed. Instead, to perform a
  check with auto-correct, add the `Auto` suffix to the task name.
  ```bash
  # perform all checks and fail if errors are found
  ./gradlew moduleCheck

  # perform all checks and auto-correct if possible
  ./gradlew moduleCheckAuto
  ```
- Tasks are no longer generated for most individual rules. Instead, rules should be toggled via
  the [Gradle DSL](http://localhost:3000/ModuleCheck/docs/next/configuration) and can be invoked
  through `./gradlew modulecheck` or `./gradlew moduleCheckAuto`.

### 📐 New Rules

- Add the [Depths](https://rbusarow.github.io/ModuleCheck/docs/0.12.0/rules/depths)
  rule [@RBusarow](https://github.com/RBusarow)  ([#278](https://github.com/rbusarow/ModuleCheck/pull/278))
- New
  rule: [Unused Android Extensions](https://rbusarow.github.io/ModuleCheck/docs/0.12.0/rules/unused_kotlin_android_extensions) [@tasomaniac](https://github.com/tasomaniac) ([#440](https://github.com/rbusarow/ModuleCheck/pull/440))

### 🚀 Features

- Add support for depths, dotviz dependency graph, checkstyle, and plaintext result
  reporting [@RBusarow](https://github.com/RBusarow) ([#243](https://github.com/rbusarow/ModuleCheck/pull/243))

### 🐛 Bug Fixes

- Add a test case for false
  positive [@tasomaniac](https://github.com/tasomaniac) ([#419](https://github.com/rbusarow/ModuleCheck/pull/419))
- Don't call a dependency overshot if it's already declared in that source
  set [@RBusarow](https://github.com/RBusarow) ([#521](https://github.com/rbusarow/ModuleCheck/pull/521))
- don't try to parse `.png`s as
  xml [@RBusarow](https://github.com/RBusarow) ([#522](https://github.com/rbusarow/ModuleCheck/pull/522))
- fix parsing xml resource declarations when there's a dot in the
  name [@RBusarow](https://github.com/RBusarow) ([#512](https://github.com/rbusarow/ModuleCheck/pull/512))
- Fix false positive for `unusedDependency` when a resource from the dependency is used with R from
  the dependent in a downstream
  project [@RBusarow](https://github.com/RBusarow) ([#510](https://github.com/rbusarow/ModuleCheck/pull/510))
- better modeling for generated databinding declarations and
  references [@RBusarow](https://github.com/RBusarow) ([#509](https://github.com/rbusarow/ModuleCheck/pull/509))
- count layout files and `@+id/__` declarations as part of a module's
  declarations [@RBusarow](https://github.com/RBusarow) ([#499](https://github.com/rbusarow/ModuleCheck/pull/499))
- Support the alternative usage of kapt
  plugin [@tasomaniac](https://github.com/tasomaniac) ([#481](https://github.com/rbusarow/ModuleCheck/pull/481))
- add new dependency declarations even if their transitive source can't be
  found [@RBusarow](https://github.com/RBusarow) ([#469](https://github.com/rbusarow/ModuleCheck/pull/469))
- don't generate BuildConfig if it's ignored in Android
  settings [@RBusarow](https://github.com/RBusarow) ([#470](https://github.com/rbusarow/ModuleCheck/pull/470))
- force single-threaded GroovyLangParser
  access [@RBusarow](https://github.com/RBusarow) ([#463](https://github.com/rbusarow/ModuleCheck/pull/463))
- fix false positive for `disableViewBinding` when used in debug source set of different
  module [@RBusarow](https://github.com/RBusarow) ([#446](https://github.com/rbusarow/ModuleCheck/pull/446))
- don't swallow a newline when replacing a dependency with a preceding blank
  line [@RBusarow](https://github.com/RBusarow) ([#444](https://github.com/rbusarow/ModuleCheck/pull/444))
- better handling for detecting complex precompiled configuration
  names [@RBusarow](https://github.com/RBusarow) ([#442](https://github.com/rbusarow/ModuleCheck/pull/442))
- support multiple android base
  packages [@RBusarow](https://github.com/RBusarow) ([#411](https://github.com/rbusarow/ModuleCheck/pull/411))
- support `.java` files without a package
  declaration [@RBusarow](https://github.com/RBusarow) ([#400](https://github.com/rbusarow/ModuleCheck/pull/400))
- strip illegal characters from XML before
  parsing [@RBusarow](https://github.com/RBusarow) ([#376](https://github.com/rbusarow/ModuleCheck/pull/376))
- fix auto-correct when using a non-standard config
  name [@RBusarow](https://github.com/RBusarow) ([#368](https://github.com/rbusarow/ModuleCheck/pull/368))
- fix false positive for kapt processors in non-kapt
  configurations [@RBusarow](https://github.com/RBusarow) ([#350](https://github.com/rbusarow/ModuleCheck/pull/350))
- don't allow projects to inherit
  themselves [@RBusarow](https://github.com/RBusarow) ([#343](https://github.com/rbusarow/ModuleCheck/pull/343))
- update configuration
  docs [@RBusarow](https://github.com/RBusarow) ([#335](https://github.com/rbusarow/ModuleCheck/pull/335))
- always create depth and graph reports when running their explicit
  tasks [@RBusarow](https://github.com/RBusarow) ([#332](https://github.com/rbusarow/ModuleCheck/pull/332))
- collect depth info after applying
  changes [@RBusarow](https://github.com/RBusarow) ([#331](https://github.com/rbusarow/ModuleCheck/pull/331))
- fix testFixtures handling in
  OverShotDependencyFinding [@RBusarow](https://github.com/RBusarow) ([#297](https://github.com/rbusarow/ModuleCheck/pull/297))
- treat testFixtures and the associated main sources like different
  projects [@RBusarow](https://github.com/RBusarow) ([#288](https://github.com/rbusarow/ModuleCheck/pull/288))
- correctly apply the `testFixtures(...)` wrapper for replaced/added
  dependencies [@RBusarow](https://github.com/RBusarow) ([#287](https://github.com/rbusarow/ModuleCheck/pull/287))

### ℹ️ Website

- Add documentation for new
  rule [@tasomaniac](https://github.com/tasomaniac) ([#454](https://github.com/rbusarow/ModuleCheck/pull/454))
- add snapshots badge to README and website
  home [@RBusarow](https://github.com/RBusarow) ([#410](https://github.com/rbusarow/ModuleCheck/pull/410))
- add `moduleCheckAuto` to main README and call out "next" docs
  version [@RBusarow](https://github.com/RBusarow) ([#408](https://github.com/rbusarow/ModuleCheck/pull/408))
- correct the tasks listed in the "next" version of the
  docs [@RBusarow](https://github.com/RBusarow) ([#404](https://github.com/rbusarow/ModuleCheck/pull/404))
- update copyright template for
  2022 [@RBusarow](https://github.com/RBusarow) ([#362](https://github.com/rbusarow/ModuleCheck/pull/362))
- update configuration
  docs [@RBusarow](https://github.com/RBusarow) ([#335](https://github.com/rbusarow/ModuleCheck/pull/335))
- add the Depths
  feature [@RBusarow](https://github.com/RBusarow) ([#278](https://github.com/rbusarow/ModuleCheck/pull/278))
- replace `autoCorrect` with `-Auto`
  suffixes [@RBusarow](https://github.com/RBusarow) ([#249](https://github.com/rbusarow/ModuleCheck/pull/249))

### Contributors

@RBusarow, @diego-gomez-olvera and @tasomaniac

# 0.11.3

### 🚀 Features

- support suppressing findings ([#235](https://github.com/rbusarow/ModuleCheck/pull/235))

### 🐛 Bug Fixes

- support testFixtures ([#232](https://github.com/rbusarow/ModuleCheck/pull/232))

### 🧰 Maintenance

- Bump kotlinpoet from 1.10.1 to 1.10.2 ([#233](https://github.com/rbusarow/ModuleCheck/pull/233))
- Bump gradle-plugin from 2.3.6 to 2.3.7 ([#229](https://github.com/rbusarow/ModuleCheck/pull/229))

### ℹ️ Website

- use titles in docs code snippets ([#237](https://github.com/rbusarow/ModuleCheck/pull/237))
- clarify CI workflow docs ([#221](https://github.com/rbusarow/ModuleCheck/pull/221))
- add example CI workflow to docs ([#220](https://github.com/rbusarow/ModuleCheck/pull/220))

# 0.11.2

### 🐛 Bug Fixes

- support overshot dependencies ([#217](https://github.com/rbusarow/ModuleCheck/pull/217))
- sorting fixes ([#215](https://github.com/rbusarow/ModuleCheck/pull/215))

# 0.11.1

### 🐛 Bug Fixes

- support constant or enum declarations in Java
  classes ([#209](https://github.com/rbusarow/ModuleCheck/pull/209))
- include generated data/viewbinding objects as
  declarations ([#208](https://github.com/rbusarow/ModuleCheck/pull/208))
- support closures in dependency
  declarations ([#205](https://github.com/rbusarow/ModuleCheck/pull/205))
- count resources as R references when used in
  AndroidManifest.xml ([#203](https://github.com/rbusarow/ModuleCheck/pull/203))

# 0.11.0 - Groovy auto-correct support

### 🐛 Bug Fixes

- Support generated manifests ([#197](https://github.com/rbusarow/ModuleCheck/pull/197))
- fix redundant "from: " output ([#193](https://github.com/rbusarow/ModuleCheck/pull/193))
- support Groovy parsing ([#190](https://github.com/rbusarow/ModuleCheck/pull/190))
- capture a finding's log string before it's
  fixed ([#184](https://github.com/rbusarow/ModuleCheck/pull/184))
- include class literal expressions when looking for type
  references ([#177](https://github.com/rbusarow/ModuleCheck/pull/177))
- check for Android kotlin sources ([#173](https://github.com/rbusarow/ModuleCheck/pull/173))

# 0.10.0

Initial release
