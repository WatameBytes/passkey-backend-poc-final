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
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Table(name = "PASSWORD")
public class Password {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ENCRYPTED_PASSWORD")
    private String encryptedPassword;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERSONA_ID", referencedColumnName = "ID")
    private Persona persona;
}
