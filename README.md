# CensorCraft

Minecraft Forge mod that listens to your voice and explodes you for speaking forbidden words.

# How it works

Each client records and transcribes their live audio feed locally before the text (what you said) is sent to the server, where it decides to explodes the player if they spoke a forbidden word.

# Installation

Use [CurseForge](https://www.curseforge.com/download/app) to run the mod. Other launchers have not been tested.

## macOS

In the CurseForge launcher, go to **Settings** and enable **Skip launcher with CurseForge**.

This mod requires using your microphone, and this setting allows the mod to request permission to use the microphone. When you first join a world, you'll see a popup asking to use the microphone. If you accidentally deny the mod permission to use it, you can change that in **System Preferences / Security & Privacy / Microphone**.

# Config

You can use the config button in the mods menu to edit basic **client** settings.

Use the server config file (*your_world/serverconfig/censorcraft-server.toml*) to edit **server** settings. This is where the magic happens, like changing the list of forbidden words, explosion parameters, ratting on players who aren't using their microphone, etc.

# Developers

## Project Structure

This is a multi-project gradle build:

- [forge](./forge)
Forge mod source. Depends on JScribe.
- [jscribe](./jscribe)
Records and transcribes speech-to-text. Depends on macrophone.
- [macrophone](./macrophone)
Allows macOS clients to interact with the microphone. I don't know if this is needed anymore.

Good luck building the forge dependencies on your first try lol

## Contributing
Do whatever you want with this repository, like porting CurseCraft to a different mod loader or ripping it off for a project of your own. Feel free to make pull requests or open issues.

# To-Do
* Rebuild whispercpp for Vulkan support
* Change audio recording file algorithm to try to be more live
* Test on all platforms
* Add Mac microphone helper methods back from macrophone
