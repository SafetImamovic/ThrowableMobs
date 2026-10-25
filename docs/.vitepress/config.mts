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
          { text: 'Download (CurseForge)', link: 'https://www.curseforge.com/minecraft/mc-mods/throwablemobs' },
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

    socialLinks: [
      { icon: 'github', link: repo },
      // Simple Icons' CurseForge logo (CC0); VitePress 1.x has no built-in one.
      {
        icon: { svg: '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M18.326 9.2145S23.2261 8.4418 24 6.1882h-7.5066V4.4H0l2.0318 2.3576V9.173s5.1267-.2665 7.1098 1.2372c2.7146 2.516-3.053 5.917-3.053 5.917L5.0995 19.6c1.5465-1.4726 4.494-3.3775 9.8983-3.2857-2.0565.65-4.1245 1.6651-5.7344 3.2857h10.9248l-1.0288-3.2726s-7.918-4.6688-.8336-7.1127z"/></svg>' },
        link: 'https://www.curseforge.com/minecraft/mc-mods/throwablemobs',
        ariaLabel: 'CurseForge',
      },
    ],

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
