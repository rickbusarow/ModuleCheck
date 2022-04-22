---
id: redundant_dependency
title: Redundant Dependency
sidebar_label: Redundant Dependency
---
:::caution

This rule creates a brittle dependency graph, because some necessary dependencies are only provided
transitively by other dependencies. Any manual changes to dependencies can have unexpected
consequences downstream.

This rule is **not recommended** and disabled by default, but it's still available for those who
want to keep their build files as small as possible.

:::

Finds project dependencies which are declared as `api` in other dependency projects, but also
declared in the current project. These dependencies can be removed without actually breaking the
build, since they're still provided by an upstream dependency through the `api` configuration.

```mermaid
flowchart LR

  linkStyle default stroke-width:2px,fill:none,stroke:green;

  classDef depth2 fill:#BBF,stroke:#000,color:#000
  classDef depth1 fill:#B9B,stroke:#000,color:#000
  classDef depth0 fill:#FBB,stroke:#000,color:#000

  subgraph sg_redundant [A redundant graph]
    direction TB

    lib1_redundant(:lib-1):::depth0
    lib2_redundant(:lib-2):::depth1
    app_redundant(:app):::depth2

    app_redundant --> |api| lib1_redundant
    app_redundant --> |api| lib2_redundant

    lib2_redundant --> |api| lib1_redundant
  end

  subgraph sg_minimalist [A graph with no redundancy]
    direction TB

    lib1_minimalist(:lib-1):::depth0
    lib2_minimalist(:lib-2):::depth1
    app_minimalist(:app):::depth2

    app_minimalist --> |api| lib2_minimalist

    lib2_minimalist --> |api| lib1_minimalist
  end

  style sg_redundant fill:#C66,stroke:#000,color:#FFF
  style sg_minimalist fill:#696,stroke:#000,color:#FFF

  sg_redundant --> |./gradlew moduleCheck| sg_minimalist

```

This is the opposite of the [inherited dependency] rule, which ensures a stable graph by explicitly
declaring each dependency. [Inherited dependency] is enabled by default, and is the recommended
approach. Both rules may not be enabled at the same time.

[Inherited dependency]:inherited_dependency.md
