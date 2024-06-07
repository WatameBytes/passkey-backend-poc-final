package com.vtd.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vtd.backend.credentialRepository.CredentialService;
import com.vtd.backend.entity.Persona;
import com.vtd.backend.entity.RegistrationChallenge;
import com.vtd.backend.models.PasskeyRegistrationRequest;
import com.vtd.backend.models.RegistrationFinishRequest;
import com.vtd.backend.models.RegistrationFinishResponse;
import com.vtd.backend.models.RegistrationStartResponse;
import com.vtd.backend.utils.BytesUtil;
import com.vtd.backend.utils.WebAuthnUtils;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialParameters;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.RegistrationExtensionInputs;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.extension.appid.AppId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final CredentialService credentialService;

    private final AppId appId;

    private static final Long TIMEOUT = 20000L; // 20 seconds

    private final RelyingParty relyingParty;

    public RegistrationStartResponse startRegistration(PasskeyRegistrationRequest passkeyRegistrationRequest) throws JsonProcessingException {
        String username = passkeyRegistrationRequest.getUsername();

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        Optional<Persona> accountEntity = credentialService.getPersonaRepository().findByUsername(username);

        if (accountEntity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Username not found");
        }

        byte[] accountIdBytes = BytesUtil.longToBytes(accountEntity.get().getId());

        // Create a UserIdentity object
        UserIdentity userIdentity = UserIdentity.builder()
                .name(username)
                .displayName(username)
                .id(new ByteArray(accountIdBytes))
                .build();

        AuthenticatorSelectionCriteria authenticatorSelection = AuthenticatorSelectionCriteria.builder()
                // .authenticatorAttachment(AuthenticatorAttachment.PLATFORM) // Specifies the type of authenticator to use:
                // Options:
                // - AuthenticatorAttachment.PLATFORM: Uses a platform authenticator (built into the device, e.g., Touch ID, Windows Hello).
                // - AuthenticatorAttachment.CROSS_PLATFORM: Uses a cross-platform authenticator (external devices like USB security keys).
                // - Omit this setting to allow both platform and cross-platform authenticators.

                // .userVerification(UserVerificationRequirement.PREFERRED) // Specifies the user verification requirement:
                // Options:
                // - UserVerificationRequirement.PREFERRED: Prefers user verification (biometrics, PIN), but allows registration without it if not available.
                // - UserVerificationRequirement.DISCOURAGED: Suggests not using user verification, but does not prohibit it.
                // - UserVerificationRequirement.REQUIRED: Enforces user verification; registration will fail if the authenticator cannot perform it.
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();

        // Define the AppId extension
        RegistrationExtensionInputs extensions = RegistrationExtensionInputs.builder()
                .appidExclude(appId) // Specifies the AppID exclusion extension:
                // The AppID extension is used to support legacy FIDO U2F authenticators.

                .credProps(true) // Specifies the credProps extension:
                // Indicates that the client should return information about the credential's properties.

                //.largeBlob()
                //.uvm()
                .build();

        // Define public key credential parameters
        // Included Algorithms:
        // - ES256 (ECDSA with P-256 and SHA-256): Widely used, good balance of security and performance, recommended by NIST.
        // - ES384 (ECDSA with P-384 and SHA-384): Higher security than ES256, suitable for environments with higher security requirements.
        // - ES512 (ECDSA with P-521 and SHA-512): Highest level of security among the ECDSA algorithms.
        // - RS256 (RSASSA-PKCS1-v1_5 with SHA-256): Commonly used, strong security, suitable for compatibility with older systems.
        // - RS384 (RSASSA-PKCS1-v1_5 with SHA-384): Higher security than RS256.
        // - RS512 (RSASSA-PKCS1-v1_5 with SHA-512): Highest level of security among the RSA algorithms.
        //
        // Excluded Algorithms:
        // - EdDSA (Edwards-curve Digital Signature Algorithm): Not included due to its relatively recent adoption and potential compatibility issues in some enterprise environments.
        // - RS1 (RSASSA-PKCS1-v1_5 with SHA-1): Not included due to known security weaknesses in SHA-1.
        // Define public key credential parameters
        List<PublicKeyCredentialParameters> pubKeyCredParams = List.of(
                PublicKeyCredentialParameters.builder()
                        .alg(COSEAlgorithmIdentifier.ES256)
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build(),
                PublicKeyCredentialParameters.builder()
                        .alg(COSEAlgorithmIdentifier.ES384)
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build(),
                PublicKeyCredentialParameters.builder()
                        .alg(COSEAlgorithmIdentifier.ES512)
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build(),
                PublicKeyCredentialParameters.builder()
                        .alg(COSEAlgorithmIdentifier.RS256)
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build(),
                PublicKeyCredentialParameters.builder()
                        .alg(COSEAlgorithmIdentifier.RS384)
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build(),
                PublicKeyCredentialParameters.builder()
                        .alg(COSEAlgorithmIdentifier.RS512)
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build()
        );

        // Start the registration process
        StartRegistrationOptions startRegistrationOptions = StartRegistrationOptions.builder()
                .user(userIdentity)
                .timeout(TIMEOUT) // Specifies the timeout for the registration operation:
                // Options:
                // - TIMEOUT: The amount of time the operation is allowed to take, in milliseconds.
                .authenticatorSelection(authenticatorSelection)
                .extensions(extensions)
                .build();

        // Get the PublicKeyCredentialCreationOptions object
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = relyingParty
                .startRegistration(startRegistrationOptions)
                .toBuilder()
                .pubKeyCredParams(pubKeyCredParams)
                .build();

        // Serialize to JSON
        String publicKeyCredentialCreationOptionsJson = publicKeyCredentialCreationOptions.toCredentialsCreateJson();

        // This commented section shows the original approach where the registration ID is not updated after the initial save or update.
        // The registration ID remains the same if the username already exists in the database.
        //
        // // Find or create a RegistrationChallenge
        // RegistrationChallenge registrationChallenge = saveOrUpdate(username, publicKeyCredentialCreationOptionsJson);
        //
        // // Create the RegistrationStartResponse to return to the client
        // RegistrationStartResponse registrationStartResponse = new RegistrationStartResponse();
        // registrationStartResponse.setRegistrationId(registrationChallenge.getRegistrationId());
        // registrationStartResponse.setUsername(username);
        // registrationStartResponse.setPublicKeyCredentialCreationOptions(publicKeyCredentialCreationOptions);
        // return registrationStartResponse;

        // This new approach generates a new registration ID every time, even if the username already exists in the database.
        RegistrationChallenge registrationChallenge = saveOrUpdateNewId(username, publicKeyCredentialCreationOptionsJson);
        credentialService.getRegistrationChallengeRepository().save(registrationChallenge);

        // Create the RegistrationStartResponse to return to the client
        RegistrationStartResponse registrationStartResponse = new RegistrationStartResponse();
        registrationStartResponse.setRegistrationId(registrationChallenge.getRegistrationId());
        registrationStartResponse.setUsername(username);
        registrationStartResponse.setPublicKeyCredentialCreationOptions(publicKeyCredentialCreationOptions);
        return registrationStartResponse;
    }

    // Method to save or update the RegistrationChallenge
    public RegistrationChallenge saveOrUpdate(String username, String publicKeyCredentialCreationOptionsJson) {
        Optional<RegistrationChallenge> existingChallenge = credentialService.getRegistrationChallengeRepository().findByUsername(username);
        if (existingChallenge.isPresent()) {
            RegistrationChallenge challengeToUpdate = existingChallenge.get();
            challengeToUpdate.setPublicKeyCredentialCreationOptionsJson(publicKeyCredentialCreationOptionsJson);
            return credentialService.getRegistrationChallengeRepository().save(challengeToUpdate);
        } else {
            RegistrationChallenge newChallenge = new RegistrationChallenge();
            newChallenge.setRegistrationId(UUID.randomUUID().toString());
            newChallenge.setUsername(username);
            newChallenge.setPublicKeyCredentialCreationOptionsJson(publicKeyCredentialCreationOptionsJson);
            return credentialService.getRegistrationChallengeRepository().save(newChallenge);
        }
    }


    // Method to save or update the RegistrationChallenge - Different registrationId each time
    public RegistrationChallenge saveOrUpdateNewId(String username, String publicKeyCredentialCreationOptionsJson) {
        Optional<RegistrationChallenge> existingChallenge = credentialService.getRegistrationChallengeRepository().findByUsername(username);
        if (existingChallenge.isPresent()) {
            RegistrationChallenge challengeToUpdate = existingChallenge.get();
            challengeToUpdate.setPublicKeyCredentialCreationOptionsJson(publicKeyCredentialCreationOptionsJson);
            challengeToUpdate.setRegistrationId(UUID.randomUUID().toString()); // Generate a new registrationId each time
            return credentialService.getRegistrationChallengeRepository().save(challengeToUpdate);
        } else {
            RegistrationChallenge newChallenge = new RegistrationChallenge();
            newChallenge.setRegistrationId(UUID.randomUUID().toString());
            newChallenge.setUsername(username);
            newChallenge.setPublicKeyCredentialCreationOptionsJson(publicKeyCredentialCreationOptionsJson);
            return credentialService.getRegistrationChallengeRepository().save(newChallenge);
        }
    }

    public RegistrationFinishResponse finishRegistration(RegistrationFinishRequest registrationFinishRequest) throws Exception {
        Optional<RegistrationChallenge> startResponseOptional = credentialService.getRegistrationChallengeRepository().findByRegistrationId(registrationFinishRequest.getRegistrationId());

        if (startResponseOptional.isEmpty()) {
            throw new Exception("Registration ID not found");
        }

        PublicKeyCredentialCreationOptions deserializedOptions = WebAuthnUtils.deserializePublicKeyCredentialCreationOptions(startResponseOptional.get().getPublicKeyCredentialCreationOptionsJson());
        PublicKeyCredentialCreationOptions requestObject = deserializedOptions;

        UserIdentity userIdentity = requestObject.getUser();
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> responseObject = registrationFinishRequest.getCredential();
        RegistrationResult registrationResult;
        try {
            registrationResult = relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                        .request(requestObject)
                        .response(responseObject)
                        .build());
            Long id = BytesUtil.bytesToLong(userIdentity.getId().getBytes());
            String credentialIdBase64 = registrationResult.getKeyId().getId().getBase64();
            byte[] publicKey = registrationResult.getPublicKeyCose().getBytes();
            credentialService.addCredential(id, credentialIdBase64, publicKey);
            return new RegistrationFinishResponse("Registration successful");
        } catch (Exception e) {
            throw new Exception("Registration failed: " + e.getMessage());
        }
    }
}
