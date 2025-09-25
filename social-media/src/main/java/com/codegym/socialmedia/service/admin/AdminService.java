package com.codegym.socialmedia.service.admin;

import com.codegym.socialmedia.dto.user.UserDTO;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface AdminService {
    Page<UserDTO> getAllUsers(int page, int size);
    void blockUser(Long userId);
    Map<String, Long> getVisitStatistics();
    Map<String, Long> getNewUserStatistics();
}


