# CensorCraft Changelog

## [4.0.0]
### Added
- `punish_everyone`, to punish every server player when one person says a taboo.
- Punishments now *override* global properties at the top of the file to allow for finer control over individual punishment configs. For example, `taboos` are still defined at the top of the file, but per-punishment configs can override this by having their own `taboos` section in their configs. This also applies to `punishment_cooldown`, `monitor_chat`, etc. Overriding `preferred_model` or `context_length` does nothing.

### Changed
- Renamed `global_taboos` to `taboos`.
- Renamed `enable` to `enabled`.

### Removed
- `ignore_global_taboos`, in favor of the override system.

## [3.0.0]

### Added

- `/censorcraft [enable/disable]` commands to quickly turn on / off punishing in-game.
- `enable_censorcraft` setting in server config (ties to the `/censorcraft` command).
- Scoreboard support to track who said the most punishments (see the [wiki](https://github.com/FreshSupaSulley/CensorCraft/wiki/server-setup#scoreboard)).

### Changed

- The built-in punishments (explosion, lightning, etc.) are now internally considered a plugin, which results in the
  server config file writing their settings as "censorcraft.punishment_name". 
