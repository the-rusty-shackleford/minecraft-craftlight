# Craftlight 0.1.0 local verification — September 20, 2026

Unreleased. Minecraft 1.21.1, NeoForge 21.1.248, Java 21.

`./gradlew test runGameTestServer jar --offline` passed **4 JUnit tests and 14
required real-server GameTests**. Tests use actual vanilla players with embedded
transport, registered recipes, menus, result slots and recipe-book placement.
No substitute crafting backend is used.

Partitions include learned/unknown/stale requests; 2×2 recipes and 3×3 recipes with
absent, present, removed or distant tables; exact cost/output counts; bounded stack
crafting; mixed wood ingredient tags; cake's three returned buckets; full and partly
full inventories; named ingredients; manual-grid bypasses; processing recipes;
Creative/Spectator restrictions; and cursor/grid returns on inventory-tab transitions.

A capacity regression first failed on the real server: vanilla quick-move inserted
part of a four-plank output and dropped the rest. The adapter now requires space for
the complete output before taking the result. Ingredients are returned unchanged on
refusal. All 14 server tests passed after this correction and the inventory-tab change.

The final `./gradlew jar runPhotoBooth --offline` passed on the **NVIDIA GeForce
RTX 4070**, with **Iris 1.8.14-beta.1, Sodium 0.8.13-beta.2 and Complementary Unbound
5.8.1**. Software-rendering overrides were unset; the actual OpenGL renderer was
checked. The host was checked for other clients before launch, sound was muted,
and the booth closed itself.

The client exercised actual E-inventory input, Craftlight/Inventory tab clicks,
search-field typing, recipe selection and server payloads. It verified station
refusal and success, exact chest/plank costs, returned buckets, unknown-recipe
filtering, live unlocks, return to the native inventory without a ghost cursor
stack, tab positioning after opening the vanilla recipe book, and GUI scales 2/3.
The earlier separate-key entry point was replaced by Rusty's inventory-tab design;
no Craftlight keybinding remains and shader toggling is unaffected.

Screenshots were visually inspected, including the tab joints, vanilla inventory
with recipe book open, ingredient previews and the compact layout. A cramped
compact status line was adjusted and the final client walkthrough repeated.

[Inventory tabs](craftlight/inventory-tabs.png) · [Browser](craftlight/browser.png) ·
[Table required](craftlight/table-required.png) · [Preview](craftlight/recipe-preview.png) ·
[Returned buckets](craftlight/returned-buckets.png) · [Unknown filtered](craftlight/unknown-filtered.png) ·
[Live unlock](craftlight/live-unlock.png) · [Recipe book](craftlight/recipe-book.png) ·
[Compact tabs](craftlight/compact-tabs.png)

The production jar excludes test fixtures and includes the domain classes and
expanded 0.1.0 metadata. No independent multiplayer or complete-pack compatibility
run is claimed. No public repository, release, pack update or server deployment
was performed for Craftlight.
