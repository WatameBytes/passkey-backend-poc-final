package com.vtd.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vtd.backend.config.CacheService;
import com.vtd.backend.entity.IdentityEntity;
import com.vtd.backend.entity.PasskeyEntity;
import com.vtd.backend.models.register.FinishRequest;
import com.vtd.backend.models.register.FinishResponse;
import com.vtd.backend.models.register.StartRequest;
import com.vtd.backend.models.register.StartResponse;
import com.vtd.backend.repository.IdentityEntityRepository;
import com.vtd.backend.repository.PasskeyEntityRepository;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.data.exception.Base64UrlException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.rowset.serial.SerialBlob;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 *The core issue was about string encoding paths. Here's what was happening before:
 *
 *Original string "guid1guid1guid"
 *   → Treated as Base64URL (wrong! it wasn't Base64URL encoded)
 *   → Decoded as Base64URL (producing garbage bytes)
 *   → Re-encoded as Base64URL (producing "guid1guid1guiQ")
 *
 * What we fixed by using .getBytes(StandardCharsets.UTF_8):
 *Original string "guid1guid1guid"
 *   → Converted directly to UTF-8 bytes (preserving exact string content)
 *   → Stored as ByteArray (preserving exact bytes)
 *   → Converted back from bytes to string (recovering original content)
 */
@RequiredArgsConstructor
@RestController
public class RegisterController {

    @Autowired
    private IdentityEntityRepository identityEntityRepository;

    @Autowired
    private PasskeyEntityRepository passkeyEntityRepository;
    @Autowired
    private RelyingParty relyingParty;

    @Autowired
    private CacheService cacheService;

    /**
     * Initiates a WebAuthn registration process for an existing user.
     *
     * @param startRequest The registration request containing the user's public GUID
     * @return ResponseEntity containing the registration options for the authenticator
     * @throws JsonProcessingException If there's an error processing JSON
     * @throws Base64UrlException If there's an error with Base64URL encoding
     *
     * @apiNote The publicGuid in the request is expected to be a plain string. It will be
     * converted to bytes using UTF-8 encoding for WebAuthn registration. This ensures the
     * exact string value is preserved throughout the registration process.
     */
    @PostMapping("/registration/start")
    public ResponseEntity<StartResponse> startRegistration(@RequestBody StartRequest startRequest) throws JsonProcessingException, Base64UrlException {
        // Check if the user already exists
        Optional<IdentityEntity> existingUser = identityEntityRepository.findByPublicGuid(startRequest.getPublicGuid());

        if (existingUser.isEmpty()) {
            // If the user doesn't exist, return an error or handle user creation
            return ResponseEntity.badRequest().body(null); // Adjust error handling as needed
        }

        LocalDateTime currentDateTime = LocalDateTime.now();
        String name = String.format("%s-%s", "FuwaMoco", currentDateTime);

        System.out.println("PublicGuid before: " + startRequest.getPublicGuid());

        //String publicGuid = startRequest.getPublicGuid();

        //ByteArray byteArray = ByteArray.fromBase64Url(publicGuid);
        ByteArray byteArray = new ByteArray(startRequest.getPublicGuid().getBytes(StandardCharsets.UTF_8));

        UserIdentity userIdentity = UserIdentity.builder()
                .name(name)
                .displayName(name)
                //.id(ByteArray.fromBase64(startRequest.getPublicGuid()))
                .id(byteArray)
                .build();

        System.out.println(byteArray.getBase64Url());

        AuthenticatorSelectionCriteria authenticatorSelectionCriteria = AuthenticatorSelectionCriteria.builder()
                //.residentKey(ResidentKeyRequirement.PREFERRED)
                .userVerification(UserVerificationRequirement.PREFERRED)
                //.authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
                .build();


        PublicKeyCredentialCreationOptions request = relyingParty.startRegistration(
                StartRegistrationOptions.builder()
                        .user(userIdentity)
                        .authenticatorSelection(authenticatorSelectionCriteria)
                        .timeout(60000L)
                        .build());


        // Generate a unique challenge ID
        String challengeId = UUID.randomUUID().toString();
        String saveToCache = request.toJson();

        // Save challenge to cache
        cacheService.save(challengeId, saveToCache);

        // Construct the response
        StartResponse response = new StartResponse();
        response.setRegistrationId(challengeId);
        response.setPublicKeyCredentialCreationOptions(request.toCredentialsCreateJson());

        return ResponseEntity.ok(response);
    }


    /**
     * Completes the WebAuthn registration process by validating the authenticator response.
     *
     * @param finishRequest Contains the registration ID and authenticator response
     * @return ResponseEntity with the registration result
     * @throws Exception If registration fails or data cannot be processed
     *
     * @apiNote This method recovers the original publicGuid by converting the stored bytes
     * back to a string using UTF-8 encoding. This ensures the GUID matches exactly what
     * was provided in the start registration process.
     */
    @PostMapping("/registration/finish")
    public ResponseEntity<FinishResponse> finishRegistration(@RequestBody FinishRequest finishRequest) throws Exception {
        // Retrieve challenge JSON from cache
        String challengeJson = cacheService.retrieve(finishRequest.getRegistrationId());

        if (challengeJson == null) {
            // Handle cache miss
            return ResponseEntity.badRequest().body(new FinishResponse("Invalid or expired registration ID"));
        }

        PublicKeyCredentialCreationOptions request = PublicKeyCredentialCreationOptions.fromJson(challengeJson);

        try {
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                    PublicKeyCredential.parseRegistrationResponseJson(finishRequest.getPublicKeyCredentialString());

            RegistrationResult result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());

            String credentialIdBase64 = result.getKeyId().getId().getBase64();
            byte[] publicKey = result.getPublicKeyCose().getBytes();

            byte[] userId = request.getUser().getId().getBytes();

            ByteArray byteArray = new ByteArray(userId);
            //String publicGuid = byteArray.getBase64Url(); // Use Base64Url without padding
            //String publicGuid = Base64.getUrlEncoder().withoutPadding().encodeToString(userId);
            //String publicGuid = byteArray.getBase64Url();
            String publicGuid = new String(userId, StandardCharsets.UTF_8);


            System.out.println("Raw userId: " + Arrays.toString(userId));
            System.out.println("Encoded publicGuid: " + publicGuid);

            PasskeyEntity passkey = new PasskeyEntity();
            passkey.setCredentialId(credentialIdBase64);
            passkey.setPublicKey(new SerialBlob(publicKey));
            passkey.setPasskeyUuid(UUID.randomUUID().toString());

            System.out.println("PublicGuid: " + publicGuid);
            Optional<IdentityEntity> identity = identityEntityRepository.findByPublicGuid(publicGuid);

            if (identity.isEmpty()) {
                return ResponseEntity.badRequest().body(new FinishResponse("Identity not found for public GUID"));
            }

            passkey.setIdentityEntity(identity.get());
            passkeyEntityRepository.save(passkey);
            System.out.println("Success");
            return ResponseEntity.ok(new FinishResponse("Registration completed successfully"));
        } catch (RegistrationFailedException e) {
            return ResponseEntity.badRequest().body(new FinishResponse("Registration failed: " + e.getMessage()));
        }
    }


}
