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

module.exports = {
  title: "ModuleCheck",
  tagline: "Fast dependency graph linting for Gradle projects",
  url: "https://rbusarow.github.io/",
  baseUrl: "/ModuleCheck/",
  onBrokenLinks: "throw",
  onBrokenMarkdownLinks: "warn",
  favicon: "img/favicon.ico",
  organizationName: "rbusarow", // Usually your GitHub org/user name.
  projectName: "ModuleCheck", // Usually your repo name.
  themeConfig: {
    docs: {
      sidebar: {
        hideable: true,
      }
    },
    colorMode: {
      defaultMode: "light",
      disableSwitch: false,
      respectPrefersColorScheme: true,
    },
    announcementBar: {
      id: "supportus",
      content:
        '⭐️ If you like ModuleCheck, give it a star on <a target="_blank" rel="noopener noreferrer" href="https://github.com/rbusarow/ModuleCheck/">GitHub</a>! ⭐️',
    },
    navbar: {
      title: "ModuleCheck",
      logo: {
        alt: 'ModuleCheck Logo',
        src: 'img/logo.png',
      },
      items: [
        {
          type: "doc",
          docId: "quickstart",
          label: "Docs",
          position: "left",
        },
        {
          to: 'CHANGELOG',
          label: 'ChangeLog',
          position: 'left'
        },
        {
          type: "docsVersionDropdown",
          position: "left",
          dropdownActiveClassDisabled: true,
          dropdownItemsAfter: [
            {
              to: "/changelog",
              label: "CHANGELOG",
            },
          ],
        },
        {
          label: "Twitter",
          href: "https://twitter.com/rbusarow",
          position: "right",
        },
        {
          label: "GitHub",
          href: "https://github.com/rbusarow/ModuleCheck",
          position: "right",
        },
      ],
    },
    footer: {
      copyright: `Copyright © ${new Date().getFullYear()} Rick Busarow, Built with Docusaurus.`,
    },
    prism: {
      theme: require("prism-react-renderer/themes/github"),
      darkTheme: require("prism-react-renderer/themes/dracula"),
      additionalLanguages: ["kotlin", "groovy", "java"],
    },
  },
  presets: [
    [
      "@docusaurus/preset-classic",
      {
        docs: {
          remarkPlugins: [require('mdx-mermaid')],
          sidebarPath: require.resolve("./sidebars.js"),
          editUrl: "https://github.com/rbusarow/ModuleCheck",
        },
        blog: {
          showReadingTime: true,
          editUrl: "https://github.com/rbusarow/ModuleCheck",
        },
        theme: {
          customCss: require.resolve("./src/css/custom.css"),
        },
      },
    ],
  ],
};
