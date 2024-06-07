package com.vtd.backend.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PersonaResponse {
    private Long id;
    private String personaId;
    private String username;
}
