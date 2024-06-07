package com.vtd.backend.service;

import com.vtd.backend.entity.Password;
import com.vtd.backend.entity.Persona;
import com.vtd.backend.mapper.PersonaMapper;
import com.vtd.backend.models.LoginRequest;
import com.vtd.backend.models.PersonaResponse;
import com.vtd.backend.models.RegisterRequest;
import com.vtd.backend.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonaService {

    @Autowired
    private PersonaRepository personaRepository;

    public PersonaResponse registerUser(RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();

        if (personaRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        String encryptedPassword = encryptPassword(password);
        Password passwordEntity = new Password();
        passwordEntity.setEncryptedPassword(encryptedPassword);

        Persona persona = new Persona();
        persona.setPersonaId(UUID.randomUUID().toString());
        persona.setUsername(username);
        persona.setPassword(passwordEntity);
        passwordEntity.setPersona(persona);

        personaRepository.save(persona);

        return PersonaMapper.INSTANCE.personaToPersonaResponse(persona);
    }

    public PersonaResponse loginUser(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        Optional<Persona> personaOptional = personaRepository.findByUsername(username);
        if (personaOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        Persona persona = personaOptional.get();
        String encryptedPassword = encryptPassword(password);

        if (!persona.getPassword().getEncryptedPassword().equals(encryptedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return PersonaMapper.INSTANCE.personaToPersonaResponse(persona);
    }

    private String encryptPassword(String password) {
        // Simple Base64 encoding for demonstration purposes
        return Base64.getEncoder().encodeToString(password.getBytes());
    }
}