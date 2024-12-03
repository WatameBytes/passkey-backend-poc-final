package com.vtd.backend.repository;

import com.vtd.backend.entity.ChallengeEntity;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChallengeRepository extends CassandraRepository<ChallengeEntity, String> {

    Optional<ChallengeEntity> findByChallengeId(String challengeId);

    Optional<ChallengeEntity> deleteByChallengeId(String challengeId);
}
