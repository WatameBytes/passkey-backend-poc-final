package com.vtd.backend.repository;

import com.vtd.backend.entity.Passkey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasskeyRepository extends JpaRepository<Passkey, Long> {
    Optional<Passkey> findByCredentialId(String credentialId);
}
