module.exports = {
  Basics: [
  'quickstart',
  'configuration',
  'changelog',
  ],
  Rules: [
    'rules/unused',
    'rules/redundant',
    'rules/overshot',
    {
      type: "category",
      label: "Android",
      collapsed: false,
      items: [
        'rules/android/disable_resources',
        'rules/android/disable_viewbinding',
      ]
    },
  ],
  Features: ['mdx',]
};

