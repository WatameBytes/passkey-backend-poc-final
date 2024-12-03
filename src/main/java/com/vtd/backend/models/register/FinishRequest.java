package com.vtd.backend.models.register;

import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import lombok.Data;

@Data
public class FinishRequest {
    private String registrationId;


    private String publicKeyCredentialString;
}
