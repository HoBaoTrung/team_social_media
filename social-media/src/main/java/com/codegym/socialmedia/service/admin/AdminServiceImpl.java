package com.codegym.socialmedia.service.admin;

import com.codegym.socialmedia.dto.user.UserDTO;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.repository.user.IUserRepository;
import com.codegym.socialmedia.repository.TrackingRepository;
import com.codegym.socialmedia.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private TrackingRepository trackingRepository;

    @Autowired
    private UserService userService;

    @Override
    public Page<UserDTO> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = userService.getCurrentUser().getId();
        Page<User> users = userRepository.findByIdNot(currentUserId, pageable);
        return  users.map(UserDTO::new);
    }


    @Override
    public void blockUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setActive(!user.isActive()); // Giả sử có field 'active'
            userRepository.save(user);
        });
    }



    @Override
    public Map<String, Long> getVisitStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("today", trackingRepository.countVisitsOn(LocalDate.now()));
        stats.put("week", trackingRepository.countVisitsFrom(LocalDate.now().minusDays(7)));
        stats.put("month", trackingRepository.countVisitsFrom(LocalDate.now().minusDays(30)));
        return stats;
    }

    @Override
    public Map<String, Long> getNewUserStatistics() {
        Map<String, Long> stats = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(23, 59, 59);

        stats.put("today", userRepository.countByCreatedAtBetween(startOfToday, endOfToday));
        stats.put("week", userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7)));
        stats.put("month", userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(30)));

        return stats;
    }

}
