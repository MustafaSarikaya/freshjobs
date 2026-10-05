package com.jobscout.repository;

import com.jobscout.domain.Platform;
import com.jobscout.domain.Source;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Long> {
    List<Source> findByActiveTrue();
    List<Source> findByPlatform(Platform platform);
    Optional<Source> findByPlatformAndBoardId(Platform platform, String boardId);
}
