# Changelog

## 2.3.7

- Fixed a deserialization bug. If your macros were reset when upgrading to v2.3.6,
install v2.3.7 and check the `config` folder of your Minecraft instance for a 
`commandkeys.unreadable.json` file. If it exists, delete your `commandkeys.json` 
file, rename the `unreadable` file to `commandkeys.json`, and start the game again.
- Updated German translation (Lucanoria)
- Updated Russian translation (rfin0)

## 2.3.6

- Added high contrast button textures
- Added option to allow repeating macros to resume on re-activation of profile

## 2.3.5

- Prevented blank messages from being automatically removed from type-mode macros on save

## 2.3.4

- Fixed macros becoming unresponsive on deletion of a macro with the same keybind

## 2.3.3

- Added Ukrainian translation (ttrafford7)

## 2.3.2

- Add Russian translation (rfin0)

## 2.3.1

- Update German translation (Lucanoria)

## 2.3.0

- Added activation ratelimiter to prevent spam on servers 
- Added alternate keybind to allow reversal of cycling macros 
- Added placeholders for the block that the player is looking at 
- Fixed messages sending with incomplete placeholder replacement
