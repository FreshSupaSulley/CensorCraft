# CensorCraft

CensorCraft is a Forge Minecraft mod that actively listens to your voice with [Simple Voice Chat](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat) and punishes you for saying forbidden words.

# Setup

Install it on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/censorcraft)! See the [wiki](https://github.com/FreshSupaSulley/CensorCraft/wiki) for setup instructions.

# How it works
When you first try to join a world, you'll be asked to download a [whisper.cpp](https://github.com/ggml-org/whisper.cpp) model to your client config folder. When in-game, that model is used to transcribe your voice in real-time. If you speak a forbidden word, punishments are ran, and they are set by the server admin.

This mod comes with a handful of default punishments, but it's extendable with plugins.

## Project structure

This is a multi-project gradle build:

- [forge](./forge)
Forge-specific code.
- [common](./common)
The meat of the mod, no modloader code.
- [api](./api)
The CensorCraft API project, allowing you to hook into CensorCraft to create your own punishments or mess with internal events.

# Contributing

Pull requests are encouraged!
