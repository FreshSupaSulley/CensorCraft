# CensorCraft API Changelog

## [2.0.0]

### Added

- Client-side punishments now have a dedicated function that can be optionally implemented.
- `ClientPunishEvent` and `ServerPunishEvent` events to hook into running sided code.
- `ChatTabooEvent`.

### Changed

- Punishments are now serializable and send their instance variables to the client, allowing for server-client
  communication.
- Punishments now need to have unique IDs with respect to other punishments defined in the same plugin. More
  importantly, plugins also need to have unique IDs with respect to every other plugin loaded. This is because we write
  to the server config file based off of the concatenation of the plugin ID and the punishment ID and therefore we need
  to ensure proper separation.

### Removed

- `ClientAcknowledgePunish` in favor of hooking into `ClientPunishEvent`.
- `PunishEvent`, as it was separated into the two aforementioned events.

## [1.0.0]

### Added

- Welcome to the CensorCraft API! Register custom punishments and hook into internal events.
