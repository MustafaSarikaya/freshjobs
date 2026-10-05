package com.jobscout.api;

import com.jobscout.api.generated.api.AdminApi;
import com.jobscout.api.generated.model.LocationTag;
import com.jobscout.api.generated.model.LocationTagUpdate;
import com.jobscout.api.generated.model.Source;
import com.jobscout.api.generated.model.SourceInput;
import com.jobscout.domain.Platform;
import com.jobscout.mapper.LocationTagMapper;
import com.jobscout.mapper.SourceMapper;
import com.jobscout.repository.LocationTagRepository;
import com.jobscout.repository.SourceRepository;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Admin-only management of sources and location tags (secured in SecurityConfig). */
@RestController
@RequestMapping("/api/v1")
class AdminController implements AdminApi {

    private final SourceRepository sources;
    private final LocationTagRepository locations;

    AdminController(SourceRepository sources, LocationTagRepository locations) {
        this.sources = sources;
        this.locations = locations;
    }

    @Override
    public ResponseEntity<Source> createSource(SourceInput input) {
        com.jobscout.domain.Source entity = new com.jobscout.domain.Source();
        apply(entity, input);
        entity = sources.save(entity);
        return ResponseEntity
            .created(URI.create("/api/v1/admin/sources/" + entity.getId()))
            .body(SourceMapper.toDto(entity));
    }

    @Override
    public ResponseEntity<Source> updateSource(Long id, SourceInput input) {
        com.jobscout.domain.Source entity = findSource(id);
        apply(entity, input);
        return ResponseEntity.ok(SourceMapper.toDto(sources.save(entity)));
    }

    @Override
    public ResponseEntity<Void> deleteSource(Long id) {
        sources.delete(findSource(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LocationTag> updateLocationTag(Long id, LocationTagUpdate update) {
        com.jobscout.domain.LocationTag tag = locations.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Location tag not found"));
        String country = blankToNull(update.getCountryCode());
        tag.setCountryCode(country == null ? null : country.toUpperCase(Locale.ROOT));
        tag.setRegionCode(blankToNull(update.getRegionCode()));
        tag.setCity(blankToNull(update.getCity()));
        // The geocoder job only picks tags with no country. A hand-cleared
        // country is maxed out on failures so the job never refills it.
        tag.setGeocodedAt(Instant.now());
        tag.setGeocodingFailures(country == null ? Integer.MAX_VALUE : 0);
        return ResponseEntity.ok(LocationTagMapper.toDto(locations.save(tag)));
    }

    private com.jobscout.domain.Source findSource(Long id) {
        return sources.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Source not found"));
    }

    private void apply(com.jobscout.domain.Source entity, SourceInput input) {
        Platform platform = Platform.valueOf(input.getPlatform().name());
        String boardId = input.getBoardId().strip();
        sources.findByPlatformAndBoardId(platform, boardId)
            .filter(other -> !other.getId().equals(entity.getId()))
            .ifPresent(other -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Source '" + other.getName() + "' already uses this board");
            });
        entity.setName(input.getName().strip());
        entity.setPlatform(platform);
        entity.setBoardId(boardId);
        entity.setActive(input.getIsActive());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
