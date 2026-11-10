package com.carpentersblocks.core;

import com.carpentersblocks.core.attribute.AttributeKey;
import com.carpentersblocks.core.attribute.AttributeLocation;
import com.carpentersblocks.core.attribute.AttributeType;
import com.carpentersblocks.core.attribute.AttributeView;
import com.carpentersblocks.core.interaction.AttributeRules;
import com.carpentersblocks.core.interaction.HeldItemKind;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AttributeRulesTest {
    private static AttributeKey key(AttributeType type, AttributeLocation location) {
        return AttributeKey.of(type, location);
    }

    private static AttributeView view(AttributeKey... keys) {
        Set<AttributeKey> set = new HashSet<>(Arrays.asList(keys));
        return set::contains;
    }

    @Test
    void firstCoverGoesToHostThenSneakingToClickedSide() {
        AttributeView empty = view();
        assertEquals(key(AttributeType.COVER, AttributeLocation.HOST),
            AttributeRules.addTarget(HeldItemKind.COVER, false, Side.NORTH, empty, l -> true));
        // Sneaking on a bare block is left to vanilla (place the held block next to it).
        assertNull(AttributeRules.addTarget(HeldItemKind.COVER, true, Side.NORTH, empty, l -> true));

        AttributeView hosted = view(key(AttributeType.COVER, AttributeLocation.HOST));
        // A plain click on a covered block must not swallow the block as a side cover.
        assertNull(AttributeRules.addTarget(HeldItemKind.COVER, false, Side.NORTH, hosted, l -> true));
        assertEquals(key(AttributeType.COVER, AttributeLocation.NORTH),
            AttributeRules.addTarget(HeldItemKind.COVER, true, Side.NORTH, hosted, l -> true));
    }

    @Test
    void coverRespectsBlockRestrictions() {
        AttributeView hosted = view(key(AttributeType.COVER, AttributeLocation.HOST));
        assertNull(AttributeRules.addTarget(HeldItemKind.COVER, true, Side.UP, hosted, AttributeLocation::isHost));
    }

    @Test
    void dyeNeedsSneakAndTargetsSideCoverWhenPresent() {
        AttributeView sided = view(key(AttributeType.COVER, AttributeLocation.HOST), key(AttributeType.COVER, AttributeLocation.EAST));
        assertNull(AttributeRules.addTarget(HeldItemKind.DYE, false, Side.EAST, sided, l -> true));
        assertEquals(key(AttributeType.DYE, AttributeLocation.EAST),
            AttributeRules.addTarget(HeldItemKind.DYE, true, Side.EAST, sided, l -> true));
        assertEquals(key(AttributeType.DYE, AttributeLocation.HOST),
            AttributeRules.addTarget(HeldItemKind.DYE, true, Side.WEST, sided, l -> true));
    }

    @Test
    void slotsAreNotOverwritten() {
        AttributeView dyed = view(key(AttributeType.DYE, AttributeLocation.HOST));
        assertNull(AttributeRules.addTarget(HeldItemKind.DYE, true, Side.UP, dyed, l -> true));
        AttributeView lit = view(key(AttributeType.ILLUMINATOR, AttributeLocation.HOST));
        assertNull(AttributeRules.addTarget(HeldItemKind.ILLUMINATOR, true, Side.UP, lit, l -> true));
    }

    @Test
    void popOrderMatches1710() {
        AttributeKey cover = key(AttributeType.COVER, AttributeLocation.HOST);
        AttributeKey dye = key(AttributeType.DYE, AttributeLocation.HOST);
        AttributeKey overlay = key(AttributeType.OVERLAY, AttributeLocation.HOST);
        AttributeKey design = key(AttributeType.DESIGN, AttributeLocation.HOST);
        AttributeKey light = key(AttributeType.ILLUMINATOR, AttributeLocation.HOST);

        assertEquals(Collections.singletonList(light), AttributeRules.popTargets(view(cover, dye, overlay, light), Side.UP));
        assertEquals(Collections.singletonList(overlay), AttributeRules.popTargets(view(cover, dye, overlay), Side.UP));
        assertEquals(Collections.singletonList(dye), AttributeRules.popTargets(view(cover, dye), Side.UP));
        assertEquals(Arrays.asList(cover, design), AttributeRules.popTargets(view(cover, design), Side.UP));
        assertTrue(AttributeRules.popTargets(view(), Side.UP).isEmpty());
    }

    @Test
    void popPrefersClickedSideCover() {
        AttributeKey host = key(AttributeType.COVER, AttributeLocation.HOST);
        AttributeKey south = key(AttributeType.COVER, AttributeLocation.SOUTH);
        assertEquals(Collections.singletonList(south), AttributeRules.popTargets(view(host, south), Side.SOUTH));
        assertEquals(Collections.singletonList(host), AttributeRules.popTargets(view(host, south), Side.NORTH));
    }
}
