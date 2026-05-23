package ru.practicum.comment.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.CommentSearchFilter;
import ru.practicum.comment.model.CommentState;
import ru.practicum.comment.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Validated
@RestController
@AllArgsConstructor
@RequestMapping(path = "/admin/comment")
public class AdminCommentController {
    private final CommentService commentService;

    @PatchMapping("/{commentId}/publish")
    public CommentFullDto publishComment(
            @PathVariable Long commentId) {

        return commentService.publishComment(commentId);
    }

    @PatchMapping("/{commentId}/reject")
    public CommentFullDto rejectComment(
            @PathVariable Long commentId) {
        return commentService.rejectComment(commentId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentByAdmin(@PathVariable Long commentId) {

        commentService.deleteCommentByAdmin(commentId);
    }

    @GetMapping
    public List<CommentFullDto> searchComments(
            @RequestParam(required = false) List<Long> userIds,
            @RequestParam(required = false) List<Long> eventIds,
            @RequestParam(required = false) List<CommentState> states,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        CommentSearchFilter filter = new CommentSearchFilter();
        filter.setUserIds(userIds);
        filter.setEventIds(eventIds);
        filter.setStates(states);
        filter.setRangeStart(rangeStart);
        filter.setRangeEnd(rangeEnd);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "createdOn"));

        return commentService.searchComments(filter, pageable);
    }
}