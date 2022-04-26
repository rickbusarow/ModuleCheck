---
id: disable_view_binding
slug: /rules/disable_view_binding
title: Disable ViewBinding
sidebar_label: Disable ViewBinding
---

If an Android module has `viewBinding` enabled, but doesn't contribute any generated `____Binding`
objects from layout files which are actually used, then `viewBinding` can be disabled.

```kotlin
android {
  buildFeatures {
    viewBinding = false
  }
}
```
