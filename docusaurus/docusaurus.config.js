module.exports = {
  title: 'ModuleCheck',
  tagline: 'Fast dependency graph linting for Gradle projects',
  url: 'https://rbusarow.github.io/ModuleCheck/',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'rbusarow', // Usually your GitHub org/user name.
  projectName: 'ModuleCheck', // Usually your repo name.
  themeConfig: {
    navbar: {
      title: 'ModuleCheck',
//      logo: {
//        alt: 'ModuleCheck Logo',
//        src: 'img/logo.svg',
//      },
      items: [
        {
          type: 'doc',
          docId: 'quickstart',
          label: 'Basics',
          position: 'left'
        },
        {
          type: 'doc',
          docId: 'rules/unused',
          label: 'Rules',
          position: 'left',
        },
        {
          to: 'blog',
          label: 'Blog',
          position: 'left'
        },
        {
          href: 'https://github.com/rbusarow/ModuleCheck/',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Change Log',
              to: 'docs/',
            },
            {
              label: 'Second Doc',
              to: 'docs/doc2/',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'Stack Overflow',
              href: 'https://stackoverflow.com/questions/tagged/ModuleCheck',
            },
            {
              label: 'Twitter',
              href: 'https://twitter.com/rbusarow',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Blog',
              to: 'blog',
            },
            {
              label: 'GitHub',
              href: 'https://github.com/rbusarow/ModuleCheck/',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Rick Busarow, Built with Docusaurus.`,
    },
    prism: {
      theme: require('prism-react-renderer/themes/dracula'),
      additionalLanguages: ['kotlin', 'groovy'],
    },
  },
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          editUrl:
            'https://github.com/rbusarow/ModuleCheck/',
        },
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          editUrl:
            'https://github.com/rbusarow/ModuleCheck/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
  ],
};
