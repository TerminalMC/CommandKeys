# Changelog

## 2.4.0

Changes over latest beta

- Fixed ordering of macro activator MC keybinds.
- Updated Russian translation (rfin0)

Changes over latest release

- Added support for activating macros using MC keybinds.
- Added a button to copy macros in the profile editor.
- Added a new activation mode for key Release.
- Added a command to switch profiles.
- Added an optional list entry name field for macros.
- Changed default Conflict Strategy from Submit to Assert.
- Increased command/message field length limit from 512 to 4096.
- Fixed conflict handling for debug keys on 1.21.11

## 2.4.0-beta.1

- Added support for activating macros using MC keybinds.
- Added a button to copy macros in the profile editor.
- Added a new activation mode for key Release.
- Added a command to switch profiles.
- Added an optional list entry name field for macros.
- Changed default Conflict Strategy from Submit to Assert.
- Increased command/message field length limit from 512 to 4096.
- Fixed conflict handling for debug keys on 1.21.11

## 2.3.17

- Fixed an issue causing toggle actions to not be cancelled when activating macros in veto mode.

## 2.3.16

- Fixed an issue causing the first typed character to not register after activating a type or edit
  mode macro with a non-character key

## 2.3.15

- Added South Korean translation (doyoung07)

## 2.3.14

- Added `EDIT` mode, which allows you to manually enter replacements for a new `%edit%` placeholder
  when sending

## 2.3.13

- Added an option to cap the number of repetitions when using repeat mode

## 2.3.12

- Fixed invisible text in message length limit field on 1.21.6+

## 2.3.11

- Added a configurable message length limit to mitigate being kicked when sending overlength
  messages

## 2.3.10

- Execute non-delayed macros immediately to preserve relative ordering (Starjon)

## 2.3.9

- Updated German translations (Lucanoria)

## 2.3.8

- Improved stability of deserializers
- Fixed an issue where editing a copy of a profile would edit the original
- Added support for reloading config file to revert edits
- Enabled cancellation of movement keys
- Added support for keeping macros activated while the keybind is held
- Added support for delaying cycling messages
- Improved macro activation via options screen
- Added enhanced text fields from ChatNotify

## 2.3.7

- Fixed a deserialization bug. If your macros were reset when upgrading to v2.3.6, install v2.3.7
  and check the `config` folder of your Minecraft instance for a
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
