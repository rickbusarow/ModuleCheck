---
id: must_be_api
slug: /rules/must_be_api
title: Must Be Api
sidebar_label: Must Be Api
---

Dependencies are considered to be part of a module's public "ABI" if that module exposes some aspect
of the dependency in its own API.

For instance, if a `:moduleA` extends a class/interface from `:moduleB`, or takes a type
from `:moduleB` as a function parameter, then any consumer of `:moduleA`'s API must also have a
dependency upon `:moduleB`. In scenarios like this, the dependency module(s) should be declared
using Gradle's `api` configuration.
