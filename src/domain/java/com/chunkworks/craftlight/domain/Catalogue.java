/* Copyright (C) 2026 Rusty Shackleford and nfx. SPDX-License-Identifier: AGPL-3.0-or-later */
package com.chunkworks.craftlight.domain;

import java.util.*;

/** Immutable learned-recipe search index. AF: entries are the known recipes in
 * display order. RI: IDs unique and nonblank; unknown entries absent; lists and
 * values immutable. Construction/filtering happens on UI changes, never world ticks. */
public final class Catalogue {
    /** AF: searchable recipe identity and translated display text; RI: nonblank ID/label. */
    public record Entry(String id, String label, String keywords) {
        /** requires: nonnull text; effects: creates entry; throws: invalid identity. */
        public Entry {
            Objects.requireNonNull(id);Objects.requireNonNull(label);Objects.requireNonNull(keywords);
            if(id.isBlank()||label.isBlank())throw new IllegalArgumentException("blank recipe identity");
        }
    }
    private final List<Entry> entries;
    /** requires: nonnull entries/unlocks; effects: indexes learned recipes with stable
     * name/ID ordering; throws: duplicate IDs. */
    public Catalogue(Collection<Entry> recipes, Set<String> known) {
        Objects.requireNonNull(recipes);Objects.requireNonNull(known);
        var ids=new HashSet<String>();var ordered=new ArrayList<Entry>();
        for(var recipe:recipes) {
            if(!ids.add(recipe.id()))throw new IllegalArgumentException("duplicate recipe "+recipe.id());
            if(known.contains(recipe.id()))ordered.add(recipe);
        }
        ordered.sort(Comparator.comparing(Entry::label,String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::id));
        entries=List.copyOf(ordered);
    }
    /** requires: nonnull query; effects: returns known recipes matching every
     * case-insensitive word across label, ID and keywords; throws: null query. */
    public List<Entry> search(String query) {
        String trimmed=Objects.requireNonNull(query).strip().toLowerCase(Locale.ROOT);
        if(trimmed.isEmpty())return entries;
        var words=trimmed.split("\\s+");
        return entries.stream().filter(entry->{
            String text=(entry.label()+" "+entry.id()+" "+entry.keywords()).toLowerCase(Locale.ROOT);
            for(var word:words)if(!text.contains(word))return false;
            return true;
        }).toList();
    }
    /** requires: positive recipe yield/stack limit; effects: bounds one convenience
     * request to one recipe or at most one output stack; throws: invalid count. */
    public static int batches(int yield,int stackLimit,boolean stack) {
        if(yield<1||stackLimit<1)throw new IllegalArgumentException("positive output counts required");
        return stack?Math.max(1,Math.min(64,stackLimit/yield)):1;
    }
}
