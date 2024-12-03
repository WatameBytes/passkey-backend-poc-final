package com.vtd.backend.repository;

import com.vtd.backend.entity.PasskeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasskeyEntityRepository extends JpaRepository<PasskeyEntity, Long> {
    Optional<PasskeyEntity> findByCredentialId(String credentialId);
}
