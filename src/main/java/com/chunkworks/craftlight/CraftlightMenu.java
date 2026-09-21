/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Real vanilla crafting grid with server-owned recipe/workbench restrictions.
 * AF: the menu owns transient ingredients and their vanilla result; the catalog
 * chooses recipes rather than granting items. RI: only learned recipes can be
 * taken; large recipes additionally require the cached workbench in reach.
 * Closing uses vanilla's ingredient-return path. */
public final class CraftlightMenu extends CraftingMenu {
    public static final int READY=0,UNKNOWN=1,STATION=2,MATERIALS=3,FULL=4,CRAFTED=5,PROCESSING=6;
    private final Player owner;
    private final Level openedLevel;
    private final BlockPos workbench;
    private final DataSlot table=DataSlot.standalone(),status=DataSlot.standalone();
    private long lastCraftTick=Long.MIN_VALUE;

    /** requires: player inventory; effects: opens a real 3x3 container, constraining
     * larger recipes by nearby workbench; throws: none. */
    public CraftlightMenu(int id,Inventory inventory) {
        super(id,inventory,inventory.player.level().isClientSide ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(inventory.player.level(),inventory.player.blockPosition()));
        owner=inventory.player;openedLevel=owner.level();
        workbench=openedLevel.isClientSide?null:findWorkbench(owner);
        addDataSlot(table);addDataSlot(status);
        if(!openedLevel.isClientSide)table.set(workbenchAvailable()?1:0);
    }
    private static BlockPos findWorkbench(Player p) {
        BlockPos best=null;double distance=Double.MAX_VALUE;
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-4,-4,-4),p.blockPosition().offset(4,4,4))) {
            if(!p.level().hasChunkAt(pos)||!p.level().getBlockState(pos).is(Blocks.CRAFTING_TABLE))continue;
            double d=p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
            if(d<distance&&p.canInteractWithBlock(pos,0)) { best=pos.immutable();distance=d; }
        }
        return best;
    }
    private boolean workbenchAvailable() {
        return workbench!=null&&owner.level()==openedLevel&&openedLevel.hasChunkAt(workbench)
                &&openedLevel.getBlockState(workbench).is(Blocks.CRAFTING_TABLE)&&owner.canInteractWithBlock(workbench,0);
    }
    /** requires: none; effects: returns synchronized workbench availability; throws: none. */
    public boolean hasWorkbench() { return table.get()!=0; }
    /** requires: none; effects: returns last operation status; throws: none. */
    public int status() { return status.get(); }
    /** requires: server thread; effects: sets visible status; throws: none. */
    public void status(int value) { status.set(value); }
    /** requires: server thread; effects: permits at most one craft request per tick; throws: none. */
    public boolean acceptRequest() {
        long now=openedLevel.getGameTime();if(now==lastCraftTick)return false;lastCraftTick=now;return true;
    }
    @Override public MenuType<?> getType() { return Craftlight.MENU.get(); }
    @Override public boolean stillValid(Player player) { return player==owner&&player.isAlive()&&!player.isSpectator()&&!player.isCreative()&&player.level()==openedLevel; }
    /** requires: resolved recipe; effects: checks current unlock and station; throws: none. */
    public boolean permits(RecipeHolder<?> recipe) {
        return recipe!=null&&owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer&&serverPlayer.getRecipeBook().contains(recipe)&&recipe.value() instanceof CraftingRecipe craft
                &&(craft.canCraftInDimensions(2,2)||workbenchAvailable());
    }
    private boolean permitsResult() {
        return permits(((ResultContainer)getSlot(0).container).getRecipeUsed());
    }
    private void guardResult() {
        if(owner!=null&&!owner.level().isClientSide&&!getSlot(0).getItem().isEmpty()&&!permitsResult())getSlot(0).set(ItemStack.EMPTY);
    }
    @Override public void slotsChanged(Container container) { super.slotsChanged(container);guardResult(); }
    @Override public void finishPlacingRecipe(RecipeHolder<CraftingRecipe> recipe) { super.finishPlacingRecipe(recipe);guardResult(); }
    @Override public void broadcastChanges() {
        if(owner!=null&&!owner.level().isClientSide) { table.set(workbenchAvailable()?1:0);guardResult(); }
        super.broadcastChanges();
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(slot==0&&!player.level().isClientSide&&!permitsResult())return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int slot) {
        return slot==0&&!player.level().isClientSide&&!permitsResult()?ItemStack.EMPTY:super.quickMoveStack(player,slot);
    }
}
