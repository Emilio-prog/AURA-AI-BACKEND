package com.auraia.backend.mappers;

import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.entities.Contact;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContactMapper {

    DomainResponses.ContactResponse toResponse(Contact contact);
}
