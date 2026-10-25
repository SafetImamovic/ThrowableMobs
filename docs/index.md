---
layout: home

hero:
  name: Throwable Mobs
  text: Pick them up. Throw them.
  tagline: A Minecraft mod for Fabric and NeoForge. Charge a throw, see where it lands, and watch every mob react. Catch your friends' throws mid-air.
  image:
    src: /throwablemobs.png
    alt: A pig flying through the air
  actions:
    - theme: brand
      text: Download
      link: https://www.curseforge.com/minecraft/mc-mods/throwablemobs
    - theme: alt
      text: Read the guide
      link: /guide/
    - theme: alt
      text: GitHub
      link: https://github.com/SafetImamovic/ThrowableMobs

features:
  - icon: 🎯
    title: Charged throws
    details: Hold to charge like a bow. A trajectory preview shows exactly where the mob lands, bounces included. Light mobs fly far, heavy ones barely leave your hands.
  - icon: 💥
    title: Every mob reacts
    details: Creepers explode, endermen teleport and come for you, slimes bounce and split, chickens float down and lay eggs. Modded mobs act like the vanilla mob they're built on.
  - icon: 🧤
    title: Catch mid-air
    details: Grab a thrown mob out of the air. An outline shows which ones you can catch. Throw a creeper to a friend and hope they're paying attention.
  - icon: 🧩
    title: Data-driven
    details: Every number and behavior is set per mob with datapack mob profiles and tags. Other mods add their own behaviors through a Java API.
    link: /guide/
    linkText: Read the guide
---

![Picking up a pig on a beach, charging the throw and throwing it across the sand](/screenshots/throw-pig.gif)

![Another player throws a creeper, and it explodes where it lands](/screenshots/creeper-throw.gif)

## Install

Minecraft Java **1.21.1** or **26.3**, on the client **and** the server. Both versions have the same features.

| Loader | Also install |
|---|---|
| Fabric | [Fabric API](https://modrinth.com/mod/fabric-api), [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) |
| NeoForge (21.1.197 or newer for 1.21.1, 26.3.0.95 or newer for 26.3) | Nothing |

## Controls

| Action | Default |
|---|---|
| Pick up / put down | Left Shift + Right Click |
| Throw | Hold Right Click while carrying, let go to throw |
| Cancel throw | Left Click while charging |

Rebind them under Controls → Throwable Mobs, or type `/throwablemobs keybinds`. With Carry On installed, it gets the
shared keys and a chat message explains how to rebind.

## Commands

| Command | Who | What |
|---|---|---|
| `/throwablemobs help` | Everyone | Lists the commands |
| `/throwablemobs config` | Everyone | Opens the settings |
| `/throwablemobs keybinds` | Everyone | Opens Controls |
| `/throwablemobs profile <mob>` | Operators | Shows a mob's profile and where each value comes from |
