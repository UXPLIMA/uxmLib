package com.uxplima.uxmlib.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;

import com.uxplima.uxmlib.text.Text;
import org.junit.jupiter.api.Test;

class BannerTest {

    @Test
    void rendersTitleWithNameAndVersion() {
        List<Component> lines = Banner.lines("uxmLib", "1.2.3");

        assertThat(lines).hasSize(3);
        assertThat(Text.plain(lines.get(1))).isEqualTo("uxmLib v1.2.3");
    }

    @Test
    void framesTheTitleBetweenTwoMatchingRules() {
        List<Component> lines = Banner.lines("any", "0");

        assertThat(Text.plain(lines.get(0))).isEqualTo(Text.plain(lines.get(2)));
        assertThat(Text.plain(lines.get(0))).isNotEmpty();
    }

    @Test
    void defaultBannerUsesTheLibraryVersion() {
        List<Component> lines = Banner.lines();

        assertThat(Text.plain(lines.get(1))).isEqualTo("uxmLib v" + UxmLib.VERSION);
    }

    @Test
    void printFeedsEveryLineToTheSinkInOrder() {
        List<String> printed = new ArrayList<>();

        Banner.print("uxmLib", "1.2.3", line -> printed.add(Text.plain(line)));

        assertThat(printed).hasSize(3);
        assertThat(printed.get(1)).isEqualTo("uxmLib v1.2.3");
    }
}
