## 0.12.1

#### 🗑 Deprecations

- The names of all findings have been updated/standardized. Any declarations which were suppressing
  a finding with the old ID (via `@Suppress("someFinding")` or `//suppress=someFinding`) will still
  work, but they should be updated to use the new names.
  See [the migrations guide](/migrations#standardized-finding-names)
- The method for defining `additionalKaptMatchers` in the Gradle DSL has been deprecated, replaced
  with the `additionalCodeGenerators` property and `CodeGeneratorBinding`.
  See [the migrations guide](/migrations#code-generator-binding)

#### 💥 Breaking Changes

- The base `:moduleCheck` task will now automatically hook into the root project's `:check` task, if
  one
  exists. [@RBusarow](https://github.com/RBusarow) ([#611](https://github.com/rbusarow/ModuleCheck/pull/611))

#### 🚀 Features

- Added support
  for [Static Analysis Results Interchange Format (SARIF)](https://sarifweb.azurewebsites.net)
  report
  output [@RBusarow](https://github.com/RBusarow) ([#566](https://github.com/rbusarow/ModuleCheck/pull/566))

#### 🐛 Bug Fixes

- don't find `must-be-api` if the project is already an api dependency
  also [@RBusarow](https://github.com/RBusarow) ([#666](https://github.com/rbusarow/ModuleCheck/pull/666))
- remove AGP and KGP from the plugin's runtime
  classpath ([079ab9d](https://github.com/RBusarow/ModuleCheck/commit/079ab9d709add63dbf44ecbd8a534bf279becd47))
- fix matching to custom
  kaptMatchers [@RBusarow](https://github.com/RBusarow) ([#658](https://github.com/rbusarow/ModuleCheck/pull/658))
- properly use settings to determine which kinds of depth output to
  create [@RBusarow](https://github.com/RBusarow) ([#647](https://github.com/rbusarow/ModuleCheck/pull/647))
- fix relative paths for custom graph report
  directory [@RBusarow](https://github.com/RBusarow) ([#612](https://github.com/rbusarow/ModuleCheck/pull/612))
- use type-safe accessor "path" when adding a dependency with type-safe
  syntax [@RBusarow](https://github.com/RBusarow) ([#608](https://github.com/rbusarow/ModuleCheck/pull/608))
- evaluate suppress/noinspection annotations
  eagerly [@RBusarow](https://github.com/RBusarow) ([#604](https://github.com/rbusarow/ModuleCheck/pull/604))
- fixes false negative for unused kapt plugin when there are no
  processors [@RBusarow](https://github.com/RBusarow) ([#603](https://github.com/rbusarow/ModuleCheck/pull/603))
- fix Dagger NoSuchMethodError for `dagger.internal.Preconditions.checkNotNullFromProvides` in
  SNAPSHOT [@RBusarow](https://github.com/RBusarow) ([#570](https://github.com/rbusarow/ModuleCheck/pull/570))

#### 🧰 Maintenance

- add a discrete job in CI for publishing to mavenLocal, then cache
  it [@RBusarow](https://github.com/RBusarow) ([#668](https://github.com/rbusarow/ModuleCheck/pull/668))
- update the build classpath baseline for the snapshot build's new
  runt… [@RBusarow](https://github.com/RBusarow) ([#664](https://github.com/rbusarow/ModuleCheck/pull/664))
- use the current SNAPSHOT for plugin
  dogfooding [@RBusarow](https://github.com/RBusarow) ([#663](https://github.com/rbusarow/ModuleCheck/pull/663))
- migrate TestKit tests away from the Specs
  DSLs [@RBusarow](https://github.com/RBusarow) ([#660](https://github.com/rbusarow/ModuleCheck/pull/660))
- hook dependencyGuard into the `check`
  task [@RBusarow](https://github.com/RBusarow) ([#661](https://github.com/rbusarow/ModuleCheck/pull/661))
- give Dokka explicit dependency upon KtLint tasks and more broadly
  dis… [@RBusarow](https://github.com/RBusarow) ([#659](https://github.com/rbusarow/ModuleCheck/pull/659))
- Update dropbox-dependencyGuard to
  v0.2.0 [@renovate](https://github.com/renovate) ([#657](https://github.com/rbusarow/ModuleCheck/pull/657))
- require comments for public APIs in Detekt, and add
  baselines [@RBusarow](https://github.com/RBusarow) ([#656](https://github.com/rbusarow/ModuleCheck/pull/656))
- add dependency-guard and
  baselines [@RBusarow](https://github.com/RBusarow) ([#654](https://github.com/rbusarow/ModuleCheck/pull/654))
- Update dependency prism-react-renderer to
  v1.3.3 [@renovate](https://github.com/renovate) ([#653](https://github.com/rbusarow/ModuleCheck/pull/653))
- Update dependency com.android.tools.build:gradle to
  v7.2.0 [@renovate](https://github.com/renovate) ([#620](https://github.com/rbusarow/ModuleCheck/pull/620))
- Update actions/setup-java action to
  v3 [@renovate](https://github.com/renovate) ([#652](https://github.com/rbusarow/ModuleCheck/pull/652))
- Update dependency com.autonomousapps.dependency-analysis to
  v1.2.1 [@renovate](https://github.com/renovate) ([#651](https://github.com/rbusarow/ModuleCheck/pull/651))
- Update actions/upload-artifact action to
  v3 [@renovate](https://github.com/renovate) ([#629](https://github.com/rbusarow/ModuleCheck/pull/629))
- Update dependency com.gradleup.auto.manifest to
  v2 [@renovate](https://github.com/renovate) ([#645](https://github.com/rbusarow/ModuleCheck/pull/645))
- Update react monorepo to v18 (
  major) [@renovate](https://github.com/renovate) ([#646](https://github.com/rbusarow/ModuleCheck/pull/646))
- remove github actions
  caching [@RBusarow](https://github.com/RBusarow) ([#649](https://github.com/rbusarow/ModuleCheck/pull/649))
- remove
  dependabot [@RBusarow](https://github.com/RBusarow) ([#648](https://github.com/rbusarow/ModuleCheck/pull/648))
- create a shared `.gradle` cache for TestKit
  tests [@RBusarow](https://github.com/RBusarow) ([#640](https://github.com/rbusarow/ModuleCheck/pull/640))
- add the `artifacts-check` convention
  plugin [@RBusarow](https://github.com/RBusarow) ([#615](https://github.com/rbusarow/ModuleCheck/pull/615))
- fix incorrect/duplicate maven artifact
  ids [@RBusarow](https://github.com/RBusarow) ([#614](https://github.com/rbusarow/ModuleCheck/pull/614))
- revert KaptMatcher name
  to `modulecheck.api.KaptMatcher` [@RBusarow](https://github.com/RBusarow) ([#613](https://github.com/rbusarow/ModuleCheck/pull/613))
-

delete `ConfiguredModule` [@RBusarow](https://github.com/RBusarow) ([#609](https://github.com/rbusarow/ModuleCheck/pull/609))

- disable the "use tab character" option in IDE
  codestyle [@RBusarow](https://github.com/RBusarow) ([#607](https://github.com/rbusarow/ModuleCheck/pull/607))
- replace `java-test-fixtures` usages with `-testing`
  modules [@RBusarow](https://github.com/RBusarow) ([#605](https://github.com/rbusarow/ModuleCheck/pull/605))
- Bump mermaid from 8.14.0 to 9.1.1 in
  /website [@dependabot](https://github.com/dependabot) ([#601](https://github.com/rbusarow/ModuleCheck/pull/601))
- Dependency block to dependencies
  block [@RBusarow](https://github.com/RBusarow) ([#600](https://github.com/rbusarow/ModuleCheck/pull/600))
- split
  up `:modulecheck-parsing:gradle` [@RBusarow](https://github.com/RBusarow) ([#599](https://github.com/rbusarow/ModuleCheck/pull/599))
- Bump dagger from 2.41 to
  2.42 [@dependabot](https://github.com/dependabot) ([#597](https://github.com/rbusarow/ModuleCheck/pull/597))
- rename `modulecheck.reporting.logging.Logger`
  to `McLogger` [@RBusarow](https://github.com/RBusarow) ([#593](https://github.com/rbusarow/ModuleCheck/pull/593))
- pull `Finding` apis out of `:modulecheck-rules:api` and into their
  ow… [@RBusarow](https://github.com/RBusarow) ([#591](https://github.com/rbusarow/ModuleCheck/pull/591))
- Fix execution optimization for KtLint
  in `:modulecheck-plugin` [@RBusarow](https://github.com/RBusarow) ([#590](https://github.com/rbusarow/ModuleCheck/pull/590))
- use the stable version of the plugin in
  CI [@RBusarow](https://github.com/RBusarow) ([#589](https://github.com/rbusarow/ModuleCheck/pull/589))
- suppress Detekt's ComplexMethod for single `when`
  statements [@RBusarow](https://github.com/RBusarow) ([#584](https://github.com/rbusarow/ModuleCheck/pull/584))
- pull rules and configs into their own
  modules [@RBusarow](https://github.com/RBusarow) ([#583](https://github.com/rbusarow/ModuleCheck/pull/583))
- Bump kotest-assertions-core-jvm from 5.2.3 to
  5.3.0 [@dependabot](https://github.com/dependabot) ([#579](https://github.com/rbusarow/ModuleCheck/pull/579))
- Bump turbine from 0.7.0 to
  0.8.0 [@dependabot](https://github.com/dependabot) ([#580](https://github.com/rbusarow/ModuleCheck/pull/580))
- fix `BasePluginTest`'s unused `stacktrace`
  parameter [@RBusarow](https://github.com/RBusarow) ([#578](https://github.com/rbusarow/ModuleCheck/pull/578))
- add Detekt's SARIF reports to
  CI [@RBusarow](https://github.com/RBusarow) ([#568](https://github.com/rbusarow/ModuleCheck/pull/568))
- standardize hermit's definition
  in `libs.versions.toml` [@RBusarow](https://github.com/RBusarow) ([#567](https://github.com/rbusarow/ModuleCheck/pull/567))
- move `File.createSafely()` to production
  code [@RBusarow](https://github.com/RBusarow) ([#565](https://github.com/rbusarow/ModuleCheck/pull/565))
- Bump dokka-gradle-plugin from 1.6.20 to
  1.6.21 [@dependabot](https://github.com/dependabot) ([#563](https://github.com/rbusarow/ModuleCheck/pull/563))
- Bump async from 2.6.3 to 2.6.4 in
  /website [@dependabot](https://github.com/dependabot) ([#543](https://github.com/rbusarow/ModuleCheck/pull/543))
- Bump kotlin-reflect from 1.6.20 to
  1.6.21 [@dependabot](https://github.com/dependabot) ([#553](https://github.com/rbusarow/ModuleCheck/pull/553))
- Bump mermaid from 8.13.8 to 8.14.0 in
  /website [@dependabot](https://github.com/dependabot) ([#545](https://github.com/rbusarow/ModuleCheck/pull/545))
- Bump dokka-gradle-plugin from 1.6.10 to
  1.6.20 [@dependabot](https://github.com/dependabot) ([#544](https://github.com/rbusarow/ModuleCheck/pull/544))
- Bump @mdx-js/react from 1.6.22 to 2.1.1 in
  /website [@dependabot](https://github.com/dependabot) ([#546](https://github.com/rbusarow/ModuleCheck/pull/546))
- Bump antlr4 from 4.10 to
  4.10.1 [@dependabot](https://github.com/dependabot) ([#550](https://github.com/rbusarow/ModuleCheck/pull/550))

#### ℹ️ Website

- remove the `google()` repository requirement from
  docs [@RBusarow](https://github.com/RBusarow) ([#667](https://github.com/rbusarow/ModuleCheck/pull/667))
- 594 hook into root project check
  task [@RBusarow](https://github.com/RBusarow) ([#611](https://github.com/rbusarow/ModuleCheck/pull/611))
- replace `KaptMatcher`
  with `CodeGeneratorBinding` [@RBusarow](https://github.com/RBusarow) ([#610](https://github.com/rbusarow/ModuleCheck/pull/610))
- update Docusaurus to
  2.0.0-beta.20 [@RBusarow](https://github.com/RBusarow) ([#592](https://github.com/rbusarow/ModuleCheck/pull/592))
- add support for sarif
  reporting [@RBusarow](https://github.com/RBusarow) ([#566](https://github.com/rbusarow/ModuleCheck/pull/566))
- strict rule/finding name
  conventions [@RBusarow](https://github.com/RBusarow) ([#564](https://github.com/rbusarow/ModuleCheck/pull/564))
- add `google()` repository to config
  docs [@RBusarow](https://github.com/RBusarow) ([#559](https://github.com/rbusarow/ModuleCheck/pull/559))
- add missing docs for
  rules [@RBusarow](https://github.com/RBusarow) ([#555](https://github.com/rbusarow/ModuleCheck/pull/555))
- fix
  publishing [@RBusarow](https://github.com/RBusarow) ([#548](https://github.com/rbusarow/ModuleCheck/pull/548))
- release
  0.12.0 [@RBusarow](https://github.com/RBusarow) ([#547](https://github.com/rbusarow/ModuleCheck/pull/547))

#### Contributors

@RBusarow

## 0.12.0

#### 💥 Breaking Changes

- The `autoCorrect` property in the Gradle settings DSL has been removed. Instead, to perform a
  check with auto-correct, add the `Auto` suffix to the task name.
  ```bash
  # perform all checks and fail if errors are found
  ./gradlew moduleCheck

  # perform all checks and auto-correct if possible
  ./gradlew moduleCheckAuto
  ```
- Tasks are no longer generated for most individual rules. Instead, rules should be toggled via
  the [Gradle DSL](/docs/next/configuration) and can be
  invoked
  through `./gradlew modulecheck` or `./gradlew moduleCheckAuto`.

#### 📐 New Rules

- Add the [Depths](/docs/rules/project_depth)
  rule [@RBusarow](https://github.com/RBusarow)  ([#278](https://github.com/rbusarow/ModuleCheck/pull/278))
- New
  rule: [Unused Android Extensions](/docs/rules/unused_kotlin_android_extensions) [@tasomaniac](https://github.com/tasomaniac) ([#440](https://github.com/rbusarow/ModuleCheck/pull/440))

#### 🚀 Features

- Add support for depths, dotviz dependency graph, checkstyle, and plaintext result
  reporting [@RBusarow](https://github.com/RBusarow) ([#243](https://github.com/rbusarow/ModuleCheck/pull/243))

#### 🐛 Bug Fixes

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

#### ℹ️ Website

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

#### Contributors

@RBusarow, @diego-gomez-olvera and @tasomaniac

## 0.11.3

#### 🚀 Features

- support suppressing findings ([#235](https://github.com/rbusarow/ModuleCheck/pull/235))

#### 🐛 Bug Fixes

- support testFixtures ([#232](https://github.com/rbusarow/ModuleCheck/pull/232))

#### 🧰 Maintenance

- Bump kotlinpoet from 1.10.1 to 1.10.2 ([#233](https://github.com/rbusarow/ModuleCheck/pull/233))
- Bump gradle-plugin from 2.3.6 to 2.3.7 ([#229](https://github.com/rbusarow/ModuleCheck/pull/229))

#### ℹ️ Website

- use titles in docs code snippets ([#237](https://github.com/rbusarow/ModuleCheck/pull/237))
- clarify CI workflow docs ([#221](https://github.com/rbusarow/ModuleCheck/pull/221))
- add example CI workflow to docs ([#220](https://github.com/rbusarow/ModuleCheck/pull/220))

## 0.11.2

#### 🐛 Bug Fixes

- support overshot dependencies ([#217](https://github.com/rbusarow/ModuleCheck/pull/217))
- sorting fixes ([#215](https://github.com/rbusarow/ModuleCheck/pull/215))

## 0.11.1

#### 🐛 Bug Fixes

- support constant or enum declarations in Java
  classes ([#209](https://github.com/rbusarow/ModuleCheck/pull/209))
- include generated data/viewbinding objects as
  declarations ([#208](https://github.com/rbusarow/ModuleCheck/pull/208))
- support closures in dependency
  declarations ([#205](https://github.com/rbusarow/ModuleCheck/pull/205))
- count resources as R references when used in
  AndroidManifest.xml ([#203](https://github.com/rbusarow/ModuleCheck/pull/203))

## 0.11.0 - Groovy auto-correct support

#### 🐛 Bug Fixes

- Support generated manifests ([#197](https://github.com/rbusarow/ModuleCheck/pull/197))
- fix redundant "from: " output ([#193](https://github.com/rbusarow/ModuleCheck/pull/193))
- support Groovy parsing ([#190](https://github.com/rbusarow/ModuleCheck/pull/190))
- capture a finding's log string before it's
  fixed ([#184](https://github.com/rbusarow/ModuleCheck/pull/184))
- include class literal expressions when looking for type
  references ([#177](https://github.com/rbusarow/ModuleCheck/pull/177))
- check for Android kotlin sources ([#173](https://github.com/rbusarow/ModuleCheck/pull/173))

## 0.10.0

Initial release
