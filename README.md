# CensorCraft

CensorCraft is a Forge Minecraft mod that actively listens to your voice with [Simple Voice Chat](https://www.curseforge.com/minecraft/mc-mods/simple-voice-chat) and punishes you for saying forbidden words.

Install it on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/censorcraft)!

# How It Works
When you first try to join a world, you'll be asked to download a model to your client config folder (see models below). When in-game, your audio feed is captured and transcribed locally in real-time. If you speak a forbidden word, all sorts of punishment may be incurred, such as:

- Exploding the player
- Striking the player with lightning
- Summoning any mob by the player
- Giving the player any potion effect
- Teleporting the player to a random dimension
- Instantly killing the player
- Running any command
- … etc.

Punishments are set by the server admin. All punishment options are available in the server config file.

## Project Structure

This is a multi-project gradle build:

- [forge](./forge)
Forge mod source. Depends on JScribe.
- [jscribe](./jscribe)
Transcribes speech-to-text using (technically a fork of) [whisper-jni](https://github.com/GiviMAD/whisper-jni).

# Contributing

Do whatever you want with this repository, such as porting to another mod loader or forking for a project of your own. Feel free to make pull requests or open issues.
