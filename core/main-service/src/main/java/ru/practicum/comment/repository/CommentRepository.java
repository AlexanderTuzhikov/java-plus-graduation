package ru.practicum.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.CommentState;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByAuthorId(Long userId, Pageable pageable);

    Page<Comment> findByEventIdAndState(Long eventId, CommentState state, Pageable pageable);

    Page<Comment> findByEventId(Long eventId, Pageable pageable);

    Page<Comment> findByState(CommentState state, Pageable pageable);

    List<Comment> findByEventIdInAndState(List<Long> eventIds, CommentState state);

    @Query("SELECT c FROM Comment c WHERE " +
            "(:userIds IS NULL OR c.author.id IN :userIds) AND " +
            "(:eventIds IS NULL OR c.event.id IN :eventIds) AND " +
            "(:states IS NULL OR c.state IN :states) AND " +
            "(:rangeStart IS NULL OR c.createdOn >= :rangeStart) AND " +
            "(:rangeEnd IS NULL OR c.createdOn <= :rangeEnd)")
    Page<Comment> searchComments(@Param("userIds") List<Long> userIds,
                                 @Param("eventIds") List<Long> eventIds,
                                 @Param("states") List<CommentState> states,
                                 @Param("rangeStart") LocalDateTime rangeStart,
                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                 Pageable pageable);
}