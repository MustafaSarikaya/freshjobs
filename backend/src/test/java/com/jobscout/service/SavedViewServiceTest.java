package com.jobscout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobscout.api.generated.model.ColumnDef;
import com.jobscout.api.generated.model.SavedView;
import com.jobscout.api.generated.model.SavedViewCreateRequest;
import com.jobscout.session.SessionPreferences;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SavedViewServiceTest {

    private final SessionPreferences prefs = new SessionPreferences();
    private final SavedViewService service = new SavedViewService(prefs);

    @Test
    void createStoresViewInSession() {
        SavedView created = service.create(request("Remote"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getConfig()).containsEntry("pageSize", 25);
        assertThat(service.list()).containsExactly(created);
    }

    @Test
    void updateReplacesFieldsAndKeepsCreatedAt() {
        SavedView created = service.create(request("Remote"));
        var createdAt = created.getCreatedAt();

        SavedView updated = service.update(created.getId(), request("Hybrid"));

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo("Hybrid");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void createDefaultsMissingSortAndConfig() {
        SavedViewCreateRequest req = request("Remote");
        req.setSort(null);
        req.setConfig(null);

        SavedView created = service.create(req);

        assertThat(created.getSort()).isEmpty();
        assertThat(created.getConfig()).isEmpty();
    }

    @Test
    void deleteRemovesView() {
        SavedView created = service.create(request("Remote"));

        service.delete(created.getId());

        assertThat(service.list()).isEmpty();
    }

    @Test
    void getUnknownViewIsNotFound() {
        assertThatThrownBy(() -> service.get(42L))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rejectsBlankNameAndEmptyColumns() {
        SavedViewCreateRequest blank = request(" ");
        SavedViewCreateRequest noColumns = request("Remote");
        noColumns.setColumns(List.of());

        assertThatThrownBy(() -> service.create(blank))
            .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.create(noColumns))
            .isInstanceOf(ResponseStatusException.class);
    }

    private static SavedViewCreateRequest request(String name) {
        SavedViewCreateRequest req = new SavedViewCreateRequest();
        req.setName(name);
        req.setColumns(List.of(new ColumnDef("title", true)));
        req.setConfig(Map.of("pageSize", 25));
        return req;
    }
}
