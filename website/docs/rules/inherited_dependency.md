---
id: inherited_dependency
slug: /rules/inherited_dependency
title: Inherited Dependency
sidebar_label: Inherited Dependency
---

Assume that `:moduleA` depends upon `:moduleB`, and `:moduleB` depends upon `:moduleC` via
an `api` configuration.  Also assume that `:moduleA` uses something from `:moduleC`, but doesn't
have an explicit dependency for it.  It just inherits that dependency from `:moduleB`.

ModuleCheck will recommend adding a direct, explicit dependency for `:moduleA` -> `:moduleC`.
