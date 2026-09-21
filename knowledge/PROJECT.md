# Craftlight

Version **0.1.0** is published and deployed in pack **1.54.0**.
See [release and deployment verification](../devtools/verification/release-0.1.0.md).

Version 0.1.0. Minecraft 1.21.1, NeoForge 21.1.248, Java 21.

Rusty requested a lightweight Creative-style Survival crafting browser restricted
to learned recipes. They chose normal ingredient consumption and a nearby table
for 3×3 recipes, then directed that the browser live in a tab on the E inventory.
D-0001 records crafting rules; D-0002 records the inventory-tab entry point.

The domain catalogue is immutable and Minecraft-free. The server adapter reuses
vanilla recipe placement/result/remainder handling, guards manual output retrieval,
checks complete output capacity, and validates the cached nearby table. The client
uses NeoForge screen events to add Creative-style tabs without replacing the native
inventory. Its catalogue responds to recipe-book updates, preserving the current
page. No extra keybinding, dependency, world tick scan or persistent data is added.

Local verification covers 4 JUnit tests, 14 real-server GameTests, and the actual
E-inventory/tab/browser route under NVIDIA/Iris/Complementary, including a compact
layout. See [evidence](../devtools/verification/craftlight.md). Full-pack server startup is verified in the release record. Independent
multiplayer and full-pack client playtesting remain unverified.

Rusty authorized publication, pack assembly and deployment on September 20, 2026.

## Release authorization — September 20, 2026

Rusty requested: "release it all, 2 minute server warning". Version 0.1.0 is
authorized for public source/jar publication and pack 1.54.0 deployment. The clean
build passed 4 JUnit tests, 14 real-server GameTests and the complete
GPU shader booth. See [release verification](../devtools/verification/release-0.1.0.md).
Earlier release holds are superseded for this version.
