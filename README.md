### This is a work in progress, in a very early state, and there are bugs.

___

ModuleCheck identifies unused **internal** (sub-project) dependencies within a Gradle project.

It does this **without performing a build**, which makes the parsing extremely fast.

All inspection is done using Gradle build files, Java/Kotlin source, and `res` xml files for Kotlin.

Documentation is at [https://rbusarow.github.io/ModuleCheck](https://rbusarow.github.io/ModuleCheck/).

### Config
```kotlin
// settings.gradle.kts

pluginManagement {
  repositories {
    gradlePluginPortal()
  }
}
```

```kotlin
// top-level build.gradle.kts

plugins {
  id("com.rickbusarow.module-check") version "0.10.0"
}
```

### Tasks

all checks
```shell
./gradlew moduleCheck
```

kapt checks
```shell
./gradlew moduleCheckKapt
```

sorts
```shell
./gradlew moduleCheckSortPlugins moduleCheckSortDependencies
```

unused
```shell
./gardlew moduleCheckUnused
```

redundant
```shell
./gradlew moduleCheckRedundant
```

overshot
```shell
./gradlew moduleCheckOvershot
```


### TODO

#### Done
- [X] kapt
- [X] disable android resources
- [X] disable viewbinding
- [X] sort plugins
- [X] sort dependencies
- [X] overshot

#### Partial
- [ ] unused
- [ ] redundant
- [ ] used?
- [ ] parse java files
- [ ] auto-fix

#### Backlog
- [ ] disable android values ???
- [ ] Reports
  - [ ] checkstyle
  - [ ] junit
  - [ ] text
- [ ] duplicates & different configs (like listed as `implementation` and `api`)
- [ ] differentiate between java and android modules
- [ ] parse resources (strings, drawables, icons, etc.)
- [ ] imports for nested classes
- [ ] minify (remove dependency if declared as api by dependency module)
- [ ] disallow test dependencies if no test src directory
- [ ] disallow androidTest dependencies if no androidTest src directory

