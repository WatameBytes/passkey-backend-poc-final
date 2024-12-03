package com.vtd.backend.config.credentialRepository;

import com.datastax.oss.driver.shaded.guava.common.io.ByteStreams;
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
        // Passkey - ....
        // Assume we pass a JWT here
        System.out.println("Username is : " + username);

        System.out.println("GET CREDENTIAL WAS CALLED");
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

    @Override
    @Transactional
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        String credentialIdBase64 = Base64.getEncoder().encodeToString(credentialId.getBytes());
        String publicGuid = Base64.getEncoder().encodeToString(userHandle.getBytes());


        System.out.println("CredentialId : " + credentialIdBase64);

        Optional<PasskeyEntity> passkeyEntityOptional =
                passkeyEntityRepository.findByCredentialId(credentialIdBase64);

        if (!passkeyEntityOptional.get().getIdentityEntity().getPublicGuid().equals(publicGuid)) {
            System.out.println("Failed to find a Passkey");
            return Optional.empty();
        }


        System.out.println("Passkey Entity: " + passkeyEntityOptional.get());

        return passkeyEntityOptional.map(passkeyEntity ->
                RegisteredCredential.builder()
                        .credentialId(new ByteArray(Base64.getDecoder().decode(passkeyEntity.getCredentialId())))
                        .userHandle(userHandle)
                        .publicKeyCose(blobToByteArray(passkeyEntity.getPublicKey()))
                        .signatureCount(0) // Adjust based on your business logic
                        .build()
        );
    }

    @Override
    @Transactional
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
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
