package com.vtd.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vtd.backend.credentialRepository.CredentialService;
import com.vtd.backend.entity.AssertionChallenge;
import com.vtd.backend.entity.Persona;
import com.vtd.backend.models.AssertionFinishRequest;
import com.vtd.backend.models.AssertionStartResponse;
import com.vtd.backend.models.UsernameRequest;
import com.vtd.backend.repository.PersonaRepository;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.data.AssertionExtensionInputs;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.extension.appid.AppId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import static com.vtd.backend.utils.WebAuthnUtils.deserializeAssertionResult;

@RequiredArgsConstructor
@Service
public class AuthenticationService {
    private final CredentialService credentialService;
    private final RelyingParty relyingParty;
    private final AppId appId;

    public AssertionStartResponse startAssertion(UsernameRequest usernameRequest) throws JsonProcessingException {
        String username = usernameRequest.getUsername();

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        Optional<Persona> personaEntity = credentialService.getPersonaRepository().findByUsername(username);

        if (personaEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Username not found");
        }

        Persona persona = personaEntity.get();

        // Create the extensions input
        AssertionExtensionInputs extensionInputs = AssertionExtensionInputs.builder()
                .appid(appId)
                .build();

        StartAssertionOptions startAssertionOptions = StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.PREFERRED)
                .timeout(60000L)
                .username(persona.getUsername())
                .extensions(extensionInputs)
                .build();

        AssertionRequest assertionRequest = relyingParty.startAssertion(startAssertionOptions);

        System.out.println("BEFORE US CONVERTING IT");
        System.out.println(assertionRequest);
        // Serialize to Json
        String assertionRequestJson = assertionRequest.toJson();
        System.out.println("WHAT WE HAVE and converted");
        System.out.println(assertionRequestJson);

        //String publicKeyCredentialRequestOptionsJson = assertionRequest.getPublicKeyCredentialRequestOptions().toCredentialsGetJson();
        AssertionChallenge assertionChallenge = saveOrUpdateNewId(username, assertionRequestJson);
        System.out.println("We serialized the following: " + assertionRequestJson);
        credentialService.getAssertionChallengeRepository().save(assertionChallenge);

        return new AssertionStartResponse(assertionChallenge.getAssertionId(), assertionRequest);
    }

    public String finishAssertion(AssertionFinishRequest assertionFinishRequest) throws AssertionFailedException {
        System.out.println("BEFORE CHECKING");
        System.out.println("Assertion ID: " + assertionFinishRequest.getAssertionId());

        Optional<AssertionChallenge> assertionChallenge = credentialService.getAssertionChallengeRepository()
                .findByAssertionId(assertionFinishRequest.getAssertionId());

        System.out.println("AFTER ASSERTION ID CHECKER");

        if (assertionChallenge.isEmpty()) {
            System.out.println("Assertion ID not found");
            return "Assertion ID not found";
        }

        System.out.println("BEFORE DESERIALIZING");
        String json = assertionChallenge.get().getAssertionRequestJson();
        System.out.println(json);

        try {
            AssertionRequest resultFrom = AssertionRequest.fromJson(json);
            System.out.println("SUCCESSFULLY DESERIALIZED");
            System.out.println("BEFORE RELYING PARTY");

            AssertionResult result = relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(resultFrom)
                            .response(assertionFinishRequest.getCredential())
                            .build()
            );

            System.out.println("RESULT IS: " + result);

            if (result.isSuccess()) {
                System.out.println("Assertion successful");

                if (!credentialService.updateSignatureCount(result)) {
                    System.out.println("Failed to update signature count");
                    return "Failed to update signature count";
                }

                return "Assertion successful";
            } else {
                System.out.println("Assertion failed");
                return "Assertion failed";
            }

        } catch (JsonProcessingException e) {
            System.out.println("FAILED AT DESERIALIZING");
            e.printStackTrace();
            return "Failed to deserialize the assertion request JSON.";
        } catch (Exception e) {
            System.out.println("ERROR DURING ASSERTION");
            e.printStackTrace();
            return "An error occurred during assertion processing.";
        }
    }


    public AssertionChallenge saveOrUpdateNewId(String username, String assertionRequestJson) {
        Optional<AssertionChallenge> existingChallenge = credentialService.getAssertionChallengeRepository().findByUsername(username);

        AssertionChallenge challenge;
        if (existingChallenge.isPresent()) {
            challenge = existingChallenge.get();
            challenge.setAssertionRequestJson(assertionRequestJson);
            challenge.setAssertionId(UUID.randomUUID().toString());
        } else {
            challenge = new AssertionChallenge();
            challenge.setAssertionId(UUID.randomUUID().toString());
            challenge.setUsername(username);
            challenge.setAssertionRequestJson(assertionRequestJson);
        }

        credentialService.getAssertionChallengeRepository().save(challenge);
        System.out.println("We serialized and saved the following: " + assertionRequestJson);
        System.out.println("Saved Assertion ID: " + challenge.getAssertionId());

        return challenge;
    }

}
