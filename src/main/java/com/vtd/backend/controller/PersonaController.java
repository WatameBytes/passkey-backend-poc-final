package com.vtd.backend.controller;

import com.vtd.backend.config.BackendConfig;
import com.vtd.backend.models.LoginRequest;
import com.vtd.backend.models.PersonaResponse;
import com.vtd.backend.models.RegisterRequest;
import com.vtd.backend.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BackendConfig.ACCOUNT_ENDPOINT)
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    @PostMapping("/register")
    public ResponseEntity<PersonaResponse> register(@RequestBody RegisterRequest registerRequest) {
        PersonaResponse personaResponse = personaService.registerUser(registerRequest);
        return ResponseEntity.ok(personaResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<PersonaResponse> login(@RequestBody LoginRequest loginRequest) {
        PersonaResponse personaResponse = personaService.loginUser(loginRequest);
        return ResponseEntity.ok(personaResponse);
    }
}
