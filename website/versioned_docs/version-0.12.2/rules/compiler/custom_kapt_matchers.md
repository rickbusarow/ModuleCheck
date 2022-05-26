---
id: custom_kapt_matchers
slug: /rules/custom_kapt_matchers
title: Custom Kapt Matchers
sidebar_label: Custom Kapt Matchers
---

It's simple to add a custom matcher for an internal-use annotation processor.

Just define a list of regex strings for all of the fully qualified names of its annotations.

```kotlin
moduleCheck {
  additionalKaptMatchers.set(
    listOf(
      modulecheck.api.KaptMatcher(
        name = "MyProcessor",
        processor = "my-project.codegen:processor",
        annotationImports = listOf(
          "myproject\\.\\*",
          "myproject\\.MyInject",
          "myproject\\.MyInject\\.Factory",
          "myproject\\.MyInjectParam",
          "myproject\\.MyInjectModule"
        )
      )
    )
  )
}
```
