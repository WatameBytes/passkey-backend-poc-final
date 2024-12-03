package com.vtd.backend.repository;

import com.vtd.backend.entity.IdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityEntityRepository extends JpaRepository<IdentityEntity, Long> {

    Optional<IdentityEntity> findByPublicGuid(String publicGuid);
}
