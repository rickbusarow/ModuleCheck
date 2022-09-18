/*
 * Copyright (C) 2021-2022 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package modulecheck.builds

import org.gradle.api.Project

/**
 * Determines if this project is the root project **and** root of a composite build, if it's part of
 * a composite build.
 *
 * A composite build is a build using 'includeBuild(...)' in settings.gradle[.kts]. In composite
 * builds, the root of an included build is also a `rootProject` inside that included build. So
 * within that composite build, there are multiple projects for which `project == rootProject` would
 * return true.
 *
 * The Project property [gradle][org.gradle.api.Project.getGradle] refers to the specific
 * [gradle][org.gradle.api.invocation.Gradle] instance in that invocation of `./gradlew`, and the
 * only time [gradle.parent][org.gradle.api.invocation.Gradle.getParent] is null is when it's at the
 * true root of that tree.
 *
 * @return true if this project is the root of the entire build, else false
 */
fun Project.isRootOfComposite(): Boolean {
  return this == rootProject && gradle.parent == null
}
