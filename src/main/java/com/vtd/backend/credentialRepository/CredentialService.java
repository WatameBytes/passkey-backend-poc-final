package com.vtd.backend.credentialRepository;

import com.vtd.backend.entity.Passkey;
import com.vtd.backend.entity.Persona;
import com.vtd.backend.repository.AssertionChallengeRepository;
import com.vtd.backend.repository.PasskeyRepository;
import com.vtd.backend.repository.PersonaRepository;
import com.vtd.backend.repository.RegistrationChallengeRepository;
import com.vtd.backend.utils.BlobUtil;
import com.vtd.backend.utils.BytesUtil;
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
    private final AssertionChallengeRepository assertionChallengeRepository;
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

    // TODO: YOU BREAK!!!
    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        System.out.println("GET USERHANDLE FOR USERNAME");
        Optional<Persona> personaOptional = personaRepository.findByUsername(username);
        if (personaOptional.isEmpty()) {
            throw new RuntimeException("Username not found: " + username);
        }

        Persona persona = personaOptional.get();
        System.out.println("PERSONA FOUND IN HERE IS " + persona);
        return Optional.of(new ByteArray(BytesUtil.longToBytes(persona.getId())));
    }

    // TODO: NOT VERIFIED 100%, build after looking at the other two
    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        System.out.println("GET USERNAME FOR HANDLE INVOKED");
        Long personaId = BytesUtil.bytesToLong(userHandle.getBytes());
        Optional<Persona> personaOptional = personaRepository.findById(personaId);

        // If personaOptional is empty, return Optional.empty(), otherwise return the username
        return personaOptional.map(Persona::getUsername);
    }

    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        System.out.println("LOOKUP INVOKED");
        System.out.println("USER HANDLE IS : " + Base64.getEncoder().encodeToString(userHandle.getBytes()));
        String credentialIdBase64 = Base64.getEncoder().encodeToString(credentialId.getBytes());
        long personaId = BytesUtil.bytesToLong(userHandle.getBytes());
        System.out.println("PERSONAID IS " + personaId);

        System.out.println("CONVERTED BYTES TO LONG");
        // Find the persona by ID
        Optional<Persona> personaOptional = personaRepository.findById(personaId);
        if (personaOptional.isEmpty()) {
            System.out.println("NO PERSONA ID FOUND");
            return Optional.empty();
        }
        System.out.println("GRABBING PERSONA");
        Persona persona = personaOptional.get();
        List<Passkey> passkeys = persona.getPasskeys();
        if (passkeys.isEmpty()) {
            System.out.println("PASSKEYS WAS EMPTY");
            return Optional.empty();
        }

        // Find the matching passkey
        System.out.println("BEFORE STREAM");
        System.out.println("TARGET CREDENTIAL ID IS " + credentialIdBase64);

        for (Passkey passkey : passkeys) {
            System.out.println("PASSKEY IS " + passkey.getCredentialId());
            if (credentialIdBase64.equals(passkey.getCredentialId())) {
                System.out.println("MATCHING PASSKEY FOUND");
                System.out.println("Passkey Details: ");
                System.out.println("  Public Key COSE: " + passkey.getPublicKey());
                System.out.println("  Signature Count: " + passkey.getCount());

                ByteArray publicKeyCose = new ByteArray(BlobUtil.blobToBytes(passkey.getPublicKey()));
                System.out.println("Converted Public Key COSE: " + Base64.getEncoder().encodeToString(publicKeyCose.getBytes()));

                RegisteredCredential registeredCredential = RegisteredCredential.builder()
                        .credentialId(new ByteArray(Base64.getDecoder().decode(passkey.getCredentialId())))
                        .userHandle(userHandle)// IT WAS YOU!!!!!
                        .publicKeyCose(publicKeyCose)
                        .signatureCount(passkey.getCount())
                        .build();
                System.out.println("REGISTERED CREDENTIAL CREATED: " + registeredCredential);
                return Optional.of(registeredCredential);
            }
        }

        System.out.println("NO MATCHING PASSKEY FOUND");
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
