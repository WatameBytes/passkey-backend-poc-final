package com.vtd.backend.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import lombok.Data;

/**
 * This class represents the request to finish the registration process in the WebAuthn flow.
 * It contains the registration ID and the public key credential (which includes the attestation response).
 */
@Data
public class RegistrationFinishRequest {

    // The ID associated with the registration process
    private final String registrationId;

    // The public key credential containing the authenticator's attestation response and any client extension outputs
    private final PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential;

    /**
     * Constructor for the RegistrationFinishRequest.
     * This constructor is annotated with @JsonCreator and @JsonProperty to indicate that
     * it should be used to deserialize JSON into an instance of this class.
     *
     * @param registrationId The ID of the registration process.
     * @param credential The public key credential provided by the authenticator.
     */
    @JsonCreator
    public RegistrationFinishRequest(
            @JsonProperty("registrationId") String registrationId,
            @JsonProperty("credential") PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential) {
        this.registrationId = registrationId;
        this.credential = credential;
    }
}

