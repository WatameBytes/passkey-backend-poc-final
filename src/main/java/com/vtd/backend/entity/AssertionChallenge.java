package com.vtd.backend.entity;

import com.yubico.webauthn.AssertionRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;

@Document(collection = "assertionChallenges")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@CompoundIndex(def = "{'username': 1}", unique = true)
public class AssertionChallenge {

    @Id
    private String id; // MongoDB's unique identifier
    private String assertionId; // Our business logic identifier
//
//    private String publicKeyCredentialRequestOptionsJson;
    private String username;

    private String assertionRequestJson;
}
