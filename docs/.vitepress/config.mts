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
          { text: 'Download (Modrinth)', link: 'https://modrinth.com/mod/throwablemobs' },
          { text: 'Download (GitHub releases)', link: `${repo}/releases` },
          { text: 'Changelog', link: `${repo}/blob/master/CHANGELOG.md` },
          { text: 'Report a bug', link: `${repo}/issues/new/choose` },
          { text: 'Support on Ko-fi', link: 'https://ko-fi.com/safetimamovic' },
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
      // Simple Icons' Modrinth, CurseForge and Ko-fi logos (CC0); VitePress 1.x has no built-in ones.
      {
              icon: {
                      svg: '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12.252.004a11.78 11.768 0 0 0-8.92 3.73 11 10.999 0 0 0-2.17 3.11 11.37 11.359 0 0 0-1.16 5.169c0 1.42.17 2.5.6 3.77.24.759.77 1.899 1.17 2.529a12.3 12.298 0 0 0 8.85 5.639c.44.05 2.54.07 2.76.02.2-.04.22.1-.26-1.7l-.36-1.37-1.01-.06a8.5 8.489 0 0 1-5.18-1.8 5.34 5.34 0 0 1-1.3-1.26c0-.05.34-.28.74-.5a37.572 37.545 0 0 1 2.88-1.629c.03 0 .5.45 1.06.98l1 .97 2.07-.43 2.06-.43 1.47-1.47c.8-.8 1.48-1.5 1.48-1.52 0-.09-.42-1.63-.46-1.7-.04-.06-.2-.03-1.02.18-.53.13-1.2.3-1.45.4l-.48.15-.53.53-.53.53-.93.1-.93.07-.52-.5a2.7 2.7 0 0 1-.96-1.7l-.13-.6.43-.57c.68-.9.68-.9 1.46-1.1.4-.1.65-.2.83-.33.13-.099.65-.579 1.14-1.069l.9-.9-.7-.7-.7-.7-1.95.54c-1.07.3-1.96.53-1.97.53-.03 0-2.23 2.48-2.63 2.97l-.29.35.28 1.03c.16.56.3 1.16.31 1.34l.03.3-.34.23c-.37.23-2.22 1.3-2.84 1.63-.36.2-.37.2-.44.1-.08-.1-.23-.6-.32-1.03-.18-.86-.17-2.75.02-3.73a8.84 8.839 0 0 1 7.9-6.93c.43-.03.77-.08.78-.1.06-.17.5-2.999.47-3.039-.01-.02-.1-.02-.2-.03Zm3.68.67c-.2 0-.3.1-.37.38-.06.23-.46 2.42-.46 2.52 0 .04.1.11.22.16a8.51 8.499 0 0 1 2.99 2 8.38 8.379 0 0 1 2.16 3.449 6.9 6.9 0 0 1 .4 2.8c0 1.07 0 1.27-.1 1.73a9.37 9.369 0 0 1-1.76 3.769c-.32.4-.98 1.06-1.37 1.38-.38.32-1.54 1.1-1.7 1.14-.1.03-.1.06-.07.26.03.18.64 2.56.7 2.78l.06.06a12.07 12.058 0 0 0 7.27-9.4c.13-.77.13-2.58 0-3.4a11.96 11.948 0 0 0-5.73-8.578c-.7-.42-2.05-1.06-2.25-1.06Z"/></svg>',
              },
              link: "https://modrinth.com/mod/throwablemobs",
              ariaLabel: "Modrinth",
      },
      {
        icon: { svg: '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M18.326 9.2145S23.2261 8.4418 24 6.1882h-7.5066V4.4H0l2.0318 2.3576V9.173s5.1267-.2665 7.1098 1.2372c2.7146 2.516-3.053 5.917-3.053 5.917L5.0995 19.6c1.5465-1.4726 4.494-3.3775 9.8983-3.2857-2.0565.65-4.1245 1.6651-5.7344 3.2857h10.9248l-1.0288-3.2726s-7.918-4.6688-.8336-7.1127z"/></svg>' },
        link: 'https://www.curseforge.com/minecraft/mc-mods/throwablemobs',
        ariaLabel: 'CurseForge',
      },
      {
        icon: { svg: '<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M11.351 2.715c-2.7 0-4.986.025-6.83.26C2.078 3.285 0 5.154 0 8.61c0 3.506.182 6.13 1.585 8.493 1.584 2.701 4.233 4.182 7.662 4.182h.83c4.209 0 6.494-2.234 7.637-4a9.5 9.5 0 0 0 1.091-2.338C21.792 14.688 24 12.22 24 9.208v-.415c0-3.247-2.13-5.507-5.792-5.87-1.558-.156-2.65-.208-6.857-.208m0 1.947c4.208 0 5.09.052 6.571.182 2.624.311 4.13 1.584 4.13 4v.39c0 2.156-1.792 3.844-3.87 3.844h-.935l-.156.649c-.208 1.013-.597 1.818-1.039 2.546-.909 1.428-2.545 3.064-5.922 3.064h-.805c-2.571 0-4.831-.883-6.078-3.195-1.09-2-1.298-4.155-1.298-7.506 0-2.181.857-3.402 3.012-3.714 1.533-.233 3.559-.26 6.39-.26m6.547 2.287c-.416 0-.65.234-.65.546v2.935c0 .311.234.545.65.545 1.324 0 2.051-.754 2.051-2s-.727-2.026-2.052-2.026m-10.39.182c-1.818 0-3.013 1.48-3.013 3.142 0 1.533.858 2.857 1.949 3.897.727.701 1.87 1.429 2.649 1.896a1.47 1.47 0 0 0 1.507 0c.78-.467 1.922-1.195 2.623-1.896 1.117-1.039 1.974-2.364 1.974-3.897 0-1.662-1.247-3.142-3.039-3.142-1.065 0-1.792.545-2.338 1.298-.493-.753-1.246-1.298-2.312-1.298"/></svg>' },
        link: 'https://ko-fi.com/safetimamovic',
        ariaLabel: 'Ko-fi',
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
