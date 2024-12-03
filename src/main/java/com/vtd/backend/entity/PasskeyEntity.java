package com.vtd.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Blob;

@Entity
@Table(name = "passkey_entity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasskeyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "identity_id", referencedColumnName = "id")
    private IdentityEntity identityEntity;

    @Column(nullable = false, unique = true)
    private String passkeyUuid;

    @Lob
    @Column(name = "PUBLIC_KEY")
    private Blob publicKey;

    @Column(nullable = false, unique = true)
    private String credentialId;
}