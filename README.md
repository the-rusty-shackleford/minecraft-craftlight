# Craftlight

A lightweight Survival crafting browser for **Minecraft 1.21.1 / NeoForge 21.1.248**.
Open your inventory with **E**, then select the **crafting-table tab** above it.
The chest tab returns to your normal inventory. There is no extra keybinding.

Browse a Creative-style grid containing only recipes you have learned. Search by
item name, recipe ID or recipe type. New unlocks appear while the page is open.

- **Click** a recipe to craft once.
- **Shift-click** to craft up to one output stack, using the ingredients available.
- **Right-click** to preview its ingredients and result.
- Scroll or use the arrow buttons to change pages; hover for recipe details.

Crafting uses the ordinary ingredients from your inventory and keeps vanilla recipe
outputs, ingredient alternatives and returned containers. Automatic placement
protects named or otherwise used ingredients as the vanilla recipe book does.
The entire output must fit before a craft proceeds. Returned buckets and other
containers follow vanilla's inventory/drop behavior.

Recipes that fit the inventory's **2×2** grid work anywhere. Larger recipes require
a nearby **crafting table in reach**. The page finds a table when opened, then checks
that it is still present and reachable. If the nearby table changes, switch to the
inventory tab and back. Breaking the table or moving away prevents further large
crafts. The ordinary crafting grid remains usable, subject to the same learned-recipe
and workbench rules; switching tabs returns its inputs and anything on the cursor.

Smelting, stonecutting and other processing recipes can be inspected, but use their
normal station, fuel and time. Recipes without a fixed display result remain in
normal crafting. Craftlight does not recursively craft ingredients or pull from
nearby storage. Creative keeps its normal inventory.

## Installation and status

Install Craftlight on **both client and server**, with Java 21 and NeoForge. No
recipe viewer, shader mod or other library is required.

**0.1.0** is the first release. Download it from
[GitHub Releases](https://github.com/the-rusty-shackleford/minecraft-craftlight/releases/tag/v0.1.0).
Deployed in shared pack **1.54.0**; use **Update Pack** in Prism to install it.

[Inventory tabs](devtools/verification/craftlight/inventory-tabs.png) ·
[Recipe browser](devtools/verification/craftlight/browser.png) ·
[Verification](devtools/verification/craftlight.md)

## Development

The JDK-only `domain` source set contains the immutable learned-recipe search index
and request bounds. `main` adapts Minecraft's actual crafting menu, recipe placement,
result slots and recipe book. Only recipe IDs and bounded actions cross the network;
the server validates the active menu, unlocks, workbench and ingredients. The client
rebuilds its catalogue on recipe updates and renders a bounded page. Table discovery
runs on page opening; there are no recurring world scans or persistent world data.

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test runGameTestServer jar --offline
```

GameTests use actual server players, menus and registered recipes. Their fixture
world is reset under this repo's ignored `run/world`; other worlds are untouched.
The optional real-client booth copies that fixture into `run/booth/saves/booth`,
executes inventory/tab/crafting interactions, captures screens, mutes sound and exits.
`./gradlew check` includes both the server and client gates; use `-PskipBooth` only
when no rendering session is available, and report the missing visual gate.

Before running `runPhotoBooth`, verify on the host that no other Minecraft client
is rendering. Use the actual desktop display, unset software-rendering overrides,
and verify the GPU renderer in the log. Iris/Sodium and the shader pack used in the
verification record can be placed in the ignored `run/booth` instance for shader
checks. They are not bundled or required for ordinary play.

Design decisions are in the [project store](knowledge/PROJECT.md).

Copyright Rusty Shackleford and nfx. AGPL-3.0-or-later.

Clean release results: [version 0.1.0](devtools/verification/release-0.1.0.md).
