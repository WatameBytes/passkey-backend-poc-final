package com.vtd.backend.mapper;

import com.vtd.backend.entity.Persona;
import com.vtd.backend.models.PersonaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PersonaMapper {
    PersonaMapper INSTANCE = Mappers.getMapper(PersonaMapper.class);

    PersonaResponse personaToPersonaResponse(Persona persona);
}
