package com.codegym.socialmedia.component;

import com.codegym.socialmedia.component.privacy.PrivacyPolicyResolver;
import com.codegym.socialmedia.dto.comment.DisplayCommentDTO;
import com.codegym.socialmedia.dto.comment.MentionDTO;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.model.social_action.PostComment;
import com.codegym.socialmedia.repository.comment.LikeCommentRepository;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CommentAssembler {
    private final LikeCommentRepository likeCommentRepository;
    private final FriendshipService friendshipService;
    private final PrivacyPolicyResolver privacyPolicyResolver;
    record CommentFrame(PostComment comment, int depth, DisplayCommentDTO parentDto) {}
    /**
     * Convert một comment thành DTO với depth control (1 gốc và tối đa 3 cấp hậu duệ).
     */
    public DisplayCommentDTO mapToDTO(PostComment root, User currentUser) {
        DisplayCommentDTO rootDto = buildDtoBase(root, currentUser);
        int maxDepth = 2;
        rootDto.setComment(renderContent(root.getContent(), root.getMentionedUsers()));
        rootDto.setReplies(new ArrayList<>());

        traverseComments(root, currentUser, maxDepth, rootDto);
        return rootDto;
    }

    private void traverseComments(
            PostComment root,
            User currentUser,
            int maxDepth,
            DisplayCommentDTO rootDto
    ) {
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

                //  Add vào children theo đúng thứ tự sorted
                for (PostComment child : sorted) {
                    DisplayCommentDTO childDto = buildDtoBase(child, currentUser);
                    childDto.setComment(renderContent(child.getContent(), child.getMentionedUsers()));
                    childDto.setReplies(new ArrayList<>());
                    children.add(childDto);
                }

                //  Push vào stack theo thứ tự ngược để DFS đúng
                for (int i = sorted.size() - 1; i >= 0; i--) {
                    PostComment child = sorted.get(i);
                    DisplayCommentDTO childDto = children.get(i);
                    stack.push(new CommentFrame(child, depth + 1, childDto));
                }


            } else {
                List<DisplayCommentDTO> flat = new ArrayList<>();
                collectDescendantsFlat(current, flat, currentUser);
                parentDto.setReplies(flat.isEmpty() ? null : flat);
            }
        }
    }


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
                                    currentUser.getId(),
                                    p.getUser().getId(),
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
        int likeCount = likeCommentRepository.countByComment(comment);
        dto.setLikeCount(likeCount);

        boolean likedByCurrentUser = likeCommentRepository.existsByCommentAndUser(comment,currentUser);
        dto.setLikedByCurrentUser(likedByCurrentUser);

        // Mentions
        List<User> mentions = comment.getMentionedUsers();
        dto.setMentions(mentions.stream()
                .map(u -> new MentionDTO(u.getId(), u.getUsername(), u.getFullName()))
                .toList());

        return dto;
    }

    private void collectDescendantsFlat(
            PostComment root,
            List<DisplayCommentDTO> collector,
            User currentUser
    ) {
        Deque<PostComment> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            PostComment current = stack.pop();

            List<PostComment> replies = current.getReplies();
            if (replies == null || replies.isEmpty()) continue;

            List<PostComment> sorted = sortComments(replies);

            // push lại để giữ thứ tự
            for (int i = 0; i < sorted.size() ; i++) {
                PostComment child = sorted.get(i);

                DisplayCommentDTO leaf = buildDtoBase(child, currentUser);
                leaf.setComment(renderContent(child.getContent(), child.getMentionedUsers()));
                leaf.setReplies(null);
                collector.add(leaf);

                stack.push(child);
            }
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

        int len = content.length();
        StringBuilder out = new StringBuilder(len);

        // ===== 1. Build lookup structures =====
        Map<String, User> mentionMap = new HashMap<>();
        Set<Integer> nameLengths = new HashSet<>();

        if (mentions != null) {
            for (User u : mentions) {
                String first = u.getFirstName() == null ? "" : u.getFirstName().trim();
                String last = u.getLastName() == null ? "" : u.getLastName().trim();
                String fullName = (first + (last.isEmpty() ? "" : " " + last)).trim();

                if (!fullName.isEmpty()) {
                    String key = fullName.toLowerCase();
                    mentionMap.put(key, u);
                    nameLengths.add(fullName.length());
                }
            }
        }

        // Sort độ dài giảm dần để tránh prefix bug
        List<Integer> sortedLengths = new ArrayList<>(nameLengths);
        sortedLengths.sort(Collections.reverseOrder());

        // ===== 2. Parse content =====
        int idx = 0;

        while (idx < len) {
            int at = content.indexOf('@', idx);

            if (at == -1) {
                out.append(HtmlUtils.htmlEscape(content.substring(idx)));
                break;
            }

            // append phần trước @
            if (at > idx) {
                out.append(HtmlUtils.htmlEscape(content.substring(idx, at)));
            }

            // check boundary trước @
            boolean okBoundaryBefore =
                    (at == 0) || Character.isWhitespace(content.charAt(at - 1));

            if (!okBoundaryBefore) {
                out.append(HtmlUtils.htmlEscape("@"));
                idx = at + 1;
                continue;
            }

            boolean matched = false;

            // ===== 3. Try match by name length =====
            for (int nameLen : sortedLengths) {
                int endPos = at + 1 + nameLen;

                if (endPos > len) continue;

                String candidate = content.substring(at + 1, endPos);
                User user = mentionMap.get(candidate.toLowerCase());

                if (user != null) {
                    // check boundary sau
                    if (endPos == len ||
                            !Character.isLetterOrDigit(content.charAt(endPos))) {

                        String fullName = candidate;

                        String escapedUsername = HtmlUtils.htmlEscape(user.getUsername());
                        String escapedFullName = HtmlUtils.htmlEscape(fullName);

                        String anchor = "<a class=\"mention\""
                                + " href=\"/profile/" + escapedUsername + "\""
                                + " data-username=\"" + escapedUsername + "\""
                                + " aria-label=\"mention " + escapedFullName + "\">"
                                + "@" + escapedFullName
                                + "</a>";

                        out.append(anchor);
                        idx = endPos;
                        matched = true;
                        break;
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
