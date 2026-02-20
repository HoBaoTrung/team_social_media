package com.codegym.socialmedia.controller;

import com.codegym.socialmedia.component.CommentAssembler;
import com.codegym.socialmedia.dto.comment.CommentRequest;
import com.codegym.socialmedia.dto.comment.DisplayCommentDTO;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.PostComment;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import com.codegym.socialmedia.service.post.PostCommentService;
import com.codegym.socialmedia.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    @Autowired
    private final PostCommentService postCommentService;
    @Autowired
    private final UserService userService;


    @Autowired
    private CommentAssembler commentAssembler;

    @PostMapping("/comments")
    public DisplayCommentDTO addComment(@RequestBody CommentRequest req) {
        PostComment saved = postCommentService.addComment(req.getPostId(), userService.getCurrentUser(), req.getContent(), req.getMentionedUserIds());

        DisplayCommentDTO newComment = commentAssembler.mapToDTO(saved, userService.getCurrentUser());
        newComment.setCanEdit(true);
        newComment.setCanDeleted(true);
        newComment.setCanReply(true);
        return newComment;
    }


    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<DisplayCommentDTO>> getCommentsByPost(@PathVariable Long postId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        Page<DisplayCommentDTO> commentsPage = postCommentService.getCommentsByPost(postId, userService.getCurrentUser(), page, size);
        List<DisplayCommentDTO> dtos = commentsPage.getContent().stream().toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/comments/{id}")
    public DisplayCommentDTO getComment(@PathVariable Long id) {
        PostComment comment = postCommentService.getCommentById(id).orElse(null);
        return commentAssembler.mapToDTO(comment, userService.getCurrentUser());
    }

    @PutMapping("/comments/{id}")
    public DisplayCommentDTO editComment(@RequestBody CommentRequest req, @PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        PostComment updated = postCommentService.updateComment(id, currentUser, req.getContent(),req.getMentionedUserIds());
        return commentAssembler.mapToDTO(updated, currentUser); // trả về DTO với quyền
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();

        PostComment deletedComment = postCommentService.deleteComment(id, currentUser);
        if (deletedComment != null) {
            // Trả về DTO
            DisplayCommentDTO dto =  commentAssembler.mapToDTO(deletedComment,currentUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "deletedComment", dto
            ));
        } else {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Bạn không có quyền xóa bình luận này"
            ));
        }
    }

    @PostMapping("/comments/like/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleLikeComment(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean isLikeComment = postCommentService.toggleLikeComment(id, currentUser);

            response.put("success", true);
            response.put("isLiked", isLikeComment);
            response.put("message", isLikeComment ? "Đã thích" : "Đã bỏ thích");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<?> replyToComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequest req) {
        String content = req.getContent();
        User currentUser = userService.getCurrentUser();

        DisplayCommentDTO dto = postCommentService.replyToComment(commentId, currentUser, content,req.getMentionedUserIds());
        dto.setCanEdit(true);
        dto.setCanDeleted(true);

        return ResponseEntity.ok(Map.of("reply", dto));
    }


}


