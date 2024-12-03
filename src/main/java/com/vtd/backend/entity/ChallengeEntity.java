package com.vtd.backend.entity;

import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Table(value = "passkey_challenge")
public class ChallengeEntity {

    @PrimaryKeyColumn(name = "challengeid", type = PrimaryKeyType.PARTITIONED)
    private String challengeId;

    private String challengeJson;
}
