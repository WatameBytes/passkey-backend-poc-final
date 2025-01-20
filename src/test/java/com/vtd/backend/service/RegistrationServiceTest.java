//package com.vtd.backend.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.vtd.backend.config.credentialRepository.CredentialService;
//import com.vtd.backend.entity.Persona;
//import com.vtd.backend.entity.RegistrationChallenge;
//import com.vtd.backend.models.PasskeyRegistrationRequest;
//import com.vtd.backend.models.RegistrationStartResponse;
//import com.vtd.backend.repository.PersonaRepository;
//import com.vtd.backend.repository.RegistrationChallengeRepository;
//import com.yubico.webauthn.RelyingParty;
//import com.yubico.webauthn.data.RelyingPartyIdentity;
//import com.yubico.webauthn.extension.appid.AppId;
//import com.yubico.webauthn.extension.appid.InvalidAppIdException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Collections;
//import java.util.Optional;
//import java.util.Set;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//public class RegistrationServiceTest {
//    private AppId appId;
//    private RelyingParty relyingParty;
//    private PasskeyRegistrationRequest passkeyRegistrationRequest;
//
//    private Persona persona;
//
//    @Mock
//    private CredentialService credentialService;
//
//    @Mock
//    private PersonaRepository personaRepository;
//
//    @Mock
//    private RegistrationChallengeRepository registrationChallengeRepository;
//
//    @InjectMocks
//    private RegistrationService registrationService;
//
//
//    @BeforeEach
//    public void setUp() throws InvalidAppIdException {
//        // Create the object we pass into our registration start method
//        passkeyRegistrationRequest = new PasskeyRegistrationRequest();
//        passkeyRegistrationRequest.setUsername("testuser");
//
//        // Persona object we get back from the database
//        persona = new Persona();
//        persona.setId(1L);
//        persona.setUsername("testuser");
//
//        // Mock the PersonaRepository to return the persona
//        when(personaRepository.findByUsername("testuser")).thenReturn(Optional.of(persona));
//
//        // Mock the CredentialService to return the PersonaRepository
//        when(credentialService.getPersonaRepository()).thenReturn(personaRepository);
//
//        // Mock the getCredentialIdsForUsername to return an empty set
//        when(credentialService.getCredentialIdsForUsername("testuser")).thenReturn(Collections.emptySet());
//
//        // Mock the getRegistrationChallengeRepository to return the mocked repository
//        when(credentialService.getRegistrationChallengeRepository()).thenReturn(registrationChallengeRepository);
//
//        // Mock the save method of RegistrationChallengeRepository to do nothing
//        when(registrationChallengeRepository.save(any(RegistrationChallenge.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        // Create AppId object
//        appId = new AppId("https://example.com");
//
//        // Create RelyingParty object
//        Set<String> origins = Set.of("https://example.com");
//        relyingParty = RelyingParty.builder()
//                .identity(RelyingPartyIdentity.builder()
//                        .id("example.com")
//                        .name("Example Relying Party")
//                        .build())
//                .credentialRepository(credentialService)
//                .origins(origins)
//                .build();
//
//        // Inject the created RelyingParty and AppId into the registrationService
//        registrationService = new RegistrationService(credentialService, appId, relyingParty);
//    }
//
//    @Test
//    public void testStartRegistration_Success() throws IOException {
//        RegistrationChallenge mockRegistrationChallenge = new RegistrationChallenge();
//        mockRegistrationChallenge.setRegistrationId("mock-registration-id");
//        mockRegistrationChallenge.setUsername("testuser");
//        mockRegistrationChallenge.setPublicKeyCredentialCreationOptionsJson("mock-creation-options-json");
//
//        RegistrationService spyRegistrationService = Mockito.spy(registrationService);
//        doReturn(mockRegistrationChallenge).when(spyRegistrationService).saveOrUpdateNewId(anyString(), anyString());
//
//
//        RegistrationStartResponse response = spyRegistrationService.startRegistration(passkeyRegistrationRequest);
//
//        assertNotNull(response);
//        assertEquals("testuser", response.getUsername());
//        assertEquals("mock-registration-id", response.getRegistrationId());
//        assertNotNull(response.getPublicKeyCredentialCreationOptions());
//
//        // Ensure the directory exists
//        Path directoryPath = Paths.get("src/test/resources/startRegistration");
//        Files.createDirectories(directoryPath);
//
//        // Write the response to a JSON file in the resource folder
//        ObjectMapper objectMapper = new ObjectMapper();
//        File file = directoryPath.resolve("input.json").toFile();
//        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, response);
//    }
//}
