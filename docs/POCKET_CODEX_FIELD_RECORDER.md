# Pocket Codex: Field Recorder

The Pocket Codex is a field instrument, not a second Codex Laptop. Its job is to notice an anomaly, collect a small observation packet, and carry that packet back to a dock. The laptop remains the place where full Infobase articles are stored and read.

## Field loop

1. Select **Scanner** and aim at a registered block or entity.
2. Activate the recorder to collect an observation.
3. Repeat the scan up to three times to improve the packet.
4. Use **Pulse** when the exact location is unknown. Pulse reports nearby registered signatures but does not collect them.
5. Review the eight-slot **Buffer**.
6. Insert the recorder into a Codex Dock next to a laptop to transfer packets into the Infobase.

Damaged observations are transferred into the laptop's recovery/decryption flow. The Pocket Codex never grants techniques or replaces training.

## Controls

- Right click: open the compact field interface.
- `V`: cycle Scanner, Pulse, and Buffer.
- `G`: activate the selected mode.
- The three tabs and action button can also be clicked.

## Adding a scannable mob or block

Scan rules are datapack JSON files in:

`src/main/resources/data/riftborne/codex_scan_targets/`

The linked Infobase article lives in:

`src/main/resources/data/riftborne/codex_entries/`

Example entity rule:

```json
{
  "type": "entity",
  "targets": ["minecraft:zombie"],
  "entry": "riftborne:zombie_field_note",
  "damaged": false,
  "priority": 10
}
```

Tags are supported, so one rule can cover a whole family:

```json
{
  "type": "entity",
  "targets": ["#riftborne:rift_creatures"],
  "entry": "riftborne:rift_creatures",
  "damaged": false,
  "priority": 20
}
```

Block rules use `"type": "block"` and block IDs or block tags. Higher priority wins when several rules match the same target. Changes can be loaded with `/reload`; Java code does not need to be changed.

Run `tools/validate_infobase.py` before launching the game. It checks both the article files and every scan rule, including broken article links.

## Model ownership

The new field-recorder source is `bbmodels/Pocket_Codex_Field_Recorder.bbmodel`. The older artist model `bbmodels/Pocket_Codex.bbmodel` is kept untouched.
