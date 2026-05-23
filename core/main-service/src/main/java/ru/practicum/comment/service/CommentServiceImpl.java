package ru.practicum.comment.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.*;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentState;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.handler.exception.ConflictException;
import ru.practicum.handler.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Transactional
    @Override
    public CommentFullDto postComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("POST comment user ID={} by event ID={}", userId, eventId);
        User user = checkUserExists(userId);
        Event event = checkEventExists(eventId);
        checkEventStateForCommentAction(event);

        Comment comment = commentMapper.mapToComment(newCommentDto);
        comment.setAuthor(user);
        comment.setEvent(event);
        comment.setState(CommentState.PENDING);
        comment.setCreatedOn(LocalDateTime.now());
        log.debug("MAP comment {}", comment);

        Comment savedComment = commentRepository.save(comment);
        log.debug("SAVE comment {}", savedComment);

        return commentMapper.mapToCommentFullDto(savedComment);
    }

    @Transactional
    @Override
    public void deleteComment(Long userId, Long commentId) {
        User user = checkUserExists(userId);
        Comment comment = checkCommentExists(commentId);
        checkCommentAuthor(user, comment);
        Event event = checkEventExists(comment.getEvent().getId());
        checkEventStateForCommentAction(event);

        commentRepository.delete(comment);
        log.info("DELETE comment ID={}", commentId);
    }

    @Transactional
    @Override
    public CommentFullDto patchComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        log.info("PATCH comment user ID={}, comment ID={}", userId, commentId);
        User user = checkUserExists(userId);
        Comment comment = checkCommentExists(commentId);
        Event event = checkEventExists(comment.getEvent().getId());
        checkEventStateForCommentAction(event);
        checkCommentAuthor(user, comment);

        commentMapper.updateCommentFromDto(updateCommentDto, comment);
        comment.setState(CommentState.PENDING);

        if (comment.getPublishedOn() != null) {
            comment.setPublishedOn(null);
        }

        log.debug("PATCHED comment {}", comment);

        Comment updatedComment = commentRepository.save(comment);
        log.debug("SAVED comment {}", updatedComment);

        return commentMapper.mapToCommentFullDto(updatedComment);
    }

    @Override
    public CommentFullDto getComment(Long userId, Long commentId) {
        log.info("GET comment ID={}", commentId);
        User user = checkUserExists(userId);
        Comment comment = checkCommentExists(commentId);
        checkCommentAuthor(user, comment);

        log.debug("FIND comment {}", comment);

        return commentMapper.mapToCommentFullDto(comment);
    }

    @Override
    public List<CommentFullDto> getComments(Long userId, Pageable pageable) {
        log.info("GET comments for user ID={}", userId);
        checkUserExists(userId);

        Page<Comment> comments = commentRepository.findByAuthorId(userId, pageable);
        log.debug("FIND comments elements={} for user ID={}", comments.getTotalElements(), userId);

        return comments.stream()
                .map(commentMapper::mapToCommentFullDto)
                .toList();
    }

    @Transactional
    @Override
    public CommentFullDto publishComment(Long commentId) {
        log.info("PUBLISH comment ID={}", commentId);
        Comment comment = checkCommentExists(commentId);

        if (!comment.getState().equals(CommentState.PENDING)) {
            log.error("Comment ID={} is not in PENDING state. Current state: {}", commentId, comment.getState());
            throw new ConflictException("Only comments in PENDING state can be published");
        }

        checkEventStateForCommentAction(comment.getEvent());

        comment.setState(CommentState.PUBLISHED);
        comment.setPublishedOn(LocalDateTime.now());

        Comment publishedComment = commentRepository.save(comment);
        log.debug("PUBLISHED comment {}", publishedComment);

        return commentMapper.mapToCommentFullDto(publishedComment);
    }

    @Transactional
    @Override
    public CommentFullDto rejectComment(Long commentId) {
        log.info("REJECT comment ID={}", commentId);
        Comment comment = checkCommentExists(commentId);

        if (!comment.getState().equals(CommentState.PENDING)) {
            log.error("Comment ID={} is not in PENDING state. Current state: {}", commentId, comment.getState());
            throw new ConflictException("Only comments in PENDING state can be rejected");
        }

        comment.setState(CommentState.REJECTED);

        Comment rejectedComment = commentRepository.save(comment);
        log.debug("REJECTED comment {}", rejectedComment);

        return commentMapper.mapToCommentFullDto(rejectedComment);
    }

    @Transactional
    @Override
    public void deleteCommentByAdmin(Long commentId) {
        log.info("DELETE comment by admin ID={}", commentId);
        Comment comment = checkCommentExists(commentId);

        commentRepository.delete(comment);
        log.debug("DELETED comment by admin {}", comment);
    }

    @Override
    public List<CommentFullDto> searchComments(CommentSearchFilter filter, Pageable pageable) {
        log.info("SEARCH comments with filter: {}", filter);

        Page<Comment> comments = commentRepository.searchComments(
                filter.getUserIds(),
                filter.getEventIds(),
                filter.getStates(),
                filter.getRangeStart(),
                filter.getRangeEnd(),
                pageable
        );

        log.debug("FOUND comments elements={}", comments.getTotalElements());

        return comments.stream()
                .map(commentMapper::mapToCommentFullDto)
                .toList();
    }

    @Override
    public List<CommentShortDto> getPublishedComments(Long eventId, Pageable pageable) {
        log.info("GET published comments for event ID={}", eventId);
        Event event = checkEventExists(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            log.error("Event ID={} is not PUBLISHED. Current state: {}", eventId, event.getState());
            throw new ConflictException("Comments can only be viewed for published events");
        }

        Page<Comment> comments = commentRepository.findByEventIdAndState(
                eventId,
                CommentState.PUBLISHED,
                pageable
        );

        log.debug("FOUND published comments elements={} for event ID={}",
                comments.getTotalElements(), eventId);

        return comments.stream()
                .map(commentMapper::mapToCommentShortDto)
                .toList();
    }

    @Override
    public CommentShortDto getPublishedComment(Long eventId, Long commentId) {
        log.info("GET published comment ID={} for event ID={}", commentId, eventId);
        Event event = checkEventExists(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            log.error("Event ID={} is not PUBLISHED. Current state: {}", eventId, event.getState());
            throw new ConflictException("Comments can only be viewed for published events");
        }

        Comment comment = checkCommentExists(commentId);

        if (!comment.getState().equals(CommentState.PUBLISHED)) {
            log.error("Comment ID={} is not PUBLISHED. Current state: {}", commentId, comment.getState());
            throw new NotFoundException("Published comment not found");
        }

        if (!comment.getEvent().getId().equals(eventId)) {
            log.error("Comment ID={} does not belong to event ID={}", commentId, eventId);
            throw new ConflictException("Comment does not belong to specified event");
        }

        log.debug("FOUND published comment {}", comment);

        return commentMapper.mapToCommentShortDto(comment);
    }

    private User checkUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User {} not found", userId);
                    return new NotFoundException("User ID=" + userId + " not found");
                });
    }

    private Comment checkCommentExists(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("Comment {} not found", commentId);
                    return new NotFoundException("Comment ID=" + commentId + " not found");
                });
    }

    private Event checkEventExists(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
    }

    private void checkEventStateForCommentAction(Event event) {
        if (event.getState().equals(EventState.PENDING) || event.getState().equals(EventState.CANCELED)) {
            log.error("Event ID={} has state '{}' — comments are prohibited", event.getId(), event.getState());
            throw new ConflictException("Unable to comment: event is pending review or cancelled");
        }
    }

    private void checkCommentAuthor(User user, Comment comment) {
        if (!Objects.equals(comment.getAuthor().getId(), user.getId())) {
            log.error("Conflict: Attempt to modify comment by unauthorized user." +
                    "Expected author ID={}, Actual user ID={}", comment.getAuthor().getId(), user.getId());
            throw new ConflictException("Conflict: Attempt to modify comment by unauthorized user.");
        }
    }
}