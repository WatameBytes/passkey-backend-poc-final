package com.vtd.backend.credentialRepository;

import com.vtd.backend.entity.Passkey;
import com.vtd.backend.entity.Persona;
import com.vtd.backend.repository.PasskeyRepository;
import com.vtd.backend.repository.PersonaRepository;
import com.vtd.backend.repository.RegistrationChallengeRepository;
import com.vtd.backend.utils.BlobUtil;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import javax.sql.rowset.serial.SerialBlob;
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

    private final PersonaRepository personaRepository;
    private final RegistrationChallengeRepository registrationChallengeRepository;
    private final PasskeyRepository passkeyRepository;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        Optional<Persona> personaOptional = personaRepository.findByUsername(username);
        if (personaOptional.isEmpty()) {
            throw new RuntimeException("Username not found: " + username);
        }
        Persona persona = personaOptional.get();

        // Fetch Persona Passkeys
        List<Passkey> passkeys = persona.getPasskeys();
        System.out.println("Size is: " + passkeys.size());

        return passkeys.stream()
                .map(passkey -> PublicKeyCredentialDescriptor.builder()
                    .id(new ByteArray(Base64.getDecoder().decode(passkey.getCredentialId())))
                    .transports(Collections.emptySet())
                    .build())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return Optional.empty();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return Optional.empty();
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {

        String credentialIdBase64 = Base64.getEncoder().encodeToString(credentialId.getBytes());

        return passkeyRepository.findByCredentialId(credentialIdBase64)
                .stream()
                .map(passkey -> {
                    byte[] publicKeyBytes = BlobUtil.blobToBytes(passkey.getPublicKey());
                    return RegisteredCredential.builder()
                            .credentialId(new ByteArray(passkey.getCredentialId().getBytes()))  // Assuming credentialId is Base64 encoded
                            .userHandle(new ByteArray(passkey.getPersona().getPersonaId().getBytes()))
                            .publicKeyCose(new ByteArray(publicKeyBytes))
                            .signatureCount(passkey.getCount())
                            .build();
                })
                .collect(Collectors.toSet());
    }

    @Transactional
    public void addCredential(Long id, String credentialIdBase64, byte[] publicKeyBytes) {
        try {
            // Convert byte[] to Blob
            Blob publicKeyBlob = new SerialBlob(publicKeyBytes);

            // Retrieve the Persona entity using the id
            Optional<Persona> personaOptional = personaRepository.findById(id);
            if (personaOptional.isPresent()) {
                Persona persona = personaOptional.get();

                // Create a Passkey entity and set its properties
                Passkey passkey = new Passkey();
                passkey.setCredentialId(credentialIdBase64);
                passkey.setPublicKey(publicKeyBlob);
                passkey.setPersona(persona);

                // Link the Passkey entity to the Persona
                persona.getPasskeys().add(passkey);

                // Save the Passkey entity
                passkeyRepository.save(passkey);
            } else {
                throw new EntityNotFoundException("Persona not found with ID: " + id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error converting byte[] to Blob", e);
        }
    }

    @Transactional
    public boolean updateSignatureCount(AssertionResult result) {
        return true;
    }
}
