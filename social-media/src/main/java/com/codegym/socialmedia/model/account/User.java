package com.codegym.socialmedia.model.account;

import com.codegym.socialmedia.general_interface.NormalRegister;
import com.codegym.socialmedia.model.admin.ModerationLog;
import com.codegym.socialmedia.model.conversation.ConversationParticipant;
import com.codegym.socialmedia.model.social_action.*;
import com.nimbusds.openid.connect.sdk.claims.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    @Email
    @NotBlank
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String firstName;
    private String lastName;
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
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @BatchSize(size = 20)
    private Set<Role> roles;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    @JoinColumn(name = "privacy_settings_id")
    private UserPrivacySettings privacySettings;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_settings_id")
    private NotificationSettings notificationSettings;

    public enum LoginMethod {
        EMAIL, FACEBOOK, GOOGLE
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum AccountStatus {
        ACTIVE, SUSPENDED, BANNED, PENDING
    }



    public boolean isAdmin() {
        return roles != null &&
                roles.stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
    }

    public String getFullName() {
        return (firstName == null ? "" : firstName) + " " +
                (lastName == null ? "" : lastName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return id != null && id.equals(((User) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}