/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.function.IntSupplier;

/** A vanilla-styled inventory tab anchored to its current container position.
 * AF: icon/action identifies one inventory page. RI: immutable identity and anchor;
 * coordinates follow the parent, including recipe-book shifts and GUI resizing. */
public final class InventoryTab extends AbstractButton {
    private final IntSupplier left,top;
    private final int column;
    private final ItemStack icon;
    private final Runnable action;
    private final ResourceLocation sprite;

    /** requires: nonnull anchors/icon/label/action, column 0 or 1; effects: creates
     * a tab, using vanilla Creative sprites; throws: invalid column. */
    public InventoryTab(IntSupplier left,IntSupplier top,int column,boolean selected,ItemStack icon,Component label,Runnable action) {
        super(left.getAsInt()+column*28,top.getAsInt()-28,26,32,label);
        if(column<0||column>1)throw new IllegalArgumentException("tab column");
        this.left=left;this.top=top;this.column=column;this.icon=icon.copy();this.action=action;
        sprite=ResourceLocation.withDefaultNamespace("container/creative_inventory/tab_top_"+(selected?"selected_":"unselected_")+(column+1));
        setTooltip(Tooltip.create(label));
    }
    private void anchor() { setX(left.getAsInt()+column*28);setY(top.getAsInt()-28); }
    @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial) {
        anchor();g.blitSprite(sprite,getX(),getY(),width,height);g.renderItem(icon,getX()+5,getY()+8);
        if(isFocused())g.renderOutline(getX()+1,getY()+1,width-2,height-5,0xFFFFFFFF);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) { anchor();return super.mouseClicked(mx,my,button); }
    @Override public void onPress() { action.run(); }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
