# Credits

Buddy Battles is an addon for [Buddycards](https://www.curseforge.com/minecraft/mc-mods/buddycards)
([source](https://github.com/wildcardgamez/Buddycards-Core)) by **wildcard_gamer**,
with contributions from **LegoMaster3650** and **agnor99**.

## What's used, and from where

Permission was explicitly requested and granted (Discord) to use the following from
Buddycards' `1.20` branch - the last version where the battle system existed in
code, before it was removed for 1.21.1:

- **Card names, stats, and abilities/effects** - the actual cost/power/ability
  logic for every ported card (see `engine/cards/`) comes from `1.20`'s
  `BuddycardsItems.java`, not invented. Card names themselves come from
  Buddycards' own item registrations/lang file.
- **GUI textures and fonts** - the deckbox screen (`deckbox.png`) is Buddycards' own
  asset, referenced in place from the Buddycards jar rather than copied. The battle
  screen's `playmat.png` started the same way but is now a custom asset for this addon
  (redrawn/enlarged main window) that still retains Buddycards' original log-panel
  border and icon-strip regions byte-for-byte (just relocated within the file) -
  covered by the same permission grant. The `smallnumbers` bitmap font is still
  Buddycards' own, referenced in place.
- The card backings were also granted by direct request as a resource to be used.
- **Deckbox item icon** (`textures/item/deckbox.png`) - the real "closed" (full)
  deckbox art from the `1.20` branch, copied into this addon's own resources (it
  doesn't exist on `main` to reference in place - see PORTING_NOTES.md).
- **"Buddies" synergy tag membership** (`CardTags` and the datapack tag files under
  `data/buddybattles/tags/item/battles/`) - which cards belong to which synergy
  grouping (fire buddies, animal buddies, etc.) is ported from `1.20`'s own tag data.

## What's original to this addon

- The engine restructuring itself (`BattleBoard`/`BattleSession`/`BattleParticipant`
  replacing the block-coupled `BattleContainer`/`PlaymatBlockEntity` design)
- The villager-challenge trigger, the request/accept flow for player challenges, the
  damage-interrupt rule, and `VillagerBattleAI`.
- Cards without abilities or implementation in the 1.20 branch were created.