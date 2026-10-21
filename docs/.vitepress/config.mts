import { defineConfig } from 'vitepress'

const repo = 'https://github.com/SafetImamovic/ThrowableMobs'

// Served from GitHub Pages at /ThrowableMobs/, built by .github/workflows/docs.yml.
export default defineConfig({
  title: 'Throwable Mobs',
  description: 'Pick up mobs, throw them, catch them mid-air. Docs for players, pack devs and mod devs.',
  lang: 'en-US',
  base: '/ThrowableMobs/',
  cleanUrls: true,
  lastUpdated: true,
  // Local planning notes live next to the guide but are gitignored; never publish them.
  srcExclude: ['plans/**'],
  // The guide's index stays README.md so it also reads well on GitHub.
  rewrites: {
    'guide/README.md': 'guide/index.md',
  },
  head: [['link', { rel: 'icon', type: 'image/png', href: '/ThrowableMobs/throwablemobs.png' }]],

  themeConfig: {
    logo: '/throwablemobs.png',

    nav: [
      { text: 'Guide', link: '/guide/' },
      { text: 'Cookbook', link: '/guide/cookbook' },
      {
        text: 'Links',
        items: [
          { text: 'Download (GitHub releases)', link: `${repo}/releases` },
          { text: 'Changelog', link: `${repo}/blob/master/CHANGELOG.md` },
          { text: 'Report a bug', link: `${repo}/issues/new/choose` },
        ],
      },
    ],

    sidebar: {
      '/guide/': [
        {
          text: 'Guide',
          items: [{ text: 'Overview', link: '/guide/' }],
        },
        {
          text: 'Server admins',
          items: [{ text: 'Server config', link: '/guide/server-config' }],
        },
        {
          text: 'Pack devs',
          items: [
            { text: 'Mob profiles', link: '/guide/mob-profiles' },
            { text: 'Behaviors', link: '/guide/behaviors' },
            { text: 'Tags', link: '/guide/tags' },
            { text: 'Modded mobs', link: '/guide/modded-mobs' },
            { text: 'Advancements and sounds', link: '/guide/advancements' },
          ],
        },
        {
          text: 'Mod devs',
          items: [{ text: 'Java API', link: '/guide/java-api' }],
        },
        {
          text: 'Examples',
          items: [{ text: 'Cookbook', link: '/guide/cookbook' }],
        },
      ],
    },

    socialLinks: [{ icon: 'github', link: repo }],

    search: { provider: 'local' },

    editLink: {
      pattern: `${repo}/edit/master/docs/:path`,
      text: 'Edit this page on GitHub',
    },

    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.',
    },
  },
})
