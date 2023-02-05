---
id: unused_kapt_processor
slug: /rules/unused_kapt_processor
title: Unused Kapt Processor
sidebar_label: Unused Kapt Processor
---

Annotation processors act upon a defined set of annotations. If an annotation processor is
sufficiently popular and its api is stable, then it's relatively simple to define a list of
annotations to search for. For instance, Dagger looks for the following annotations:

- `javax.inject.Inject`
- `dagger.Binds`
- `dagger.Module`
- `dagger.multibindings.IntoMap`
- `dagger.multibindings.IntoSet`
- `dagger.BindsInstance`
- `dagger.Component`
- `dagger.assisted.Assisted`
- `dagger.assisted.AssistedInject`
- `dagger.assisted.AssistedFactory`
- `com.squareup.anvil.annotations.ContributesTo`
- `com.squareup.anvil.annotations.MergeComponent`
- `com.squareup.anvil.annotations.MergeSubomponent`

If a module has the Dagger `kapt` dependency, and that module *does not* have one of the above
annotations somewhere, then Dagger isn't actually doing anything and can be removed.

This is simply a best-effort approach, and it isn't maintenance-free. Over time, the list of
annotations for any processor may change. If this rule gives a false-positive finding because of a
new annotation, please open an issue and/or pull request.
