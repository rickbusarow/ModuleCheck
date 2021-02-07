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
      label: "Kapt",
      collapsed: false,
      items: [
        'rules/kapt/unused_kapt_processor',
        'rules/kapt/unused_kapt_plugin',
        'rules/kapt/custom_kapt_matchers',
      ]
    },
    {
      type: "category",
      label: "Sorting",
      collapsed: false,
      items: [
        'rules/sorting/sort_dependencies',
        'rules/sorting/sort_plugins',
      ]
    },
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
  Features: ['mdx', ]
};
