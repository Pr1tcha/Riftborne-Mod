# Codex MVP Status

Source: `TZ_Karmanny_Kodex_Riftborne.pdf`, version 0.1 dated 2026-06-22.

## Implemented

- Pocket Codex reworked into a compact field recorder rather than a second laptop.
- Scanner, Pulse, and eight-slot Buffer modes with clickable tabs and `V`/`G` controls.
- Server-authoritative target scanning and radius-based anomaly detection.
- Three observation levels per target, plus queued and damaged packet states.
- Data-driven entity and block scan rules loaded from `codex_scan_targets` JSON files.
- Scan rules support registry IDs, tags, priorities, damaged results, and linked Infobase articles.
- Separate field-recorder Blockbench source, runtime geometry, animations, textures, and inventory icon.
- Codex Dock block with one persistent Pocket Codex slot.
- Physical synchronization requiring an adjacent Codex Laptop.
- Normal packets become laptop Infobase entries after synchronization.
- Damaged records enter the laptop Decryptor queue.
- Data-driven laptop Infobase with sections, search, categories, history, and navigation.
- Minimal Decryptor prototype that restores a selected damaged record.
- English and Russian localization for the new flow.

## Next Pass

- In-game pose and scale polish for first-person, third-person, and both hands.
- Sound and particles for scan success, pulse contacts, warnings, and dock transfer.
- Timed or interactive Decryptor mini-game instead of immediate restoration.
- Dock charging and battery consumption.
- Automatic field records from first encounters and scripted events.
- Dedicated visual model and inserted-device state for the dock.
- Multiplayer validation and in-game visual QA.

## Explicitly Excluded

- Flash drive implementation and progression are not modified in this pass.
