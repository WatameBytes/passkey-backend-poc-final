package com.vtd.backend.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;

/**
 * Utility class for handling WebAuthn-related operations.
 */
public class WebAuthnUtils {

    /**
     * Deserializes a JSON string into a {@link PublicKeyCredentialCreationOptions} object.
     *
     * {@link PublicKeyCredentialCreationOptions#toCredentialsCreateJson()} method nests
     * the object under a {@code publicKey} field, but the {@code fromJson(String)} method
     * expects a direct representation. This utility extracts and converts the nested field
     * to the expected format for deserialization.
     *
     * @param jsonString the JSON string containing the {@code publicKey} field.
     * @return the deserialized {@link PublicKeyCredentialCreationOptions} object.
     * @throws JsonProcessingException if JSON processing fails.
     */
    public static PublicKeyCredentialCreationOptions deserializePublicKeyCredentialCreationOptions(String jsonString) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonString);
        JsonNode publicKeyNode = rootNode.path("publicKey");
        String publicKeyJsonString = mapper.writeValueAsString(publicKeyNode);
        return PublicKeyCredentialCreationOptions.fromJson(publicKeyJsonString);
    }

    public static AssertionRequest deserializeAssertionResult(String jsonString) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonString);
        JsonNode assertionNode = rootNode.path("publicKey");
        String assertionJsonString = mapper.writeValueAsString(assertionNode);
        return mapper.readValue(assertionJsonString, AssertionRequest.class);
    }

//    public static PublicKeyCredentialRequestOptions deserializePublicKey(String jsonString) throws JsonProcessingException {
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode rootNode = mapper.readTree(jsonString);
//        JsonNode publicKeyNode = rootNode.path("publicKey");
//        String publicKeyJsonString = mapper.writeValueAsString(publicKeyNode);
//        return PublicKeyCredentialRequestOptions.fromJson(publicKeyJsonString);
//    }
}
