package com.auraia.backend.mappers;

import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.entities.DiaryEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DiaryEntryMapper {

    @Mapping(target = "tags", expression = "java(entry.getTags() == null ? java.util.List.of() : java.util.List.copyOf(entry.getTags()))")
    DomainResponses.DiaryEntryResponse toResponse(DiaryEntry entry);
}
