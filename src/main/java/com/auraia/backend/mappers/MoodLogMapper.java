package com.auraia.backend.mappers;

import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.entities.MoodLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MoodLogMapper {

    DomainResponses.MoodLogResponse toResponse(MoodLog moodLog);
}
