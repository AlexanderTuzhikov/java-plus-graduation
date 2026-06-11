package ru.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.category.model.Category;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventRequest;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.model.Event;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiatorId", source = "userId")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "location", source = "newEventRequest.location")
    @Mapping(target = "publishedOn", ignore = true)
    Event toEvent(NewEventRequest newEventRequest, Category category, Long userId);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "location", source = "event.location")
    @Mapping(target = "initiator", source = "userShortDto")
    @Mapping(target = "rating", source = "rating", defaultValue = "0.0")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests", defaultValue = "0L")
    EventFullDto toEventFullDto(Event event, Double rating, Long confirmedRequests, UserShortDto userShortDto);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "initiator", source = "userShortDto")
    @Mapping(target = "rating", source = "rating", defaultValue = "0.0")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests", defaultValue = "0L")
    EventShortDto toEventShortDto(Event event, Double rating, Long confirmedRequests, UserShortDto userShortDto);
}