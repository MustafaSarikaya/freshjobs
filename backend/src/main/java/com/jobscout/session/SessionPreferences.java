package com.jobscout.session;

import com.jobscout.api.generated.model.SavedView;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Per-visitor preferences (saved views, seen listings) held in the HTTP
 * session only. Nothing here is persisted; it disappears with the session.
 */
@Component
@SessionScope
public class SessionPreferences {

    private final Map<Long, SavedView> views = new LinkedHashMap<>();
    private final Set<Long> seenListingIds = new HashSet<>();
    private long nextViewId = 1;

    public synchronized List<SavedView> views() {
        return views.values().stream()
            .sorted(Comparator.comparing(SavedView::getName))
            .toList();
    }

    public synchronized Optional<SavedView> view(Long id) {
        return Optional.ofNullable(views.get(id));
    }

    public synchronized SavedView addView(SavedView view) {
        view.setId(nextViewId++);
        views.put(view.getId(), view);
        return view;
    }

    public synchronized void removeView(Long id) {
        views.remove(id);
    }

    public synchronized void markSeen(Long listingId) {
        seenListingIds.add(listingId);
    }

    public synchronized Set<Long> seenAmong(Collection<Long> listingIds) {
        return listingIds.stream()
            .filter(seenListingIds::contains)
            .collect(Collectors.toSet());
    }
}
