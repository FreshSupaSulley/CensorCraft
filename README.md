# CensorCraft

CensorCraft is a Forge Minecraft mod that actively listens to your voice with [Simple Voice Chat](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat) and punishes you for saying forbidden words.

# Setup

Install it on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/censorcraft)! See the [wiki](https://github.com/FreshSupaSulley/CensorCraft/wiki) for setup instructions.

# How It Works
When you first try to join a world, you'll be asked to download a whisper model to your client config folder (see models below). When in-game, your audio feed is captured and transcribed locally in real-time. If you speak a forbidden word, all sorts of punishment may be incurred. Punishments are set by the server admin.

This mod comes with a handful of default punishments, but it's extendable with plugins.

## Project Structure

This is a multi-project gradle build:

- [forge](./forge)
Forge mod source. Depends on all other subprojects.
- [common](./common)
The non-forge specific code of the mod. Mainly keeping it out of forge just in case that one day we want to support different mod loaders (unlikely).
- [api](./api)
The CensorCraft API code, allowing you to hook into CensorCraft to create your own punishment types or messing with internal events.

# Contributing

Do whatever you want with this repository, such as porting to another mod loader or forking for a project of your own. Open an issue if you're having trouble, but pull requests are encouraged!
