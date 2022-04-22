---
id: overshot_dependency
title: Overshot Dependency
sidebar_label: Overshot Dependency
---

Finds project dependencies which aren't used by the declaring configuration, but are used by a
dependent, downstream configuration.

For instance, assume that `:moduleB` declares an `implementation` dependency upon `:moduleA`.

```kotlin title="moduleB/build.gradle.kts"
dependencies {
  implementation(project(":moduleA"))
}
```

If `:moduleB` doesn't actually use `:moduleA` in its `main` source, but it _does_ use it in `test`
source, it's an __overshot dependency__. The declaration should be changed to
use `testImplementation`:

```kotlin title="moduleB/build.gradle.kts"
dependencies {
  testImplementation(project(":moduleA"))
}
```
