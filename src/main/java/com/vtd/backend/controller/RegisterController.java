package com.vtd.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vtd.backend.config.BackendConfig;
import com.vtd.backend.models.PasskeyRegistrationRequest;
import com.vtd.backend.models.RegistrationFinishRequest;
import com.vtd.backend.models.RegistrationFinishResponse;
import com.vtd.backend.models.RegistrationStartResponse;
import com.vtd.backend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(BackendConfig.REGISTER_ENDPOINT)
public class RegisterController {

    private final RegistrationService registrationService;

    // TODO: Why are we passing the username back?
    // Look into that
    @PostMapping("/registration/start")
    public ResponseEntity<RegistrationStartResponse> startRegistration(@RequestBody PasskeyRegistrationRequest passkeyRegistrationRequest) throws JsonProcessingException {
        RegistrationStartResponse registrationStartResponse = registrationService.startRegistration(passkeyRegistrationRequest);
        return ResponseEntity.ok(registrationStartResponse);
    }

    @PostMapping("/registration/finish")
    public ResponseEntity<RegistrationFinishResponse> finishRegistration(@RequestBody RegistrationFinishRequest registrationFinishRequest) throws Exception {
        RegistrationFinishResponse registrationFinishResponse = registrationService.finishRegistration(registrationFinishRequest);
        return ResponseEntity.ok(registrationFinishResponse);
    }

}
