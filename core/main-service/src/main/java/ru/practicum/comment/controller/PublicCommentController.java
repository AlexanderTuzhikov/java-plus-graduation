package ru.practicum.comment.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentShortDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@Slf4j
@Validated
@RestController
@AllArgsConstructor
@RequestMapping(path = "/events/{eventId}/comments")
public class PublicCommentController {
    private final CommentService commentService;

    @GetMapping
    public List<CommentShortDto> getPublishedComments(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "publishedOn"));

        return commentService.getPublishedComments(eventId, pageable);
    }

    @GetMapping("/{commentId}")
    public CommentShortDto getPublishedComment(
            @PathVariable Long eventId,
            @PathVariable Long commentId) {

        return commentService.getPublishedComment(eventId, commentId);
    }
}