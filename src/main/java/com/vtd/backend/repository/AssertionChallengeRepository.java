package com.vtd.backend.repository;

import com.vtd.backend.entity.AssertionChallenge;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface AssertionChallengeRepository extends MongoRepository<AssertionChallenge, String> {
    Optional<AssertionChallenge> findByAssertionId(String assertionId);
    Optional<AssertionChallenge> findByUsername(String username);
    Optional<AssertionChallenge> findById(String id);
}
