---
id: use_anvil_factory_generation
slug: /rules/use_anvil_factory_generation
title: Could Use Anvil Factory Generation
sidebar_label: Could Use Anvil Factory Generation
---

Anvil's [factory generation](https://github.com/square/anvil#dagger-factory-generation) is faster
than Dagger's generation using Kapt. However, it doesn't support generating Components or
Subcomponents, and it doesn't work in Java code.

This rule detects whether a module could switch from Dagger's kapt to Anvil factory generation.

Criteria:

- Anvil plugin applied with a version greater than 2.0.11
- Anvil's factory generation isn't already enabled (nothing to do in this case)
- No `@MergeComponent`, `@MergeSubcomponent`, `@Component` or `@Subcomponent` annotations
- No Dagger annotations in `.java` files
