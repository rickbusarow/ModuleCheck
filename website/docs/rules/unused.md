---
id: unused

title: Unused Dependency

sidebar_label: Unused Dependency
---

Unused module dependencies which are unused create unnecessary bottlenecks in a build task. Instead
of building modules concurrently, Gradle must wait until the dependency module is built before
beginning to build the dependent one.

ModuleCheck determines whether a dependency is unused by looking for all fully qualified names
declared in its API, then searching the dependent module's code for references to any of those
names. If there are no references, the dependency module is considered to be unused.
