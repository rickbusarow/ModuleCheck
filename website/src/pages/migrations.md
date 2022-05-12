## 0.12.0 to 0.12.1

### Standardized Finding names {#standardized-finding-names}

The names of all findings have been updated/standardized. Any declarations which were suppressing
a finding with the old ID (via `@Suppress("someFinding")` or `//suppress=someFinding`) will still
work, but they should be updated to use the new names.

| old name                      | new name                         |
|:------------------------------|:---------------------------------|
| depth                         | project-depth                    |
| disableAndroidResources       | disable-android-resources        |
| disableViewBinding            | disable-view-binding             |
| inheritedDependency           | inherited-dependency             |
| mustBeApi                     | must-be-api                      |
| overshot                      | overshot-dependency              |
| redundant                     | redundant-dependency             |
| unsortedDependencies          | sort-dependencies                |
| unsortedPlugins               | sort-plugins                     |
| useAnvilFactories             | use-anvil-factory-generation     |
| unused                        | unused-dependency                |
| unusedKaptProcessor           | unused-kapt-processor            |
| unusedKotlinAndroidExtensions | unused-kotlin-android-extensions |

### CodeGeneratorBinding {#code-generator-binding}

The `KaptMatcher` class has been deprecated in favor of `modulecheck.config.CodeGeneratorBinding`.
This new sealed class has four concrete implementations which can define just about any code
generation tool:

- `modulecheck.config.CodeGeneratorBinding.AnnotationProcessor`
  - Note that this class is a model for Kotlin KAPT as well as a standard Java annotation processor.
- `modulecheck.config.CodeGeneratorBinding.KspExtension`
- `modulecheck.config.CodeGeneratorBinding.AnvilExtension`
- `modulecheck.config.CodeGeneratorBinding.KotlinCompilerPlugin`

To migrate from `KaptMatcher`:

1. Change references of `modulecheck.api.KaptMatcher`,
   to `modulecheck.config.CodeGeneratorBinding.AnnotationProcessor`.
2. Replace the regex-styled `annotationImports` arguments with explicit fully-qualified names.
   For instance, replace `"com\\.example\\.MyAnnotation"`, with `"com.example.MyAnnotation"`.
