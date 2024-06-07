package com.vtd.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.persistence.Id;

@Document(collection = "registrationChallenges")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@CompoundIndex(def = "{'username': 1}", unique = true)
public class RegistrationChallenge {

    @Id
    private String id; // MongoDB's unique identifier
    private String registrationId; // Our business logic identifier
    private String publicKeyCredentialCreationOptionsJson;
    private String username;
}
