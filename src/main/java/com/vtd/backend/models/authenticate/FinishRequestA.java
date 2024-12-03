package com.vtd.backend.models.authenticate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinishRequestA {

    private String assertionId;
    private String publicKeyCredentialJson;
}
