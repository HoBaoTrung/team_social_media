package com.codegym.socialmedia.model.account;

import com.codegym.socialmedia.general_interface.NormalRegister;
import com.codegym.socialmedia.model.admin.ModerationLog;
import com.codegym.socialmedia.model.conversation.ConversationParticipant;
import com.codegym.socialmedia.model.social_action.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank
    private String username;

    @Column(nullable = false, unique = true)
    @Email
    @NotBlank
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Size(max = 255)
    private String profilePicture;

    @Lob
    private String bio;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @NotBlank(groups = NormalRegister.class)
    @Pattern(regexp = "^(\\+84|0)(3[2-9]|5[6,8,9]|7[0,6-9]|8[1-5]|9[0-9])\\d{7}$"
            , message = "Sai định dạng"
            , groups = NormalRegister.class
    )
    @Column(unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    private LoginMethod loginMethod;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private boolean isActive = true;

    private boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "users_roles",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id")})
    private Set<Role> roles;

    @OneToMany(mappedBy = "user")
    private List<UserSearchHistory> searchesPerformed;

    @OneToMany(mappedBy = "resultUser")
    private List<UserSearchHistory> searchesFoundIn;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserPrivacySettings privacySettings;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private NotificationSettings notificationSettings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserSession> sessions;

    @OneToMany(mappedBy = "requester")
    private List<Friendship> sentFriendRequests;

    @OneToMany(mappedBy = "addressee")
    private List<Friendship> receivedFriendRequests;

    @OneToMany(mappedBy = "user")
    private List<LikePost> likedStatuses;

    @OneToMany(mappedBy = "user")
    private List<LikeComment> likedComments;

    @OneToMany(mappedBy = "user")
    private List<ConversationParticipant> conversations;

    @OneToMany(mappedBy = "blocker")
    private List<BlockedUsers> blockedUsers;

    @OneToMany(mappedBy = "blocked")
    private List<BlockedUsers> blockedBy;

    @OneToMany(mappedBy = "reporter")
    private List<ModerationLog> reports;

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum LoginMethod {
        EMAIL, FACEBOOK, GOOGLE
    }

    public enum AccountStatus {
        ACTIVE, SUSPENDED, BANNED, PENDING
    }


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public boolean isAdmin() {
        return this.roles.stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}