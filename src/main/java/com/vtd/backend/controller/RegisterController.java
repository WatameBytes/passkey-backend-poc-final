package com.vtd.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vtd.backend.config.BackendConfig;
import com.vtd.backend.entity.ChallengeEntity;
import com.vtd.backend.entity.IdentityEntity;
import com.vtd.backend.entity.PasskeyEntity;
import com.vtd.backend.models.register.FinishRequest;
import com.vtd.backend.models.register.FinishResponse;
import com.vtd.backend.models.register.StartRequest;
import com.vtd.backend.models.register.StartResponse;
import com.vtd.backend.repository.ChallengeRepository;
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
import com.yubico.webauthn.exception.RegistrationFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.rowset.serial.SerialBlob;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class RegisterController {

    @Autowired
    private IdentityEntityRepository identityEntityRepository;

    @Autowired
    private PasskeyEntityRepository passkeyEntityRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private RelyingParty relyingParty;

    @PostMapping("/registration/start")
    public ResponseEntity<StartResponse> startRegistration(@RequestBody StartRequest startRequest) throws JsonProcessingException {
        Optional<IdentityEntity> findExistingUser = identityEntityRepository.findByPublicGuid(startRequest.getPublicGuid());

        LocalDateTime currentDateTime = LocalDateTime.now();
        String name = String.format("%s-%s", "Fuwa", currentDateTime);

        UserIdentity userIdentity = UserIdentity.builder()
                .name(name)
                .displayName(name)
                .id(ByteArray.fromBase64(startRequest.getPublicGuid()))
                .build();

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

        // Save to cassandra db
        ChallengeEntity challengeEntity = new ChallengeEntity();
        challengeEntity.setChallengeId(UUID.randomUUID().toString());
        String saveToDatabase = request.toJson();

        challengeEntity.setChallengeJson(saveToDatabase);
        challengeRepository.save(challengeEntity);

        // Construct the response
        StartResponse response = new StartResponse();
        response.setRegistrationId(challengeEntity.getChallengeId());

        String sendToClient = request.toCredentialsCreateJson();
        response.setPublicKeyCredentialCreationOptions(sendToClient);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/registration/finish")
    public ResponseEntity<FinishResponse> finishRegistration(@RequestBody FinishRequest finishRequest) throws Exception {
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                PublicKeyCredential.parseRegistrationResponseJson(finishRequest.getPublicKeyCredentialString());

        // Grabbing from CassandraDatabase
        Optional<ChallengeEntity> requestObject = challengeRepository.findByChallengeId(finishRequest.getRegistrationId());

        if (requestObject.isEmpty()) {
            return ResponseEntity.badRequest().body(new FinishResponse("Invalid registration ID"));
        }

        PublicKeyCredentialCreationOptions request = PublicKeyCredentialCreationOptions.fromJson(requestObject.get().getChallengeJson());

        try {
            RegistrationResult result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(request) // Data saved from CassandraDB
                    .response(pkc)
                    .build());

            String credentialIdBase64 = result.getKeyId().getId().getBase64();
            byte[] publicKey = result.getPublicKeyCose().getBytes();

            byte[] userId = request.getUser().getId().getBytes();
            String publicGuid = Base64.getEncoder().encodeToString(userId);


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
