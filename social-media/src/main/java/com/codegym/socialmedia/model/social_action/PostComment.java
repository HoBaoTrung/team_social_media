// PostComment.java
package com.codegym.socialmedia.model.social_action;

import com.codegym.socialmedia.model.account.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "post_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true) // chỉ in field được đánh dấu @ToString.Include
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(nullable = false)
    @ToString.Include
    private String content;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // LikeComment
//    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
//    @ToString.Exclude
//    private List<LikeComment> likedByUsers = new ArrayList<>();

    // Parent comment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonBackReference
    @ToString.Exclude
    private PostComment parent;

    // Replies
    // PostComment.java (chỉ phần replies)
    @OneToMany(mappedBy = "parent",
            cascade = { CascadeType.PERSIST, CascadeType.MERGE }, // không có REMOVE
            orphanRemoval = false,
            fetch = FetchType.LAZY)
    @JsonManagedReference
    @ToString.Exclude
    private List<PostComment> replies = new ArrayList<>();


    // Mentions
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<CommentMention> mentions = new HashSet<>();

    // Override toString gọn nhẹ, tránh in vòng lặp
    @Override
    public String toString() {
        return "[id: " + id + ", content: " + content + "]";
    }

    @Transient
    public List<User> getMentionedUsers() {
        return mentions.stream()
                .map(CommentMention::getMentionedUser)
                .collect(Collectors.toList());
    }
}
