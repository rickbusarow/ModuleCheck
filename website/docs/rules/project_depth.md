---
id: project_depth
slug: /rules/project_depth
title: Project Depth
sidebar_label: Project Depth
---

TL;DR - Low depth values mean faster builds and better all-around scalability.

---

It's often useful to think of module dependencies as a directed tree
or [directed acyclic graph](https://en.wikipedia.org/wiki/Directed_acyclic_graph). If a module is a
node, then each module dependency is a child node, and the dependencies of those dependencies are
grand-child nodes.

This is especially useful when thinking about **build performance**, because the parent-child
relationship is clear: _child nodes must build before parent nodes_.

```mermaid
flowchart TB

  classDef depth2 fill:#BBF,stroke:#000,color:#000
  classDef depth1 fill:#B9B,stroke:#000,color:#000
  classDef depth0 fill:#FBB,stroke:#000,color:#000

  linkStyle default stroke-width:2px,fill:none,stroke:green;

  app(:app):::depth2

  screen1(:screen-1):::depth1
  screen2(:screen-2):::depth1

  lib1(:lib-1):::depth0
  lib2(:lib-2):::depth0

  app --> screen1
  app --> screen2

  screen1 --> lib1
  screen1 --> lib2
  screen2 --> lib2
```

In the above example,

- `:lib-1` and `:lib-2` must be built before `:screen-1`.
- `:lib-2` must be build before `:screen-2`.
- `:screen-1` and `:screen-2` must be built before `:app`.

It's worth pointing out that this relationship is recursive, as well. Grand-child nodes must build
before their parents.

### Dependencies and Build Concurrency

Individual module builds are always done single-threaded, but multiple modules may build in parallel
so long as no module in the set depends upon another module in that set. In the above graph,

- `:lib-1` and `:lib-2` may build in parallel
- `:lib-1` and `:screen-2` may build in parallel
- `:scren-1` and `:screen-2` may build in parallel

The maximum number of parallel module builds is determined by the structure of the dependency graph
and the number of available processor cores on the machine which is performing the build.

### Depth

**Depth** refers to the maximum number of edges between a module and each of its leaf nodes in the
project dependency graph.

Low depth values indicate a shallow or flat project structure with loose (or no) coupling between
modules. In a full build, these projects scale well with hardware upgrades because they're able to
build all those independent modules in parallel.

```mermaid
flowchart  TB

  subgraph sg [A shallow graph]
    direction TB

    classDef depth3 fill:#F7B,stroke:#000,color:#000
    classDef depth2 fill:#BBF,stroke:#000,color:#000
    classDef depth1 fill:#B9B,stroke:#000,color:#000
    classDef depth0 fill:#FBB,stroke:#000,color:#000

    linkStyle default stroke-width:2px,fill:none,stroke:green;

    app(depth: 2):::depth2

    screen1(depth: 1):::depth1
    screen2(depth: 1):::depth1
    screen3(depth: 1):::depth1
    screen4(depth: 1):::depth1

    lib1(depth: 0):::depth0
    lib2(depth: 0):::depth0
    lib3(depth: 0):::depth0
    lib4(depth: 0):::depth0
    lib5(depth: 0):::depth0

    app --> screen1
    app --> screen2
    app --> screen3
    app --> screen4

    screen1 --> lib1
    screen1 --> lib4

    screen2 --> lib1
    screen2 --> lib3
    screen2 --> lib4

    screen3 --> lib2
    screen3 --> lib3
    screen3 --> lib4

    screen4 --> lib3
    screen4 --> lib5

  end

  style sg opacity:0.0

```

On the other hand, "deep" projects do not offer many opportunities for parallelization. They have
project dependencies which must be built *sequentially*. They also perform poorly in incremental
builds, because a single change to even a mid-level module invalidates cached builds for half of the
project.

```mermaid
flowchart  TB

  style sg opacity:0.0
  subgraph sg [A deep graph]
    direction TB

    classDef depth6 fill:#800,stroke:#000,color:#FFF
    classDef depth5 fill:#A50,stroke:#000,color:#FFF
    classDef depth4 fill:#C0B,stroke:#000,color:#000
    classDef depth3 fill:#F7B,stroke:#000,color:#000
    classDef depth2 fill:#BBF,stroke:#000,color:#000
    classDef depth1 fill:#B9B,stroke:#000,color:#000
    classDef depth0 fill:#FBB,stroke:#000,color:#000

    linkStyle default stroke-width:2px,fill:none,stroke:green;

    app(depth: 6):::depth6

    screen1(depth: 5):::depth5
    screen2(depth: 5):::depth5

    screen3(depth: 4):::depth4
    screen4(depth: 4):::depth4

    lib1(depth: 3):::depth3
    lib2(depth: 3):::depth3

    lib3(depth: 2):::depth2
    lib4(depth: 2):::depth2

    lib5(depth: 1):::depth1

    lib6(depth: 0):::depth0

    app --> screen1
    app --> screen2
    app --> screen3
    app --> screen4

    screen1 --> screen3
    screen1 --> screen4

    screen2 --> screen4

    screen3 --> lib1
    screen3 --> lib2

    screen4 --> lib1
    screen4 --> lib4

    lib1 --> lib3
    lib1 --> lib4

    lib2 --> lib3

    lib3 --> lib5
    lib4 --> lib5

    lib5 --> lib6

  end

```
