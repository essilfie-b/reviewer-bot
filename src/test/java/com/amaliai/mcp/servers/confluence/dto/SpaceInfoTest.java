package com.amaliai.mcp.servers.confluence.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceInfoTest {

    @Test
    void recordProperties_areExposedViaAccessors() {
        SpaceInfo info = new SpaceInfo("123", "ENG", "Engineering");

        assertThat(info.id()).isEqualTo("123");
        assertThat(info.key()).isEqualTo("ENG");
        assertThat(info.name()).isEqualTo("Engineering");
    }

    @Test
    void recordsWithSameValues_areEqual() {
        SpaceInfo left = new SpaceInfo("123", "ENG", "Engineering");
        SpaceInfo right = new SpaceInfo("123", "ENG", "Engineering");

        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
        assertThat(left).hasToString("SpaceInfo[id=123, key=ENG, name=Engineering]");
    }
}

