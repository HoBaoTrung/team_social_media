package com.codegym.socialmedia.controller;

import com.codegym.socialmedia.dto.post.FeedResponse;
import com.codegym.socialmedia.dto.post.PostCreateDto;
import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.dto.post.PostUpdateDto;
import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.model.social_action.PostComment;
import com.codegym.socialmedia.service.post.*;
import com.codegym.socialmedia.service.user.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/posts")
public class PostController {
    @Autowired private PostInteractionService postInteractionService;
    @Autowired private PostCommandService postCommandService;
    @Autowired private PostQueryService postQueryService;
    @Autowired private PostFeedService postFeedService;
//    @Autowired
//    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostCommentService commentService;
    // ================== WEB PAGES ==================

    @GetMapping("/user/{username}")
    public String userPosts(@PathVariable String username,
                            Model model,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size) {
        User targetUser = userService.getUserByUsername(username);
        if (targetUser == null) {
            model.addAttribute("error", "Người dùng không tồn tại");
            return "error/404";
        }

        User currentUser = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);

        Page<PostDisplayDto> posts;
        if (currentUser != null) {
            posts = postQueryService.getPostsByUser(targetUser, currentUser, pageable);
        } else {
            posts = postQueryService.getPublicPostsByUser(targetUser, currentUser, pageable);
        }

        model.addAttribute("posts", posts);
        model.addAttribute("targetUser", targetUser);
        model.addAttribute("isOwner", currentUser != null && currentUser.getId().equals(targetUser.getId()));

        if (currentUser != null && currentUser.getId().equals(targetUser.getId())) {
            model.addAttribute("postCreateDto", new PostCreateDto());
            model.addAttribute("privacyLevels", PrivacyLevel.values());
        }

        return "posts/user-posts";
    }

    // ================== CRUD OPERATIONS ==================

    @PostMapping("/create")
    public String createPost(@Valid @ModelAttribute PostCreateDto dto,
                             BindingResult result, @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                             RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin");
            return "redirect:" + (redirectUrl != null ? redirectUrl : "/news-feed");
        }

        try {
            postCommandService.createPost(dto, currentUser);
            redirectAttributes.addFlashAttribute("success", "Đăng bài thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:" + (redirectUrl != null ? redirectUrl : "/news-feed");
    }


    @PostMapping("/update/{id}")
    public String updatePost(@PathVariable Long id,
                             @Valid @ModelAttribute PostUpdateDto dto,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng kiểm tra lại thông tin");
            return "redirect:/posts";
        }

        try {
            dto.setId(id);
            postCommandService.updatePost(id, dto, currentUser);
            redirectAttributes.addFlashAttribute("success", "Cập nhật bài viết thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/posts";
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            postCommandService.deletePost(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Xóa bài viết thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/posts";
    }

    // ================== AJAX API ENDPOINTS ==================
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<?> searchPosts(
            @RequestParam String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "username") String username) {

        User user = userService.getUserByUsername(username);
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<PostDisplayDto> posts = postQueryService.searchUserPosts(user, currentUser, keyword, pageable);

        return ResponseEntity.ok(posts);
    }

    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPostApi(@Valid @ModelAttribute PostCreateDto dto,
                                                             BindingResult result) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        if (result.hasErrors()) {
            response.put("success", false);
            response.put("message", "Dữ liệu không hợp lệ");
            response.put("errors", result.getAllErrors());
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Post post = postCommandService.createPost(dto, currentUser);
            PostDisplayDto postDto = postQueryService.getPostById(post.getId(), currentUser);

            response.put("success", true);
            response.put("message", "Đăng bài thành công!");
            response.put("post", postDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/api/feed")
    @ResponseBody
    public ResponseEntity<?> getNewsFeed(
            @RequestParam(value = "postID", defaultValue = "-1") long postID,
            @RequestParam(value = "commentID", defaultValue = "-1") long commentID,
            @RequestParam(value = "lastScore", required = false) Long lastScore,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        if (commentID != -1) {
            PostComment comment = commentService.getCommentById(commentID).get();
            postID = comment.getPost().getId();
        }

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        if (postID == -1 && commentID == -1) {
            FeedResponse posts = postFeedService.getFeed(currentUser,lastScore,size);
            return ResponseEntity.ok(posts);
        }

        PostDisplayDto postDto = postQueryService.getPostById(postID, currentUser);
        return ResponseEntity.ok(new FeedResponse(List.of(postDto), 0L));
    }

    @GetMapping("/api/user/{username}")
    @ResponseBody
    public ResponseEntity<?> getUserPosts(
            @PathVariable String username,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        User targetUser = userService.getUserByUsername(username);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = userService.getCurrentUser();


        Pageable pageable = PageRequest.of(page, size);

        Page<PostDisplayDto> posts;
        if (currentUser != null) {
            posts = postQueryService.getPostsByUser(targetUser, currentUser, pageable);
        } else {
            posts = postQueryService.getPublicPostsByUser(targetUser, currentUser, pageable);
        }
        return ResponseEntity.ok(new FeedResponse(posts.getContent(), 0L));
//        return ResponseEntity.ok(posts);

    }

    @GetMapping("/api/user/{username}/photos")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getUserPhotos(@PathVariable String username) {
        User targetUser = userService.getUserByUsername(username);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = userService.getCurrentUser();

        try {
            // Get all posts with images
            Page<PostDisplayDto> posts;
            if (currentUser != null) {
                posts = postQueryService.getPostsByUser(targetUser, currentUser, PageRequest.of(0, 100));
            } else {
                posts = postQueryService.getPublicPostsByUser(targetUser, currentUser, PageRequest.of(0, 100));
            }

            // Extract all images from posts
            List<Map<String, String>> photos = new ArrayList<>();
            posts.getContent().forEach(post -> {
                if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
                    post.getImageUrls().forEach(imageUrl -> {
                        Map<String, String> photo = new HashMap<>();
                        photo.put("url", imageUrl);
                        photo.put("postId", post.getId().toString());
                        photos.add(photo);
                    });
                }
            });

            return ResponseEntity.ok(photos);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/api/like/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean isLiked = postInteractionService.toggleLike(id, currentUser);

            response.put("success", true);
            response.put("isLiked", isLiked);
            response.put("message", isLiked ? "Đã thích" : "Đã bỏ thích");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<PostDisplayDto> getPost(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();

        try {
            PostDisplayDto post = postQueryService.getPostById(id, currentUser);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/api/update/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePostApi(@PathVariable Long id,
                                                             @Valid @ModelAttribute PostUpdateDto dto,
                                                             BindingResult result) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        if (result.hasErrors()) {
            response.put("success", false);
            response.put("message", "Dữ liệu không hợp lệ");
            response.put("errors", result.getAllErrors());
            return ResponseEntity.badRequest().body(response);
        }

        try {
            dto.setId(id);
            Post post = postCommandService.updatePost(id, dto, currentUser);
            PostDisplayDto postDto = postQueryService.getPostById(post.getId(), currentUser);

            response.put("success", true);
            response.put("message", "Cập nhật bài viết thành công!");
            response.put("post", postDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePostApi(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        try {
            postCommandService.deletePost(id, currentUser);
            response.put("success", true);
            response.put("message", "Xóa bài viết thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

}