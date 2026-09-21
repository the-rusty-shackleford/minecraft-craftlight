/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight.domain;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Partitions: learned/unknown; empty/mixed-case/multi-token/missing query;
 * immutable/stable/duplicate identities; single/stack/non-divisible/invalid yields. */
final class CatalogueTest {
    private final Catalogue.Entry oak=new Catalogue.Entry("minecraft:oak_planks","Oak Planks","crafting wood");
    private final Catalogue.Entry birch=new Catalogue.Entry("minecraft:birch_planks","Birch Planks","crafting wood");
    @Test void knowsOnlyLearnedRecipesInStableOrder() {
        var entries=new ArrayList<>(List.of(oak,birch));
        var known=new HashSet<>(Set.of(oak.id(),birch.id()));var c=new Catalogue(entries,known);
        entries.clear();known.clear();assertEquals(List.of(birch,oak),c.search(""));
        assertEquals(List.of(oak),new Catalogue(List.of(oak,birch),Set.of(oak.id())).search(""));
        assertThrows(UnsupportedOperationException.class,()->c.search("").clear());
    }
    @Test void matchesAllWordsAcrossNameIdentityAndType() {
        var c=new Catalogue(List.of(oak,birch),Set.of(oak.id(),birch.id()));
        assertEquals(List.of(oak),c.search("  OAK  MINECRAFT wood "));
        assertEquals(List.of(),c.search("birch stone"));assertEquals(2,c.search("planks").size());
    }
    @Test void rejectsAmbiguousIdentity() {
        assertThrows(IllegalArgumentException.class,()->new Catalogue(List.of(oak,oak),Set.of()));
        assertThrows(IllegalArgumentException.class,()->new Catalogue.Entry("","x",""));
    }
    @Test void boundsCraftingToNormalRecipeBatches() {
        assertEquals(1,Catalogue.batches(4,64,false));assertEquals(16,Catalogue.batches(4,64,true));
        assertEquals(10,Catalogue.batches(6,64,true));assertEquals(1,Catalogue.batches(1,1,true));
        assertEquals(1,Catalogue.batches(70,64,true));assertEquals(64,Catalogue.batches(1,999,true));
        assertThrows(IllegalArgumentException.class,()->Catalogue.batches(0,64,true));
    }
}
