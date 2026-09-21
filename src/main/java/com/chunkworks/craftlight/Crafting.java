/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight;

import com.chunkworks.craftlight.domain.Catalogue;
import net.minecraft.network.chat.Component;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.crafting.*;

/** Server adapter. Every craft uses vanilla's placement, result and remainder
 * paths, bounded to one output stack per request. No client-supplied item stacks. */
public final class Crafting {
    private Crafting() {}
    /** requires: server thread; effects: opens survival crafting catalog; throws: none. */
    public static void open(ServerPlayer p) {
        if(!p.isAlive()||p.isSpectator()||p.isCreative()||p.containerMenu!=p.inventoryMenu)return;
        // Switching from the E inventory must return its cursor and 2x2 inputs,
        // just as closing that inventory does, before the new container opens.
        p.inventoryMenu.removed(p);
        p.openMenu(new SimpleMenuProvider((id,inv,player)->new CraftlightMenu(id,inv),Component.translatable("container.craftlight")));
    }
    /** requires: server thread; effects: validates current menu/unlock/station and
     * crafts from actual inventory, returning residual grid inputs; throws: none. */
    public static void craft(ServerPlayer p,int menuId,ResourceLocation id,boolean stack) {
        if(!(p.containerMenu instanceof CraftlightMenu menu)||menu.containerId!=menuId||!menu.stillValid(p)
                ||p.isCreative()||!menu.acceptRequest())return;
        var found=p.server.getRecipeManager().byKey(id).orElse(null);
        if(found==null||!p.getRecipeBook().contains(found)) { menu.status(CraftlightMenu.UNKNOWN);return; }
        if(!(found.value() instanceof CraftingRecipe recipe)) { menu.status(CraftlightMenu.PROCESSING);return; }
        var holder=new RecipeHolder<>(found.id(),recipe);
        if(!menu.permits(holder)) { menu.status(CraftlightMenu.STATION);return; }
        var output=recipe.getResultItem(p.registryAccess());
        if(output.isEmpty()) { menu.status(CraftlightMenu.PROCESSING);return; }
        int limit=Catalogue.batches(output.getCount(),output.getMaxStackSize(),stack);
        var placer=new Placement(menu);
        int crafted=0;
        for(int i=0;i<limit;i++) {
            if(!menu.permits(holder))break;
            menu.beginPlacingRecipe();
            placer.recipeClicked(p,holder,false);
            menu.finishPlacingRecipe(holder);
            if(menu.getSlot(0).getItem().isEmpty()||!menu.recipeMatches(holder)) { menu.status(CraftlightMenu.MATERIALS);break; }
            if(!fits(p,menu.getSlot(0).getItem())) { menu.status(CraftlightMenu.FULL);break; }
            var taken=menu.quickMoveStack(p,0);
            if(taken.isEmpty()) { menu.status(CraftlightMenu.FULL);break; }
            crafted++;
            placer.returnInputs();
        }
        // Vanilla returns any unconsumed ingredients, with normal component-aware merging.
        placer.returnInputs();
        if(crafted>0)menu.status(CraftlightMenu.CRAFTED);
        menu.broadcastChanges();p.inventoryMenu.broadcastChanges();
    }
    private static boolean fits(ServerPlayer player,net.minecraft.world.item.ItemStack output) {
        int remaining=output.getCount();
        int limit=Math.min(output.getMaxStackSize(),player.getInventory().getMaxStackSize());
        for(var slot:player.getInventory().items) {
            if(slot.isEmpty())remaining-=limit;
            else if(net.minecraft.world.item.ItemStack.isSameItemSameComponents(slot,output))remaining-=Math.max(0,limit-slot.getCount());
            if(remaining<=0)return true;
        }
        return false;
    }
    private static final class Placement extends ServerPlaceRecipe<CraftingInput,CraftingRecipe> {
        Placement(CraftlightMenu menu) { super(menu); }
        void returnInputs() { if(inventory!=null)clearGrid(); }
    }
}
