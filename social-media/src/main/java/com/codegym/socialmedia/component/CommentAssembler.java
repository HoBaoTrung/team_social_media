package com.codegym.socialmedia.component;

import com.codegym.socialmedia.component.privacy.PrivacyPolicyResolver;
import com.codegym.socialmedia.dto.comment.DisplayCommentDTO;
import com.codegym.socialmedia.dto.comment.MentionDTO;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.model.social_action.PostComment;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CommentAssembler {

    private final FriendshipService friendshipService;
    private final PrivacyPolicyResolver privacyPolicyResolver;
    record CommentFrame(PostComment comment, int depth, DisplayCommentDTO parentDto) {}
    /**
     * Convert một comment thành DTO với depth control (1 gốc và tối đa 3 cấp hậu duệ).
     */
    public DisplayCommentDTO mapToDTO(PostComment root,
                                             User currentUser
                                             ) {
        DisplayCommentDTO rootDto =
                buildDtoBase(root, currentUser);
        int maxDepth = 2;
        rootDto.setComment(renderContent(root.getContent(), root.getMentionedUsers()));
        rootDto.setReplies(new ArrayList<>());

        Deque<CommentFrame> stack = new ArrayDeque<>();
        stack.push(new CommentFrame(root, 1, rootDto));
        while (!stack.isEmpty()) {
            CommentFrame frame = stack.pop();
            PostComment current = frame.comment();
            int depth = frame.depth();
            DisplayCommentDTO parentDto = frame.parentDto();

            List<PostComment> replies = current.getReplies();
            if (replies == null || replies.isEmpty()) continue;

            List<PostComment> sorted = sortComments(replies);

            if (depth < maxDepth) {
                List<DisplayCommentDTO> children = new ArrayList<>();
                parentDto.setReplies(children);

                for (PostComment child : sorted) {
                    DisplayCommentDTO childDto =
                            buildDtoBase(child, currentUser);
                    childDto.setComment(renderContent(
                            child.getContent(), child.getMentionedUsers()));
                    childDto.setReplies(new ArrayList<>());
                    children.add(childDto);

                    stack.push(new CommentFrame(child, depth + 1, childDto));
                }
            } else {
                // flatten
                List<DisplayCommentDTO> flat = new ArrayList<>();
                collectDescendantsFlat(current, flat, currentUser);
                parentDto.setReplies(flat.isEmpty() ? null : flat);
            }
        }

        return rootDto;
//        return getComment(root, currentUser, friendshipService, 3);
    }

//    private DisplayCommentDTO getComment(PostComment comment,
//                                                User currentUser,
//                                                int depth) {
//        // Build base DTO
//        DisplayCommentDTO dto = buildDtoBase(comment, currentUser);
//
//        // Render content sau khi build base
//        dto.setComment(renderContent(comment.getContent(), comment.getMentionedUsers()));
//
//        // Xử lý replies
//        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
//            if (depth > 1) {
//                List<DisplayCommentDTO> replies = new ArrayList<>();
//                for (PostComment reply : sortComments(comment.getReplies())) {
//                    DisplayCommentDTO replyDTO =
//                            getComment(reply, currentUser, depth - 1);
//                    replies.add(replyDTO);
//                }
//                dto.setReplies(replies);
//            } else {
//                List<DisplayCommentDTO> flatDescendants = new ArrayList<>();
//                collectDescendantsFlat(comment, flatDescendants, currentUser);
//                dto.setReplies(flatDescendants.isEmpty() ? null : flatDescendants);
//            }
//        } else {
//            dto.setReplies(null);
//        }
//
//        return dto;
//    }

    /**
     * Build dữ liệu cơ bản cho DTO (không set replies, không render content).
     */
    private DisplayCommentDTO buildDtoBase(PostComment comment,
                                                  User currentUser
                                                 ) {
        DisplayCommentDTO dto = new DisplayCommentDTO();
        Post p = comment.getPost();

        // Quyền reply
        boolean canReply;
        if (currentUser != null && currentUser.isAdmin()) {
            canReply = true;
        } else {
            boolean isFriend = false;
            if (currentUser != null) {
                Friendship.FriendshipStatus friendshipStatus =
                        friendshipService.getFriendshipStatus(p.getUser(), currentUser);
                isFriend = (friendshipStatus == Friendship.FriendshipStatus.ACCEPTED);
            }
             canReply = currentUser != null && (
                    currentUser.isAdmin() ||
                            privacyPolicyResolver.canView(
                                    currentUser,
                                    p.getUser(),
                                    p.getPrivacyCommentLevel(),
                                    isFriend
                            )
            );
        }
        dto.setCanReply(canReply);

        // Base info
        dto.setCommentId(comment.getId());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setUserFullName(comment.getUser().getFullName());
        dto.setUsername(comment.getUser().getUsername());
        dto.setUserAvatarUrl(comment.getUser().getProfilePicture());

        dto.setCanEdit(currentUser != null && comment.getUser().getId().equals(currentUser.getId()));
        dto.setCanDeleted(currentUser != null && comment.getUser().getId().equals(currentUser.getId()));
        dto.setParentCommentId(comment.getParent() != null ? comment.getParent().getId() : null);

        // Like
        int likeCount = comment.getLikedByUsers() != null ? comment.getLikedByUsers().size() : 0;
        dto.setLikeCount(likeCount);

        boolean likedByCurrentUser = currentUser != null && comment.getLikedByUsers() != null &&
                comment.getLikedByUsers().stream()
                        .anyMatch(like -> like.getUser().getId().equals(currentUser.getId()));
        dto.setLikedByCurrentUser(likedByCurrentUser);

        // Mentions
        List<User> mentions = comment.getMentionedUsers();
        dto.setMentions(mentions.stream()
                .map(u -> new MentionDTO(u.getId(), u.getUsername(), u.getFullName()))
                .toList());

        return dto;
    }

    private void collectDescendantsFlat(PostComment root,
                                               List<DisplayCommentDTO> collector,
                                               User currentUser
                                               ) {
        if (root.getReplies() == null || root.getReplies().isEmpty()) return;

        for (PostComment child : sortComments(root.getReplies())) {
            DisplayCommentDTO leaf = buildDtoBase(child, currentUser);
            leaf.setComment(renderContent(child.getContent(), child.getMentionedUsers()));
            leaf.setReplies(null);
            collector.add(leaf);
            collectDescendantsFlat(child, collector, currentUser);
        }
    }

    private static List<PostComment> sortComments(Collection<PostComment> comments) {
        return comments.stream()
                .sorted(Comparator.comparing(PostComment::getCreatedAt).reversed())
                .toList();
    }

    /**
     * Render nội dung comment với mentions.
     */
    private static String renderContent(String content, List<User> mentions) {
        if (content == null || content.isEmpty()) return "";

        StringBuilder out = new StringBuilder();
        int idx = 0;
        int len = content.length();

        // Sắp xếp mentions theo tên dài trước
        List<User> sortedMentions = new ArrayList<>(mentions != null ? mentions : Collections.emptyList());
        sortedMentions.sort(Comparator.comparingInt(
                u -> -((u.getFirstName() == null ? 0 : u.getFirstName().length())
                        + (u.getLastName() == null ? 0 : u.getLastName().length()) + 1))
        );

        while (idx < len) {
            int at = content.indexOf('@', idx);
            if (at == -1) {
                out.append(HtmlUtils.htmlEscape(content.substring(idx)));
                break;
            }
            if (at > idx) {
                out.append(HtmlUtils.htmlEscape(content.substring(idx, at)));
            }

            boolean okBoundaryBefore = (at == 0) || Character.isWhitespace(content.charAt(at - 1));
            if (!okBoundaryBefore) {
                out.append(HtmlUtils.htmlEscape("@"));
                idx = at + 1;
                continue;
            }

            boolean matched = false;
            for (User u : sortedMentions) {
                String first = u.getFirstName() == null ? "" : u.getFirstName().trim();
                String last = u.getLastName() == null ? "" : u.getLastName().trim();
                if (first.isEmpty() && last.isEmpty()) continue;

                String fullName = (first + (last.isEmpty() ? "" : " " + last)).trim();
                if (fullName.isEmpty()) continue;

                int nameLen = fullName.length();
                int endPos = at + 1 + nameLen;

                if (endPos <= len) {
                    String candidate = content.substring(at + 1, endPos);
                    if (candidate.equalsIgnoreCase(fullName)) {
                        if (endPos == len || !Character.isLetterOrDigit(content.charAt(endPos))) {
                            String anchor = "<a class=\"mention\""
                                    + " href=\"/profile/" + HtmlUtils.htmlEscape(u.getUsername()) + "\""
                                    + " data-username=\"" + HtmlUtils.htmlEscape(u.getUsername()) + "\""
                                    + " aria-label=\"mention " + HtmlUtils.htmlEscape(fullName) + "\">"
                                    + HtmlUtils.htmlEscape("@" + fullName)
                                    + "</a>";
                            out.append(anchor);
                            idx = endPos;
                            matched = true;
                            break;
                        }
                    }
                }
            }

            if (!matched) {
                out.append(HtmlUtils.htmlEscape("@"));
                idx = at + 1;
            }
        }

        return out.toString();
    }
}
