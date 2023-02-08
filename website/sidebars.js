/*
 * Copyright (C) 2021-2023 Rick Busarow
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

module.exports = {
  Docs: [
    "quickstart",
    "configuration",
    "suppressing-findings",
    "ci-workflow",
    {
      type: "category",
      label: "Rules",
      collapsed: false,
      items: [
        "rules/unused_dependency",
        "rules/must_be_api",
        "rules/inherited_dependency",
        "rules/redundant_dependency",
        "rules/overshot_dependency",
        "rules/project_depth",
        {
          type: "category",
          label: "Compiler",
          collapsed: false,
          items: [
            "rules/compiler/use_anvil_factory_generation",
            "rules/compiler/unused_kapt_processor",
            "rules/compiler/unused_kapt_plugin",
            "rules/compiler/custom_kapt_matchers",
          ],
        },
        {
          type: "category",
          label: "Sorting",
          collapsed: false,
          items: [
            "rules/sorting/sort_dependencies",
            "rules/sorting/sort_plugins"
          ],
        },
        {
          type: "category",
          label: "Android",
          collapsed: false,
          items: [
            "rules/android/disable_android_resources",
            "rules/android/disable_view_binding",
            "rules/android/unused_kotlin_android_extensions",
          ],
        }
      ],
    },
  ],
};
