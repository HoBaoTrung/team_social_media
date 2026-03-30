package com.codegym.socialmedia.controller;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.service.user.UserService;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/firebase")
public class FirebaseController {

    @Autowired
    UserService userService;

    // API này phải:
    //Require JWT (Spring Security)
    //Không public
    @PostMapping("/token")
    public Map<String, String> getFirebaseToken() throws Exception {
        User user = userService.getCurrentUser();
        String uid = user.getId().toString(); // dùng ID user hệ thống bạn

        String customToken = FirebaseAuth.getInstance()
                .createCustomToken(uid);

        return Map.of("token", customToken);
    }
}