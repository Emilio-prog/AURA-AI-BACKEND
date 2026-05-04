package com.auraia.backend.mappers;

import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.entities.DiaryEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DiaryEntryMapper {

    DomainResponses.DiaryEntryResponse toResponse(DiaryEntry entry);
}
