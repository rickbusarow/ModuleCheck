---
id: disable_android_resources
slug: /rules/disable_android_resources
title: Disable Android Resources
sidebar_label: Disable Android Resources
---

If an Android module doesn't actually have any resources in the `src/__/res` directory,
then `android.buildFeatures.androidResources` can be disabled.

```kotlin
android {
  buildFeatures {
    androidResource = false
  }
}
```
