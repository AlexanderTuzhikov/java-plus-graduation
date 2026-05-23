package ru.practicum.comment.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.event.model.Event;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentState state;

    private LocalDateTime publishedOn;

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        Comment comment = (Comment) object;
        return id != 0 && id.equals(comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}