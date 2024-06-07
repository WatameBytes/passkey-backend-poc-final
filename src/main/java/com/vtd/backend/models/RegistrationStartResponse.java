package com.vtd.backend.models;

import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RegistrationStartResponse {
    private String registrationId;
    private PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions;
    private String username;
}
