package com.vtd.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vtd.backend.models.AssertionFinishRequest;
import com.vtd.backend.models.AssertionStartResponse;
import com.vtd.backend.models.UsernameRequest;
import com.vtd.backend.service.AuthenticationService;
import com.yubico.webauthn.exception.AssertionFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/authenticate")
public class AuthenticateController {

    private final AuthenticationService authenticationService;

    @PostMapping("/assertion/start")
    public ResponseEntity<AssertionStartResponse> startAssertion(@RequestBody UsernameRequest usernameRequest) throws JsonProcessingException {
        AssertionStartResponse assertionStartResponse = authenticationService.startAssertion(usernameRequest);
        return ResponseEntity.ok(assertionStartResponse);
    }

    @PostMapping("/assertion/finish")
    public ResponseEntity<String> finishAssertion(@RequestBody AssertionFinishRequest assertionFinishRequest) {
        System.out.println("INSIDE THE CONTROLLER");

        try {
            String responseMessage = authenticationService.finishAssertion(assertionFinishRequest);
            System.out.println("AFTER SERVICE");

            if (responseMessage.equals("Assertion successful")) {
                return ResponseEntity.ok(responseMessage);
            } else {
                System.out.println("ERROR");
                System.out.println(responseMessage);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseMessage);
            }

        } catch (Exception e) {
            System.out.println("GENERAL EXCEPTION");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }



}
