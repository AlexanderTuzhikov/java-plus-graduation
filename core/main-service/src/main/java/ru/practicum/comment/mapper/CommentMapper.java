package ru.practicum.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.CommentShortDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.model.Comment;
import ru.practicum.user.mapper.UserMapper;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class})
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    Comment mapToComment(NewCommentDto newCommentDto);

    @Mapping(target = "author", source = "author")
    @Mapping(target = "eventId", source = "event.id")
    CommentFullDto mapToCommentFullDto(Comment comment);

    @Mapping(target = "author", source = "author")
    @Mapping(target = "eventId", source = "event.id")
    CommentShortDto mapToCommentShortDto(Comment comment);

    @Mapping(target = "comment", source = "comment")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    void updateCommentFromDto(UpdateCommentDto updateCommentDto, @MappingTarget Comment comment);
}