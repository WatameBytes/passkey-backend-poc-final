package com.vtd.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vtd.backend.config.CacheService;
import com.vtd.backend.entity.ChallengeEntity;
import com.vtd.backend.models.authenticate.FinishRequestA;
import com.vtd.backend.models.authenticate.FinishResponseA;
import com.vtd.backend.models.authenticate.StartRequestA;
import com.vtd.backend.models.authenticate.StartResponseA;
import com.vtd.backend.models.register.FinishResponse;
import com.vtd.backend.repository.IdentityEntityRepository;
import com.vtd.backend.repository.PasskeyEntityRepository;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.data.AssertionExtensionInputs;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class AuthenticateController {

    @Autowired
    private IdentityEntityRepository identityEntityRepository;

    @Autowired
    private PasskeyEntityRepository passkeyEntityRepository;

    @Autowired
    private RelyingParty relyingParty;

    @Autowired
    private CacheService cacheService;

    @PostMapping("/authenticate/start")
    public ResponseEntity<StartResponseA> startAuthenticate(@RequestBody StartRequestA startAuthenticate) throws JsonProcessingException {
        AssertionRequest request = relyingParty.startAssertion(
                StartAssertionOptions.builder()
                        .timeout(60000L)
                        .build());

        String credentialGetJson = request.toCredentialsGetJson();
        String saveToCache = request.toJson();

        // Generate a unique challenge ID
        String assertionId = UUID.randomUUID().toString();

        // Save challenge to cache
        cacheService.save(assertionId, saveToCache);

        // Construct the response for the client
        StartResponseA clientResponse = new StartResponseA();
        clientResponse.setAssertionId(assertionId);
        clientResponse.setCredentialJson(credentialGetJson);

        return ResponseEntity.ok(clientResponse);
    }

    @PostMapping("/authenticate/finish")
    public ResponseEntity<FinishResponseA> finishAuthenticate(@RequestBody FinishRequestA finishAuthenticate) throws IOException {
        // Retrieve the challenge JSON from the cache
        String challengeJson = cacheService.retrieve(finishAuthenticate.getAssertionId());

        if (challengeJson == null) {
            // Handle cache miss
            return ResponseEntity.badRequest().body(new FinishResponseA("Invalid or expired registration ID"));
        }

        AssertionRequest request = AssertionRequest.fromJson(challengeJson);
        //System.out.println(request);
        //System.out.println("String: " + request.getUserHandle().get().toString());

        try {
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                    PublicKeyCredential.parseAssertionResponseJson(finishAuthenticate.getPublicKeyCredentialJson());

            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(request)  // The PublicKeyCredentialRequestOptions from startAssertion above
                    .response(pkc)
                    .build());

            if (result.isSuccess()) {
                System.out.println("SUCCESS");
                return ResponseEntity.ok(new FinishResponseA("Success"));
            }
        } catch (AssertionFailedException e) {
            System.out.println(e);
            return ResponseEntity.badRequest().body(new FinishResponseA("Authentication failed: " + e.getMessage()));
        }

        return ResponseEntity.ok(new FinishResponseA("Success"));
    }

}
