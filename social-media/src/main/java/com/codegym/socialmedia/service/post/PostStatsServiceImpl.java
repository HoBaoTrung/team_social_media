package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.repository.post.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostStatsServiceImpl implements PostStatService{
    @Autowired
    private PostRepository postRepository;

    public long countUserPosts(User user) {
        // Sửa từ countByUser thành countByUserAndIsDeletedFalse
        return postRepository.countByUserAndIsDeletedFalse(user);
    }

    @Override
    public List<String> getPhotosForProfile(User profileOwner, User viewer) {
        if (viewer == null) {
            return postRepository.findPublicPhotos(profileOwner);
        }
        return postRepository.findVisiblePhotos(profileOwner, viewer);
    }
}