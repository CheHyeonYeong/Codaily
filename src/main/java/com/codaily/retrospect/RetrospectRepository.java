package com.codaily.retrospect;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetrospectRepository extends JpaRepository<RetrospectEntity, UUID> {

    List<RetrospectEntity> findAllByOrderByViewCountDescCreatedAtDesc(Pageable pageable);
}
