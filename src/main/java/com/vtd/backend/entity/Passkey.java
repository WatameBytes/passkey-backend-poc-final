package com.vtd.backend.entity;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.sql.Blob;

@Entity
@Getter
@Setter
@Table(name = "PASSKEY")
public class Passkey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CREDENTIAL_ID")
    private String credentialId;

    @Lob
    @Column(name = "PUBLIC_KEY")
    private Blob publicKey;

    @Column(name = "COUNT", nullable = false)
    private Long count = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSONA_ID", referencedColumnName = "ID")
    private Persona persona;
}
