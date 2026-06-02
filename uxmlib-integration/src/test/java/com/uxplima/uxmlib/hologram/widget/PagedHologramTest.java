package com.uxplima.uxmlib.hologram.widget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Drives the paged widget against a recording sink: opening a viewer shows page zero, turning the page hides
 * the page the viewer was on and shows the one it moved to (only for that viewer), and the lifecycle reset
 * sends the viewer back to page zero. The overlapping page holograms are stand-in indices, so no live entity
 * is needed.
 */
class PagedHologramTest {

    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);

    /** Records which (viewer, page) pairs were shown and hidden. */
    private static final class RecordingPages implements PagePresenter {
        private final List<String> shown = new ArrayList<>();
        private final List<String> hidden = new ArrayList<>();

        @Override
        public void show(int page, UUID viewer) {
            shown.add(page + ":" + viewer);
        }

        @Override
        public void hide(int page, UUID viewer) {
            hidden.add(page + ":" + viewer);
        }

        void clear() {
            shown.clear();
            hidden.clear();
        }
    }

    @Test
    void openingAViewerShowsTheFirstPage() {
        RecordingPages pages = new RecordingPages();
        PagedHologram widget = new PagedHologram(3, pages);

        widget.open(A);

        assertThat(pages.shown).containsExactly("0:" + A);
        assertThat(pages.hidden).isEmpty();
    }

    @Test
    void nextHidesTheCurrentPageAndShowsTheNextForThatViewerOnly() {
        RecordingPages pages = new RecordingPages();
        PagedHologram widget = new PagedHologram(3, pages);
        widget.open(A);
        widget.open(B);
        pages.clear();

        widget.next(A);

        assertThat(pages.hidden).containsExactly("0:" + A);
        assertThat(pages.shown).containsExactly("1:" + A); // B untouched
    }

    @Test
    void prevWrapsAndReRendersOnlyTheMover() {
        RecordingPages pages = new RecordingPages();
        PagedHologram widget = new PagedHologram(3, pages);
        widget.open(A);
        pages.clear();

        widget.prev(A); // 0 -> last page

        assertThat(pages.hidden).containsExactly("0:" + A);
        assertThat(pages.shown).containsExactly("2:" + A);
    }

    @Test
    void noFlickerWhenThePageDoesNotChange() {
        RecordingPages pages = new RecordingPages();
        PagedHologram widget = new PagedHologram(1, pages); // single page: next is a no-op
        widget.open(A);
        pages.clear();

        widget.next(A);

        assertThat(pages.shown).isEmpty();
        assertThat(pages.hidden).isEmpty();
    }

    @Test
    void quitHidesThePlayersCurrentPageAndResetsItToZero() {
        RecordingPages pages = new RecordingPages();
        PagedHologram widget = new PagedHologram(3, pages);
        widget.open(A);
        widget.next(A); // A on page 1
        pages.clear();

        widget.onQuit(A);
        assertThat(pages.hidden).containsExactly("1:" + A);

        pages.clear();
        widget.open(A); // re-opening starts at page zero again
        assertThat(pages.shown).containsExactly("0:" + A);
    }

    @Test
    void pageCountReflectsTheConfiguredPages() {
        assertThat(new PagedHologram(4, new RecordingPages()).pageCount()).isEqualTo(4);
    }

    @Test
    void rejectsNonPositivePageCount() {
        assertThatThrownBy(() -> new PagedHologram(0, new RecordingPages()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
