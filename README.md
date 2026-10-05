## Buddy Battles (Buddycards Add-On)

An addon for Buddycards that lets you battle NPCs and other players using your Buddycards.

Right-click a player, villager, piglin or enderling while holding a deckbox to start a
Buddycards duel.

- **Villagers, piglins and enderlings** duel instantly. Their AI is paused for the duel,
  and they drop a reward if you win.
- **Players** get a challenge request; they accept by right-clicking you back with
  their own deck.
- Right-clicking someone already in a duel spectates it (no spectate screen yet).
- After starting a duel against a mob, you can't re-challenge that same mob for
  `rebattleCooldownSeconds`, so a bad opening hand can't be rerolled by escaping.

Cards: Base (36), Nether (27), End (27) and Cave (27) sets have full battle stats and
abilities. Any other Buddycard falls back to a 1-cost, 1-power card with no ability.

### Config

| Key | Default | |
|---|---|---|
| `villagerDuels.enabled` / `piglinDuels.enabled` / `enderlingDuels.enabled` | `true` | Allow duels against each mob type |
| `villagerDuels.aiThinkDelaySeconds` | `1.0` | Delay before each AI action |
| `challengeExpirySeconds` | `30` | How long a player challenge stays open |
| `rebattleCooldownSeconds` | `60` | Cooldown before re-challenging the same mob, counted from duel start |
| `deck.limitCommon` / `Uncommon` / `Rare` / `Epic` | `4` / `3` / `2` / `1` | Max copies per rarity in a deckbox |

### Datapacks

All files live under `data/<namespace>/buddybattles/`.

**Named decks** - `decks/<name>.json`. The exact deck an AI plays, as card id -> copies.
Not subject to the deckbox rarity limits.

```json
{ "cards": { "buddycards:buddycard_base1": 4, "buddycards:buddycard_cave5": 2 } }
```

**Villager assignments** - `villager_decks/<profession>/<level>.json`, where `<profession>`
is the registry path (`farmer`, `librarian`, ...) and `<level>` is 1 (Novice) to 5 (Master).
Professions without a file use `villager_decks/_default/<level>.json`.

**Piglin / enderling assignments** - `piglin_decks/default.json`, `enderling_decks/default.json`.

Every assignment file has the same shape. Each duel picks one deck from `decks` and, on a
player win, drops one bundle from `loot` (everything in that bundle):

```json
{
  "decks": ["butcher_3", "default_3"],
  "loot": [
    { "minecraft:porkchop": 8, "minecraft:emerald": 1 },
    { "minecraft:beef": 8, "minecraft:emerald": 1 }
  ]
}
```

If no deck resolves, the duel doesn't start. `loot` is optional.

### Credits
Credit for the Buddycards mod goes to wildcard_gamer, with contributions from LegoMaster3650 and agnor99. Card names, stats,
abilities and several GUI assets are used with explicit permission from wildcard_gamer.
See `CREDITS.md` for further information.

### License
Licensed under GPLv3.

### AI Disclaimer
AI was used as a tool in creating this mod.

A real human looked over everything and made changes though.
