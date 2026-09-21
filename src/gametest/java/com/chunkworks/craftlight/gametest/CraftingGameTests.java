/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight.gametest;
import com.chunkworks.craftlight.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

/** Real-server partitions: known/unknown/stale; 2x2/3x3 with absent/present/lost
 * station; normal/batch/full inventory; tag alternatives; returned containers;
 * forged manual output; creative/spectator. Actual vanilla players/recipes/menus. */
@GameTestHolder("craftlight") @PrefixGameTestTemplate(false)
public final class CraftingGameTests {
    private static final BlockPos TABLE=new BlockPos(6,2,5);
    private static void player(GameTestHelper h,boolean table,Consumer<ServerPlayer> action) {
        if(table)h.setBlock(TABLE,Blocks.CRAFTING_TABLE);
        var p=PlacementPlayers.player(h,5,5);
        try { p.getInventory().clearContent();Crafting.open(p);action.accept(p); }
        finally { p.closeContainer();PlacementPlayers.remove(p); }
        h.succeed();
    }
    private static RecipeHolder<?> recipe(ServerPlayer p,String id) { return p.server.getRecipeManager().byKey(ResourceLocation.parse(id)).orElseThrow(); }
    private static void learn(ServerPlayer p,String id) { p.awardRecipes(List.of(recipe(p,id))); }
    private static void craft(ServerPlayer p,String id,boolean stack) { Crafting.craft(p,p.containerMenu.containerId,ResourceLocation.parse(id),stack); }
    static int count(ServerPlayer p,Item item) { return p.getInventory().items.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum(); }
    @GameTest(template="arena") public void inventoryTabTransitionReturnsCursorAndGrid(GameTestHelper h) {
        var p=PlacementPlayers.player(h,5,5);
        try {
            p.getInventory().clearContent();p.inventoryMenu.setCarried(new ItemStack(Items.OAK_LOG,2));
            p.inventoryMenu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS,3));Crafting.open(p);
            h.assertTrue(p.containerMenu instanceof CraftlightMenu&&p.inventoryMenu.getCarried().isEmpty()&&count(p,Items.OAK_LOG)==2&&count(p,Items.OAK_PLANKS)==3,"opening tab returns inventory cursor and crafting inputs");
            p.containerMenu.getSlot(1).set(new ItemStack(Items.COBBLESTONE,4));p.containerMenu.setCarried(new ItemStack(Items.DIRT,5));p.closeContainer();
            h.assertTrue(count(p,Items.COBBLESTONE)==4&&count(p,Items.DIRT)==5,"returning tab uses normal cursor and grid cleanup");
        } finally { p.closeContainer();PlacementPlayers.remove(p); }
        h.succeed();
    }
    @GameTest(template="arena") public void smallRecipeConsumesExactIngredients(GameTestHelper h) {
        player(h,false,p->{learn(p,"minecraft:oak_planks");p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,3));
            craft(p,"minecraft:oak_planks",false);
            h.assertTrue(count(p,Items.OAK_LOG)==2&&count(p,Items.OAK_PLANKS)==4,"one recipe costs one log and yields four planks");});
    }
    @GameTest(template="arena") public void stackActionStopsAtOneOutputStack(GameTestHelper h) {
        player(h,false,p->{learn(p,"minecraft:oak_planks");p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,32));
            craft(p,"minecraft:oak_planks",true);
            h.assertTrue(count(p,Items.OAK_LOG)==16&&count(p,Items.OAK_PLANKS)==64,"stack is sixteen real crafts");});
    }
    @GameTest(template="arena") public void unknownAndStaleRequestsGrantNothing(GameTestHelper h) {
        player(h,false,p->{p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,3));
            p.getRecipeBook().remove(recipe(p,"minecraft:oak_planks"));craft(p,"minecraft:oak_planks",false);
            learn(p,"minecraft:oak_planks");Crafting.craft(p,p.containerMenu.containerId+1,ResourceLocation.parse("minecraft:oak_planks"),false);
            h.assertTrue(count(p,Items.OAK_LOG)==3&&count(p,Items.OAK_PLANKS)==0,"unknown and stale are rejected");});
    }
    @GameTest(template="arena") public void largeRecipeNeedsWorkbench(GameTestHelper h) {
        player(h,false,p->{learn(p,"minecraft:chest");p.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,8));craft(p,"minecraft:chest",false);
            h.assertTrue(count(p,Items.OAK_PLANKS)==8&&count(p,Items.CHEST)==0,"large recipe rejected without table");});
    }
    @GameTest(template="arena") public void nearbyWorkbenchAndTagAlternativesWork(GameTestHelper h) {
        player(h,true,p->{learn(p,"minecraft:chest");p.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,4));p.getInventory().setItem(1,new ItemStack(Items.BIRCH_PLANKS,4));craft(p,"minecraft:chest",false);
            h.assertTrue(count(p,Items.OAK_PLANKS)==0&&count(p,Items.BIRCH_PLANKS)==0&&count(p,Items.CHEST)==1,"vanilla tag matching consumes mixed wood");});
    }
    @GameTest(template="arena") public void breakingWorkbenchInvalidatesLargeRecipe(GameTestHelper h) {
        player(h,true,p->{learn(p,"minecraft:chest");p.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,8));h.setBlock(TABLE,Blocks.AIR);craft(p,"minecraft:chest",false);
            h.assertTrue(count(p,Items.OAK_PLANKS)==8&&count(p,Items.CHEST)==0,"cached station is revalidated");});
    }
    @GameTest(template="arena") public void walkingOutOfReachInvalidatesLargeRecipe(GameTestHelper h) {
        player(h,true,p->{learn(p,"minecraft:chest");p.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,8));p.moveTo(p.getX()+12,p.getY(),p.getZ());craft(p,"minecraft:chest",false);
            h.assertTrue(count(p,Items.OAK_PLANKS)==8&&count(p,Items.CHEST)==0,"distant station grants nothing");});
    }
    @GameTest(template="arena") public void returnsVanillaContainers(GameTestHelper h) {
        player(h,true,p->{learn(p,"minecraft:cake");
            for(int i=0;i<3;i++)p.getInventory().setItem(i,new ItemStack(Items.MILK_BUCKET));
            p.getInventory().setItem(3,new ItemStack(Items.SUGAR,2));p.getInventory().setItem(4,new ItemStack(Items.WHEAT,3));p.getInventory().setItem(5,new ItemStack(Items.EGG));
            craft(p,"minecraft:cake",false);
            h.assertTrue(count(p,Items.CAKE)==1&&count(p,Items.BUCKET)==3&&count(p,Items.MILK_BUCKET)==0&&count(p,Items.WHEAT)==0,"actual cake result returns all three buckets");});
    }
    @GameTest(template="arena") public void fullInventoryKeepsAllIngredients(GameTestHelper h) {
        player(h,false,p->{learn(p,"minecraft:oak_planks");for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,2));craft(p,"minecraft:oak_planks",false);
            h.assertTrue(count(p,Items.OAK_LOG)==2&&count(p,Items.OAK_PLANKS)==0&&count(p,Items.STONE)==35*64,"full output space rolls back reservations");});
    }
    @GameTest(template="arena") public void partialOutputSpaceDoesNotSpillCrafts(GameTestHelper h) {
        player(h,false,p->{learn(p,"minecraft:oak_planks");for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            p.getInventory().setItem(0,new ItemStack(Items.OAK_LOG,2));p.getInventory().setItem(1,new ItemStack(Items.OAK_PLANKS,62));craft(p,"minecraft:oak_planks",false);
            h.assertTrue(count(p,Items.OAK_LOG)==2&&count(p,Items.OAK_PLANKS)==62,"convenience craft requires space for the full normal yield");});
    }
    @GameTest(template="arena") public void namedIngredientsAreNotSilentlyConsumed(GameTestHelper h) {
        player(h,false,p->{learn(p,"minecraft:oak_planks");var log=new ItemStack(Items.OAK_LOG,3);
            log.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keepsake"));p.getInventory().setItem(0,log);
            craft(p,"minecraft:oak_planks",false);h.assertTrue(count(p,Items.OAK_LOG)==3&&count(p,Items.OAK_PLANKS)==0&&p.getInventory().getItem(0).getHoverName().getString().equals("Keepsake"),"vanilla unused-item selection protects named materials");});
    }
    @GameTest(template="arena") public void manualGridCannotBypassUnlockOrWorkbench(GameTestHelper h) {
        player(h,false,p->{var m=(CraftlightMenu)p.containerMenu;p.getRecipeBook().remove(recipe(p,"minecraft:chest"));
            for(int i=1;i<=9;i++)if(i!=5)m.getSlot(i).set(new ItemStack(Items.OAK_PLANKS));
            m.clicked(0,0,ClickType.QUICK_MOVE,p);h.assertTrue(count(p,Items.CHEST)==0&&m.getSlot(0).getItem().isEmpty(),"unknown manual recipe has no output");
            learn(p,"minecraft:chest");m.slotsChanged(m.getSlot(1).container);m.clicked(0,0,ClickType.QUICK_MOVE,p);
            h.assertTrue(count(p,Items.CHEST)==0&&m.getSlot(0).getItem().isEmpty(),"learned large manual recipe still needs table");
            p.closeContainer();h.assertTrue(count(p,Items.OAK_PLANKS)==8,"closing returns all manually placed ingredients");});
    }
    @GameTest(template="arena") public void processingAndGameModesCannotGrantItems(GameTestHelper h) {
        player(h,false,p->{learn(p,"minecraft:iron_ingot_from_smelting_raw_iron");p.getInventory().setItem(0,new ItemStack(Items.RAW_IRON));
            craft(p,"minecraft:iron_ingot_from_smelting_raw_iron",false);h.assertTrue(count(p,Items.IRON_INGOT)==0&&count(p,Items.RAW_IRON)==1,"smelting still needs station/fuel/time");
            p.closeContainer();p.setGameMode(GameType.CREATIVE);Crafting.open(p);h.assertTrue(!(p.containerMenu instanceof CraftlightMenu),"creative uses native inventory");
            p.setGameMode(GameType.SPECTATOR);Crafting.open(p);h.assertTrue(!(p.containerMenu instanceof CraftlightMenu),"spectator cannot craft");});
    }
}
