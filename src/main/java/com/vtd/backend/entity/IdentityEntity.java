package com.vtd.backend.entity;

import lombok.*;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "identity_entity")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IdentityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String publicGuid;

    @Column(nullable = false)
    private String userId;

    @OneToMany(mappedBy = "identityEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PasskeyEntity> passkeyEntities = new ArrayList<>();
}