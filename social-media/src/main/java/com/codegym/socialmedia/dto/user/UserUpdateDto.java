package com.codegym.socialmedia.dto.user;

import com.codegym.socialmedia.annotation.MinAge;
import com.codegym.socialmedia.annotation.Unique;
import com.codegym.socialmedia.general_interface.NormalRegister;
import com.codegym.socialmedia.model.account.User;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {
    private String username;
    private Long id;
    @Email(message = "Email không hợp lệ")
    @NotBlank
    @Unique(entityClass = User.class, fieldName = "email", idField = "id",
            message = "Email đã được sử dụng")
    private String email;

    @NotBlank(groups = NormalRegister.class, message = "Không để trống")
    @Pattern(regexp = "^(\\+84|0)(3[2-9]|5[6,8,9]|7[0,6-9]|8[1-5]|9[0-9])\\d{7}$"
            ,message = "Sai định dạng"
            ,groups = NormalRegister.class
    )
    @Unique(entityClass = User.class, fieldName = "phone", idField = "id",
            message = "Số điện thoại đã được sử dụng")
    private String phone;
    @Lob
    private String bio;
    private String avatarUrl;
    @Size(max = 50)
    private String firstName;
    @Size(max = 50)
    private String lasttName;
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    @MinAge(16)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    public UserUpdateDto (User user){
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.bio = user.getBio();
        this.avatarUrl = user.getProfilePicture();
        this.firstName = user.getFirstName();
        this.lasttName = user.getLastName();
        this.dateOfBirth = user.getDateOfBirth();
    }

    public User toUser(User user) {
        user.setUsername(this.username);
        user.setEmail(this.email);
        user.setPhone(this.phone);
        user.setBio(this.bio);
        user.setFirstName(this.firstName);
        user.setLastName(this.lasttName);
        user.setDateOfBirth(this.dateOfBirth);
        return user;
    }
}
