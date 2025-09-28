## [3.0.0] - 9/28/25

### Added

- `/censorcraft [enable/disable]` commands to quickly turn on / off punishing in-game.
- `enable_censorcraft` setting in server config (ties to the `/censorcraft` command).

### Changed

- The built-in punishments (explosion, lightning, etc.) are now internally considered a plugin, which results in the
  server config file writing their settings as "censorcraft.punishment_name". 
