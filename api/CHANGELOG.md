# CensorCraft API Changelog

## [3.0.0]

### Changed

- `Trie` getter methods were renamed and given proper Javadoc descriptions.

### Removed

- `Trie.update` to encourage instantiating new objects rather than updating it.

## [2.0.0]

### Added

- Client-side punishments now have a dedicated function that can be optionally implemented.
- `ClientPunishEvent` to hook into running sided code.
- `ChatTabooEvent`, allowing you to intercept what gets sent to chat.

### Changed

- `PunishEvent` was renamed to `ServerPunishEvent`.
- Punishments are now serializable and send their instance variables to the client, allowing for one-way (server to
  client) communication.
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
