package com.vtd.backend.models.authenticate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartResponseA {

    private String assertionId;

    private String credentialJson;
}
