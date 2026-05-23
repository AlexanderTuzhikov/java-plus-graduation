package ru.practicum.comment.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.comment.dto.*;

import java.util.List;

public interface CommentService {

    CommentFullDto postComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    void deleteComment(Long userId, Long commentId);

    CommentFullDto patchComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    CommentFullDto getComment(Long userId, Long commentId);

    List<CommentFullDto> getComments(Long userId, Pageable pageable);

    CommentFullDto publishComment(Long commentId);

    CommentFullDto rejectComment(Long commentId);

    void deleteCommentByAdmin(Long commentId);

    List<CommentFullDto> searchComments(CommentSearchFilter filter, Pageable pageable);

    List<CommentShortDto> getPublishedComments(Long eventId, Pageable pageable);

    CommentShortDto getPublishedComment(Long eventId, Long commentId);
}