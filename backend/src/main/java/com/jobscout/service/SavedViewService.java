package com.jobscout.service;

import com.jobscout.api.generated.model.SavedView;
import com.jobscout.api.generated.model.SavedViewCreateRequest;
import com.jobscout.session.SessionPreferences;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SavedViewService {

    private final SessionPreferences prefs;

    public SavedViewService(SessionPreferences prefs) {
        this.prefs = prefs;
    }

    public List<SavedView> list() {
        return prefs.views();
    }

    public SavedView create(SavedViewCreateRequest req) {
        validateRequest(req);
        SavedView view = new SavedView();
        view.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        applyRequest(view, req);
        return prefs.addView(view);
    }

    public SavedView get(Long id) {
        return prefs.view(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Saved view not found"));
    }

    public SavedView update(Long id, SavedViewCreateRequest req) {
        validateRequest(req);
        SavedView view = get(id);
        applyRequest(view, req);
        return view;
    }

    public void delete(Long id) {
        prefs.removeView(get(id).getId());
    }

    private static void applyRequest(SavedView view, SavedViewCreateRequest req) {
        view.setName(req.getName());
        view.setFilterExpression(req.getFilterExpression());
        view.setColumns(new ArrayList<>(req.getColumns()));
        view.setSort(req.getSort() == null ? new ArrayList<>() : new ArrayList<>(req.getSort()));
        view.setConfig(req.getConfig() == null ? new HashMap<>() : new HashMap<>(req.getConfig()));
        view.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static void validateRequest(SavedViewCreateRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY, "name is required");
        }
        if (req.getColumns() == null || req.getColumns().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY, "columns must be a non-empty list");
        }
    }
}
