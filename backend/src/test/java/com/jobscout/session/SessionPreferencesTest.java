package com.jobscout.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobscout.api.generated.model.SavedView;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionPreferencesTest {

    private final SessionPreferences prefs = new SessionPreferences();

    @Test
    void addViewAssignsSequentialIds() {
        SavedView first = prefs.addView(view("b"));
        SavedView second = prefs.addView(view("a"));

        assertThat(first.getId()).isEqualTo(1L);
        assertThat(second.getId()).isEqualTo(2L);
    }

    @Test
    void viewsAreOrderedByName() {
        prefs.addView(view("b"));
        prefs.addView(view("a"));

        assertThat(prefs.views()).extracting(SavedView::getName).containsExactly("a", "b");
    }

    @Test
    void viewLookupAndRemoval() {
        SavedView saved = prefs.addView(view("a"));

        assertThat(prefs.view(saved.getId())).contains(saved);
        assertThat(prefs.view(99L)).isEmpty();

        prefs.removeView(saved.getId());
        assertThat(prefs.view(saved.getId())).isEmpty();
    }

    @Test
    void seenIdsAreFilteredToRequestedListings() {
        prefs.markSeen(1L);
        prefs.markSeen(3L);

        assertThat(prefs.seenAmong(List.of(1L, 2L))).containsExactly(1L);
        assertThat(prefs.seenAmong(List.of())).isEmpty();
    }

    private static SavedView view(String name) {
        SavedView view = new SavedView();
        view.setName(name);
        return view;
    }
}
