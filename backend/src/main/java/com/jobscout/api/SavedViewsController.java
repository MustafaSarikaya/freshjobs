package com.jobscout.api;

import com.jobscout.api.generated.api.SavedViewsApi;
import com.jobscout.api.generated.model.SavedView;
import com.jobscout.api.generated.model.SavedViewCreateRequest;
import com.jobscout.service.SavedViewService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class SavedViewsController implements SavedViewsApi {

    private final SavedViewService service;

    SavedViewsController(SavedViewService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<SavedView>> listSavedViews() {
        return ResponseEntity.ok(service.list());
    }

    @Override
    public ResponseEntity<SavedView> createSavedView(SavedViewCreateRequest req) {
        var created = service.create(req);
        return ResponseEntity
            .created(java.net.URI.create("/api/v1/views/" + created.getId()))
            .body(created);
    }

    @Override
    public ResponseEntity<SavedView> getSavedView(Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @Override
    public ResponseEntity<SavedView> updateSavedView(Long id, SavedViewCreateRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @Override
    public ResponseEntity<Void> deleteSavedView(Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
