package com.vtd.backend.config.credentialRepository;

import com.vtd.backend.entity.IdentityEntity;
import com.vtd.backend.entity.PasskeyEntity;
import com.vtd.backend.repository.IdentityEntityRepository;
import com.vtd.backend.repository.PasskeyEntityRepository;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Getter
@Setter
@RequiredArgsConstructor
public class CredentialService implements CredentialRepository {



    @Autowired
    private PasskeyEntityRepository passkeyEntityRepository;

    @Autowired
    private IdentityEntityRepository identityEntityRepository;

    // StartRegistration calls this,
    // We will need a way to Pass a JWT or header data in here later
    @Override
    @Transactional
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        System.out.println("GET CREDENTIAL IDS FOR USERNAME CALLED");
        // Pass an empty list, since we don't have a username
        return Collections.emptySet();
    }

    // Only used during the finish assertion step IF a username is present
    @Override
    @Transactional
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        System.out.println("GET USERHANDLE FOR USERNAME CALLED");
        return Optional.empty();
    }

    @Override
    @Transactional
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        System.out.println("GET USERNAME FOR USERHANDLE CALLED");
        System.out.println("UserHandle: " + userHandle);
        System.out.println("UserHandle in base64: " + userHandle.getBase64());
        return Optional.of(userHandle.getBase64());
        //return Optional.empty();
    }

    /**
     * Looks up a WebAuthn registered credential using credentialId and userHandle.
     *
     * The key challenge here was handling the format mismatch between WebAuthn's ByteArray
     * userHandle and our database's raw GUID string storage:
     *
     * - WebAuthn passes userHandle as ByteArray (e.g., ByteArray(6775696431677569643167756964))
     * - Our database stores it as raw string (e.g., "guid1guid1guid")
     *
     * The solution was to decode the ByteArray userHandle to its raw string form before
     * comparing with the stored GUID, rather than comparing Base64 encoded versions.
     * This matches our database schema while maintaining WebAuthn's required ByteArray
     * format in the RegisteredCredential response.
     *
     * @param credentialId The WebAuthn credential ID as ByteArray
     * @param userHandle The user identifier as ByteArray (contains raw GUID when decoded)
     * @return Optional<RegisteredCredential> with matching credential if found and GUID matches,
     *         empty Optional otherwise
     */
    @Override
    @Transactional
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        String credentialIdBase64 = Base64.getEncoder().encodeToString(credentialId.getBytes());
        // Decode the userHandle to get the raw GUID
        String decodedUserHandle = new String(userHandle.getBytes());

        Optional<PasskeyEntity> passkeyEntityOptional = passkeyEntityRepository.findByCredentialId(credentialIdBase64);

        if (!passkeyEntityOptional.isPresent()) {
            System.out.println("No passkey found with credentialId: " + credentialIdBase64);
            return Optional.empty();
        }

        String storedGuid = passkeyEntityOptional.get().getIdentityEntity().getPublicGuid();

        if (!storedGuid.equals(decodedUserHandle)) {
            System.out.println("PublicGuid mismatch");
            System.out.println("Expected: " + decodedUserHandle);
            System.out.println("Actual: " + storedGuid);
            return Optional.empty();
        }

        return passkeyEntityOptional.map(passkeyEntity ->
                RegisteredCredential.builder()
                        .credentialId(credentialId)  // Use original credentialId ByteArray
                        .userHandle(userHandle)      // Use original userHandle ByteArray
                        .publicKeyCose(blobToByteArray(passkeyEntity.getPublicKey()))
                        .signatureCount(0)
                        .build()
        );
    }

    @Override
    @Transactional
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        System.out.println("LOOKUPALL WAS CALLED");
        System.out.println("CredentialId in ByteArray: " + credentialId);

        // Convert credential ID to Base64 for repository lookup
        String credentialIdBase64 = Base64.getEncoder().encodeToString(credentialId.getBytes());
        System.out.println("CredentialIdBase64: " + credentialIdBase64);

        // Retrieve credentials from the repository
        Optional<PasskeyEntity> optionalEntity = passkeyEntityRepository.findByCredentialId(credentialIdBase64);

        System.out.println("LOOKUP ALL WAS CALLED");

        // If no credentials found, return an empty set
        if (optionalEntity.isEmpty()) {
            return Collections.emptySet();
        }

        PasskeyEntity passkeyEntity = optionalEntity.get();

        // Build the RegisteredCredential
        RegisteredCredential registeredCredential = RegisteredCredential.builder()
                .credentialId(new ByteArray(Base64.getDecoder().decode(passkeyEntity.getCredentialId())))
                .userHandle(new ByteArray(passkeyEntity.getIdentityEntity().getPublicGuid().getBytes()))
                .publicKeyCose(blobToByteArray(passkeyEntity.getPublicKey())) // Convert BLOB directly to ByteArray
                .signatureCount(0L) // Hardcoded counter value
                .build();

        // Return a set containing the RegisteredCredential
        return Set.of(registeredCredential);
    }

    private static ByteArray blobToByteArray(Blob blob) {
        try {
            return new ByteArray(blob.getBinaryStream().readAllBytes());
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Error converting Blob to ByteArray", e);
        }
    }



}
