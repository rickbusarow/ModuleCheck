/*
 * Copyright (C) 2021-2024 Rick Busarow
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
import mermaid from 'mdx-mermaid';

const {themes} = require('prism-react-renderer');
const lightTheme = themes.github;
const darkTheme = themes.dracula;

module.exports = {
  title: "ModuleCheck",
  tagline: "Fast dependency graph linting for Gradle projects",
  url: "https://rbusarow.github.io/",
  baseUrl: "/ModuleCheck/",
  onBrokenLinks: "throw",
  onBrokenMarkdownLinks: "throw",
  favicon: "img/favicon.ico",
  markdown: {
    mermaid: true,
  },
  themes: ['@docusaurus/theme-mermaid'],
  organizationName: "rickbusarow",
  projectName: "ModuleCheck",
  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          remarkPlugins: [mermaid],
          sidebarPath: './sidebars.js',
          editUrl: "https://github.com/rbusarow/ModuleCheck/blob/main/website",
        },
        blog: {
          showReadingTime: true,
          editUrl: "https://github.com/rbusarow/ModuleCheck",
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],
  themeConfig:
  /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
      ({
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
        // announcementBar: {id: "supportus", content: '⭐️ If you like ModuleCheck, give it a star on <a target="_blank" rel="noopener noreferrer" href="https://github.com/rickbusarow/ModuleCheck/">GitHub</a>! ⭐️',},
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
              to: 'changelog',
              label: 'ChangeLog',
              position: 'left'
            },
            {
              to: 'migrations',
              label: 'Migrations',
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
              label: "Api",
              href: 'pathname:///api/index.html',
              position: "left",
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
        mermaid: {
          theme: {
            light: 'neutral',
          },
          options: {},
        },
        prism: {
          theme: lightTheme,
          darkTheme: darkTheme,
          additionalLanguages: ["kotlin", "groovy", "java"],
        },
        algolia: {
          // The application ID provided by Algolia
          appId: 'D6Z21RYLG1',

          // Public API key: it is safe to commit it
          apiKey: '2b25d0dd3470c3fdbe2ffa4e3299b0e9',

          indexName: 'modulecheck',

          // Optional: see doc section below
          contextualSearch: true,

          // Optional: Algolia search parameters
          searchParameters: {},

          // Optional: path for search page that enabled by default (`false` to disable it)
          searchPagePath: 'search',

          //... other Algolia params
        },
      }),
};
