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



## License

``` text
Copyright (C) 2021 Rick Busarow
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

