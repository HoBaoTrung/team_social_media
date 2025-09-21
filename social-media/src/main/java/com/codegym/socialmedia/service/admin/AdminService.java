package com.codegym.socialmedia.service.admin;

import com.codegym.socialmedia.dto.UserDTO;
import com.codegym.socialmedia.model.account.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface AdminService {
    Page<UserDTO> getAllUsers(int page, int size);
    void blockUser(Long userId);
    Map<String, Long> getVisitStatistics();
    Map<String, Long> getNewUserStatistics();
}


