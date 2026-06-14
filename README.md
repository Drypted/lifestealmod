![banner](https://cdn.modrinth.com/data/cached_images/25cea83c8ffef70f24b327049dde254f2ebfc945_0.webp)
This Lifesteal mod is identical by the Lifesteal SMP. In this mod, if you kill a player, you steal one of their hearts, and if you die, you lose one. Just like the Lifesteal SMP, if you lose all of your hearts, you can get revived.

**Supports**
- 26.1.2 FABRIC

**Links**
- Modrinth - [https://modrinth.com/mod/lifesteal-smp]
- Curseforge - [https://www.curseforge.com/minecraft/mc-mods/lifestealsmp]

## Features
- When a player kills another player, they steal one heart from the other player.
- Players lose a heart every time they die.
- If a player loses all their hearts, they are banned or put spectator mode (configurable).
- Players can use a revive beacon to revive other players.
- Ability to cap any enchantment to any level (e.g: protection to 3, sharpness to 4, etc)
- No dragon egg in enderchest (configurable).
- Totem of undying is disabled (configurable)
- Enderpearl is disabled (configurable).
- End crystals deal no damage to the player nor the environment (configurable).
- Respawn anchors works in the nether but not in the overworld and the end dimensions (configurable).
- Ability to limit maces on the server (configurable).
- Players can withdraw hearts using the `/lifesteal withdraw [number]` command.
- Operators/admins can use `/lifesteal item` command to get heart and revive items.
- Config accessible via `/lifesteal settings`
- Players can use `/lifesteal recipe` to view/edit the recipes of heart and the revive beacon.

### Next update:
- A way to disable specific items (e.g: netherite items).
- Riptide trident cooldown.

## Commands
| Command                               | Description                                                                              |
| ------------------------------------- | ---------------------------------------------------------------------------------------- |
| `/lifesteal enchantment_cap`          | Opens a GUI to configure enchantment caps (e.g., Protection IV, Sharpness IV, and more). |
| `/lifesteal withdraw [number]`        | Withdraws the specified number of hearts from your total hearts.                         |
| `/lifesteal recipe`                   | Opens a GUI to edit or view Heart and Revive Beacon recipes.                             |
| `/lifesteal give`                     | Adds the specified number of hearts to a player's health bar (operator only).            |
| `/lifesteal take`                     | Removes the specified number of hearts from a player's health bar (operator only).       |
| `/lifesteal item heart/revive_beacon` | Gives a Heart or Revive Beacon item to the player (operator only).                       |
| `/lifesteal revive`                   | Revive                                                                                   |

![showcases commands](https://cdn.modrinth.com/data/cached_images/5f3ecba0fb867d00abbd2c7fb5fb2af7981ccdf4_0.webp)

## Config
You can edit the config by using /lifesteal settings.

**How is it saved?**
- Config is saved inside every world, which ensures that each world/server has their unique config. You can manually edit the config as well, it is located in the world folder of a minecraft server/singleplayer world.

**What is configurable?**
- Almost everything in this mod is configurable, ability to set maximum hearts, if a player is above X hearts, they can't craft hearts, limiting mace crafting on the server,and much more.
