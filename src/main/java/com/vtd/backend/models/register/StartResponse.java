package com.vtd.backend.models.register;

import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import lombok.Data;

@Data
public class StartResponse {
    private String registrationId;

    private String publicKeyCredentialCreationOptions;
}
