/*
 * Copyright (C) 2021 Rick Busarow
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
    "ci-workflow",
    {
      type: "category",
      label: "Rules",
      collapsed: false,
      items: [
        "rules/unused",
        "rules/must_be_api",
        "rules/inherited_dependency",
        {
          type: "category",
          label: "compiler",
          collapsed: false,
          items: [
            "rules/compiler/could_use_anvil_factory",
          ],
        },
        {
          type: "category",
          label: "Kapt",
          collapsed: false,
          items: [
            "rules/kapt/unused_kapt_processor",
            "rules/kapt/unused_kapt_plugin",
            "rules/kapt/custom_kapt_matchers",
          ],
        },
        {
          type: "category",
          label: "Sorting",
          collapsed: false,
          items: ["rules/sorting/sort_dependencies", "rules/sorting/sort_plugins"],
        },
        {
          type: "category",
          label: "Android",
          collapsed: false,
          items: [
            "rules/android/disable_resources",
            "rules/android/disable_viewbinding",
          ],
        }
      ],
    },
  ],
};
