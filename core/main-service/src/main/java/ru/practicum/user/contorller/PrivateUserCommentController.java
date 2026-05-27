package ru.practicum.user.contorller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdateCommentDto;
import ru.practicum.comment.service.CommentService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateUserCommentController {
    private final CommentService commentService;

    @PostMapping("/event/{eventId}/comment")
    public ResponseEntity<CommentFullDto> postComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody NewCommentDto newCommentDto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.postComment(userId, eventId, newCommentDto));
    }

    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {

        commentService.deleteComment(userId, commentId);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/comment/{commentId}")
    public ResponseEntity<CommentFullDto> patchComment(
            @PathVariable Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentDto updateCommentDto) {

        return ResponseEntity.ok().body(commentService.patchComment(userId, commentId, updateCommentDto));
    }

    @GetMapping("/comment/{commentId}")
    public ResponseEntity<CommentFullDto> getComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {

        return ResponseEntity.ok().body(commentService.getComment(userId, commentId));
    }

    @GetMapping
    public ResponseEntity<List<CommentFullDto>> getComments(
            @PathVariable Long userId,
            @RequestParam(name = "from", defaultValue = "0") Integer from,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());

        return ResponseEntity.ok().body(commentService.getComments(userId, pageable));
    }
}