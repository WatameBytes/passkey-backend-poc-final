package com.vtd.backend.repository;

import com.vtd.backend.entity.RegistrationChallenge;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface RegistrationChallengeRepository extends MongoRepository<RegistrationChallenge, String> {
    Optional<RegistrationChallenge> findByRegistrationId(String registrationId);
    Optional<RegistrationChallenge> findByUsername(String username);
    Optional<RegistrationChallenge> findById(String id);
}
