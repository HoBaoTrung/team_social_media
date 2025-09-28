import {formatTimeAgo} from './timeUtils.js';

// Posts JavaScript
class PostManager {
    constructor() {
        this.currentPage = 0;
        this.isLoading = false;
        this.hasMorePosts = true;
        this.selectedImages = [];
        this.editingPostId = null;
        this.imagesToDelete = [];
        this.form = document.getElementById("create-post-form");
        this.submitBtn = document.getElementById("post-submit-btn");
        const container = document.getElementById('posts-container');
        this.username = container.getAttribute('data-username');
        this.mentionScrollListener = null;
        this.mentionScrollContainer = null;
        this.init();
    }

    init() {
        if (this.form != null && this.form != undefined) {
            this.form.addEventListener("submit", (e) => {
                this.handleSubmit();
            });
        }
        this.setupEventListeners();
        this.loadInitialPosts();
        this.setupInfiniteScroll();
    }

    cleanupMentionListeners() {
        if (this.mentionScrollListener) {
            this.mentionScrollContainer.removeEventListener('scroll', this.mentionScrollListener);
            window.removeEventListener('resize', this.mentionScrollListener);
            this.mentionScrollListener = null;
            this.mentionScrollContainer = null;
        }
    }

    handleSubmit() {
        // Disable nút
        this.submitBtn.disabled = true;
        this.submitBtn.innerText = "Đang đăng...";
        this.form.querySelectorAll("button").forEach(el => {
            if (el !== this.submitBtn) {
                el.disabled = true;
            }
        });
        this.form.querySelectorAll("input, textarea").forEach(el => {
            el.readOnly = true; // thay vì el.disabled = true
        });

    }

    setupEventListeners() {

        // Content input validation
        const contentInput = document.querySelector('.post-content-input');
        if (contentInput) {
            contentInput.addEventListener('input', () => this.validatePostForm());
        }

        // Image input
        const imageInput = document.getElementById('image-input');
        if (imageInput) {
            imageInput.addEventListener('change', (e) => this.handleImageSelection(e));
        }

        // Edit post form
        const editForm = document.getElementById('edit-post-form');
        if (editForm) {
            editForm.addEventListener('submit', (e) => this.handleEditPost(e));

        }

        // Edit modal events
        const editModal = document.getElementById('editPostModal');
        if (editModal) {
            editModal.addEventListener('hidden.bs.modal', () => this.resetEditForm());
        }

    }

    validatePostForm() {
        const content = document.querySelector('.post-content-input').value.trim();
        // const submitBtn = document.getElementById('post-submit-btn');

        if (content.length > 0 || this.selectedImages.length > 0) {
            this.submitBtn.disabled = false;
        } else {
            this.submitBtn.disabled = true;
        }

    }

    handleImageSelection(event) {
        const files = Array.from(event.target.files);

        // Gộp file mới vào selectedImages
        this.selectedImages = this.selectedImages.concat(files);

        // Update lại input file duy nhất bằng DataTransfer
        const dt = new DataTransfer();
        this.selectedImages.forEach(file => dt.items.add(file));
        event.target.files = dt.files;

        // Hiển thị preview
        this.displayImagePreview(this.selectedImages);
        this.validatePostForm();

    }

    displayImagePreview(files, containerId = 'image-preview-container', listId = 'image-preview-list') {
        const container = document.getElementById(containerId);
        const list = document.getElementById(listId);

        if (!files || files.length === 0) {
            container.style.display = 'none';
            list.innerHTML = '';
            return;
        }

        container.style.display = 'block';
        list.innerHTML = '';

        files.forEach((file, index) => {
            const reader = new FileReader();
            reader.onload = (e) => {
                const item = document.createElement('div');
                item.className = 'image-preview-item';

                if (file.type.startsWith("video/")) {
                    // preview video
                    item.innerHTML = `
                    <video src="${e.target.result}" controls class="preview-media"></video>
                    <button type="button" class="image-preview-remove" onclick="postManager.removeSelectedImage(${index})">
                        <i class="fas fa-times"></i>
                    </button>
                `;
                } else {
                    // preview ảnh
                    item.innerHTML = `
                    <img src="${e.target.result}" alt="Preview" class="preview-media">
                    <button type="button" class="image-preview-remove" onclick="postManager.removeSelectedImage(${index})">
                        <i class="fas fa-times"></i>
                    </button>
                `;
                }

                list.appendChild(item);
            };
            reader.readAsDataURL(file);
        });
    }

    removeSelectedImage(index) {
        this.selectedImages.splice(index, 1);
        this.displayImagePreview(this.selectedImages);
        this.validatePostForm();

        // Update file input
        const imageInput = document.getElementById('image-input');
        const dt = new DataTransfer();
        this.selectedImages.forEach(file => dt.items.add(file));
        imageInput.files = dt.files;
    }

    resetCreateForm() {
        const form = document.getElementById('create-post-form');
        form.reset();
        this.selectedImages = [];
        document.getElementById('image-preview-container').style.display = 'none';
        this.validatePostForm();
    }

    async loadInitialPosts() {
        this.currentPage = 0;
        this.hasMorePosts = true;
        const postsContainer = document.getElementById('posts-container');
        postsContainer.innerHTML = '';

        await this.loadPosts();
    }

    async loadPosts() {

        if (this.isLoading || !this.hasMorePosts || this.hasError) return;

        this.isLoading = true;
        this.showLoading(true);

        let controllerURL = `/posts/api/feed?page=${this.currentPage}&size=10`;
        const urlParams = new URLSearchParams(window.location.search);
        controllerURL += '&postID=' + (urlParams.get('postID') ? parseInt(urlParams.get('postID')) : -1);

        controllerURL += '&commentID=' + (urlParams.get('commentID') ? parseInt(urlParams.get('commentID')) : -1);

        if (this.username != null && this.username != undefined && this.username.trim() != '') {
            controllerURL = `/posts/api/user/${this.username}?page=${this.currentPage}&size=10`;
        }

        try {
            const data = await $.ajax({
                url: controllerURL,
                method: "GET",
                dataType: "json",
                xhrFields: {
                    withCredentials: true
                }
            });

            if ('pageable' in data && Array.isArray(data.content)) {
                if (data.content && data.content.length > 0) {
                    data.content.forEach(post => {
                        this.appendPost(post);
                    });

                    this.currentPage++;
                    this.hasMorePosts = !data.last;
                } else {
                    this.hasMorePosts = false;
                    if (this.currentPage === 0) {
                        const noPostsEl = document.getElementById('profile-no-posts');
                        if (noPostsEl) {
                            noPostsEl.style.display = 'block';
                        }
                    }
                    this.showNoMorePosts();
                }
            } else {
                this.appendPost(data);
                this.hasMorePosts = false;
                requestAnimationFrame(() => {
                    this.goToComment(urlParams.get('commentID'), data.id);
                });
            }


        } catch (error) {
            console.error('Error loading posts:', error);

            // Hiển thị thông báo lỗi chi tiết hơn
            const errorMessage = error.responseJSON?.message || error.statusText || 'Không thể tải bài viết';
            this.showNotification(errorMessage, 'error');

            this.hasError = true; // Ngăn không cho load tiếp khi có lỗi
        } finally {
            this.isLoading = false;
            this.showLoading(false);
        }
    }

    showNoMorePosts() {
        const noMorePosts = document.getElementById('no-more-posts');
        if (noMorePosts) {
            noMorePosts.style.display = 'block';
        }
    }

    setupInfiniteScroll() {
        window.addEventListener('scroll', () => {
            if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight - 100) {
                this.loadPosts();
            }
        });
    }

    appendPost(post) {
        const postsContainer = document.getElementById('posts-container');
        const postElement = this.createPostElement(post);
        postsContainer.insertAdjacentHTML('beforeend', postElement);
        // Đăng ký cho like bài viết
        this.subscribeToPostLikes(post.id);

        // Đăng ký cho comment mới của bài viết
        this.subscribeToPostComments(post.id);
    }


    createPostElement(post) {

        const privacyIcons = {
            'PUBLIC': '🌍',
            'FRIENDS': '👥',
            'PRIVATE': '🔒'
        };
        const imagesHtml = this.createImagesHtml(post.imageUrls);
        const timeAgo = formatTimeAgo(post.createdAt);

        return `
            <div class="post-item" data-post-id="${post.id}">
                <div class="post-header">
                    <img src="${post.userAvatarUrl || '/images/default-avatar.jpg'}" 
                         alt="Avatar" class="post-avatar">
                    <div class="post-user-info">
                        <a href="/profile/${post.username}" class="post-username">
                            ${post.userFullName}
                        </a>
                        <div class="post-meta">
                            <span class="post-time">${timeAgo}</span>
                            <span class="post-privacy">
                                <span class="privacy-icon">${privacyIcons[post.privacyLevel]}</span>
                                ${post.privacyLevel === 'PUBLIC' ? 'Công khai' :
            post.privacyLevel === 'FRIENDS' ? 'Bạn bè' : 'Chỉ mình tôi'}
                            </span>
                        </div>
                    </div>
                    ${post.canEdit || post.canDelete ? `
                       <!-- Dropdown -->
                      <div class="dropdown" >
                            <button class="btn btn-light btn-sm" type="button" 
                                    data-bs-toggle="dropdown" aria-expanded="false">
                                <i class="fas fa-ellipsis-h"></i>
                            </button>
                            <ul class="dropdown-menu dropdown-menu-end">
                                <li>
                                    ${post.canEdit ? `<button class="dropdown-item" onclick="postManager.editPost(${post.id})">
                                        <i class="fas fa-edit"></i> Chỉnh sửa
                                    </button>` : ''}
                                </li>
                                <li>
                                    ${post.canDelete ? `<button class="dropdown-item text-danger" onclick="postManager.deletePost(${post.id})">
                                        <i class="fas fa-trash"></i> Xóa
                                    </button>` : ''}
                                </li>
                            </ul>
                      </div>
                    ` : ''}
                </div>
                
                <div class="post-content">${post.content}</div>
                
                ${imagesHtml}
                
                    <div class="post-stats">
                        <div class="post-likes">
                                <div class="post-likes-icon">
                                    <i class="fas fa-heart"></i>
                                </div>
                                <span>${post.likesCount} lượt thích</span>
                           
                        </div>
                        <div>
                                <span class="post-comments-count"
                                 style="cursor:pointer;" 
      onclick="postManager.toggleComments(${post.id}, true)">${post.commentsCount} bình luận</span>
                        </div>
                    </div>
              
                
                <div class="post-actions">
                    <button class="post-action ${post.likedByCurrentUser ? 'liked' : ''}" 
                            onclick="postManager.toggleLike(${post.id},'post')">
                        <i class="fas fa-heart"></i>
                        <span>Thích</span>
                    </button>
                    ${post.canComment ? `   <button class="post-action" onclick="postManager.toggleComments(${post.id})">
                        <i class="fas fa-comment"></i>
                        <span>Bình luận</span>
                    </button>` : ''}
                 
                    
                    <button class="post-action">
                        <i class="fas fa-share"></i>
                        <span>Chia sẻ</span>
                    </button>
                </div>
                
                <div class="post-comments" id="comments-${post.id}" style="display: none;">
                 ${post.canComment ? `<div class="comment-form">
                        <img src="${this.getCurrentUserAvatar()}" alt="Avatar" class="comment-avatar">
                        <div class="comment-input-container" style="position: relative">
                            <div contenteditable="true" class="comment-input" data-placeholder="Viết bình luận..." 
                                 onkeypress="postManager.handleCommentKeyPress(event, ${post.id})"
                                 onkeydown="postManager.handleMentionKeyDown(event, ${post.id}, 'main-${post.id}')"

                                 oninput="postManager.showMentionSuggestions(${post.id}, this, 'main-${post.id}')"></div>
                            <ul id="mentions-dropdown-main-${post.id}" class="mentions-dropdown"></ul>
                             <button class="comment-submit" onclick="postManager.submitComment(${post.id})">
                                <i class="fas fa-paper-plane"></i>
                            </button>
                         </div>
                    </div>` : ''}
                    
                    
                    <div class="comments-list" id="comments-list-${post.id}" style="max-height:300px; overflow-y:auto;">
                        <!-- Comments will be loaded here -->
                    </div>
                </div>
            </div>
        `;
    }

    createImagesHtml(imageUrls) {
        if (!imageUrls || imageUrls.length === 0) {
            return '';
        }

        // Helper: check media type
        const isVideo = (url) => {
            const lower = url.toLowerCase();
            return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".ogg");
        };

        if (imageUrls.length === 1) {
            const mediaUrl = imageUrls[0];
            return `
            <div class="post-images">
                <div class="post-images-single">
                    ${isVideo(mediaUrl)
                ? `<video controls class="post-media" loading="lazy">
                           <source src="${mediaUrl}" type="video/${mediaUrl.split('.').pop()}">
                           Trình duyệt của bạn không hỗ trợ video.
                       </video>`
                : `<img src="${mediaUrl}" alt="Post image" class="post-media" loading="lazy" onclick="postManager.viewImage('${mediaUrl}')">`}
                </div>
            </div>
        `;
        }

        let gridClass = `grid-${Math.min(imageUrls.length, 4)}`;
        let html = `<div class="post-images"><div class="post-images-grid ${gridClass}">`;

        const displayCount = Math.min(imageUrls.length, 4);
        for (let i = 0; i < displayCount; i++) {
            const isLast = i === displayCount - 1 && imageUrls.length > 4;
            const extraCount = imageUrls.length - 4;
            const mediaUrl = imageUrls[i];

            html += `
            <div class="post-image-item ${isLast ? 'post-images-more' : ''}" 
                 ${isLast ? `data-count="${extraCount}"` : ''} 
                 ${!isVideo(mediaUrl)
                ? `onclick="postManager.viewImages(${JSON.stringify(imageUrls).replace(/"/g, '&quot;')}, ${i})"`
                : ''}>
                ${isVideo(mediaUrl)
                ? `<video controls class="post-media" loading="lazy">
                           <source src="${mediaUrl}" type="video/${mediaUrl.split('.').pop()}">
                           Trình duyệt của bạn không hỗ trợ video.
                       </video>`
                : `<img src="${mediaUrl}" alt="Post image" class="post-media" loading="lazy">`}
            </div>
        `;
        }

        html += '</div></div>';
        return html;
    }

    async toggleLike(targetId, type = 'post') {
        let url = "";
        if (type === 'post') url = `/posts/api/like/${targetId}`;
        else if (type === 'comment') url = `/api/comments/like/${targetId}`;

        try {
            const res = await fetch(url, {
                method: "POST",
                headers: {"Content-Type": "application/json"}
            });

            if (!res.ok) {
                const text = await res.text();
                throw new Error("Không thể like/unlike: " + text);
            }

            const data = await res.json();

            if (data.success) {
                // ✅ update UI ngay lập tức
                this.updateLikeButton(targetId, data.isLiked, type);

            } else {
                this.showNotification(data.message || 'Có lỗi xảy ra', 'error');
            }

        } catch (err) {
            console.error(err);
            alert(err.message || "Có lỗi xảy ra khi like/unlike");
        }
    }

    updateLikeCount(id, count, type) {
        let countSpan;
        if (type === 'comment') {
            countSpan = document.querySelector(`#comment-like-${id} span`);
        }

        if (countSpan) {
            countSpan.textContent = count; // chỉ hiển thị số
        }
    }


    // cập nhật class liked của nút like
    updateLikeButton(id, isLiked, like_type) {
        console.log(isLiked)
        let likeBtn;
        if (like_type == 'post') likeBtn = document.querySelector(
            `.post-item[data-post-id="${id}"] .post-action`
        );
        if (like_type == 'comment') likeBtn = document.querySelector(
            `#comment-like-${id}`
        );

        if (likeBtn) {
            if (isLiked) {
                likeBtn.classList.add("liked");
            } else {
                likeBtn.classList.remove("liked");
            }
        }
    }

    // subscribe cập nhật realtime likes qua websocket
    subscribeToPostLikes(postId) {
        stompClient.subscribe(`/topic/status/${postId}/likes`, (message) => {
            const data = JSON.parse(message.body);

            // update số lượng like
            const likesSpan = document.querySelector(
                `.post-item[data-post-id="${postId}"] .post-likes span`
            );

            if (likesSpan) {
                likesSpan.textContent = data.likeCount + " lượt thích";
            }

            // update nút like (cho user khác nhìn thấy realtime)
            this.updateLikeButton(data.postId, data.likedByCurrentUser);
        });
    }

    // subscribe cập nhật realtime comment qua websocket
    subscribeToPostComments(postId) {
        stompClient.subscribe(`/topic/post/${postId}/comments`, (message) => {
            const data = JSON.parse(message.body);

            // tìm container comment của post
            const commentsSection = document.querySelector(
                `.post-item[data-post-id="${postId}"] .comments-list`
            );

            // cập nhật số lượng comment hiển thị
            const commentCountSpan = document.querySelector(
                `.post-item[data-post-id="${postId}"] .post-comments-count`
            );
            if (commentCountSpan) {
                commentCountSpan.textContent = data.commentCount + " bình luận";
            }
        });
    }

    subscribeToCommentLikes(commentId) {
        console.log('Subscribing to /topic/comments/' + commentId + '/likes');
        stompClient.subscribe(`/topic/comments/${commentId}/likes`, (message) => {
            const data = JSON.parse(message.body);
            // server gửi: { commentId, likeCount, likedByCurrentUser }

            // ✅ update số lượng like
            this.updateLikeCount(data.commentId, data.likeCount, 'comment');

        });
    }

    async editPost(postId) {
        try {

            const response = await fetch(`/posts/api/${postId}`);

            const post = await response.json();

            this.editingPostId = postId;
            this.imagesToDelete = [];

            // Populate modal
            document.getElementById('edit-post-id').value = postId;
            document.getElementById('edit-content').value = post.content;
            document.getElementById('edit-privacy-level').value = post.privacyLevel;
            document.getElementById('edit-comment-privacy-level').value = post.privacyCommentLevel;

            // Display existing images
            this.displayExistingImages(post.imageUrls);

            // Show modal
            const modal = new bootstrap.Modal(document.getElementById('editPostModal'));
            modal.show();

        } catch (error) {
            console.error('Error loading post for edit:', error);
            this.showNotification('Không thể tải thông tin bài viết', 'error');
        }
    }

    displayExistingImages(imageUrls) {
        const container = document.getElementById('edit-existing-images');
        container.innerHTML = '';

        if (!imageUrls || imageUrls.length === 0) {
            return;
        }

        imageUrls.forEach((url, index) => {
            const item = document.createElement('div');
            item.className = 'existing-image-item';
            item.innerHTML = `
                <img src="${url}" alt="Existing image">
                <button type="button" class="existing-image-remove" 
                        onclick="postManager.markImageForDeletion('${url}', this)">
                    <i class="fas fa-times"></i>
                </button>
            `;
            container.appendChild(item);
        });
    }

    markImageForDeletion(imageUrl, button) {
        const item = button.closest('.existing-image-item');

        if (this.imagesToDelete.includes(imageUrl)) {
            // Remove from deletion list
            this.imagesToDelete = this.imagesToDelete.filter(url => url !== imageUrl);
            item.style.opacity = '1';
            button.style.background = '#f02849';
        } else {
            // Add to deletion list
            this.imagesToDelete.push(imageUrl);
            item.style.opacity = '0.5';
            button.style.background = '#28a745';
            button.innerHTML = '<i class="fas fa-undo"></i>';
        }
    }

    async handleEditPost(event) {
        event.preventDefault();

        const form = event.target;
        const formData = new FormData();

        formData.append('content', document.getElementById('edit-content').value);
        formData.append('privacyLevel', document.getElementById('edit-privacy-level').value);
        formData.append('commentPrivacyLevel', document.getElementById('edit-comment-privacy-level').value);

        // Add existing images (not marked for deletion)
        const existingImages = Array.from(document.querySelectorAll('#edit-existing-images img'))
            .map(img => img.src)
            .filter(url => !this.imagesToDelete.includes(url));

        existingImages.forEach(url => formData.append('existingImages', url));

        // Add images to delete
        this.imagesToDelete.forEach(url => formData.append('imagesToDelete', url));

        // Add new images
        const newImagesInput = document.getElementById('edit-new-images');

        Array.from(newImagesInput.files).forEach(file => {
            formData.append('newImages', file);
        });

        const submitBtn = form.querySelector('button[type="submit"]');
        const originalText = submitBtn.innerHTML;
        const elements = form.querySelectorAll('input, select, textarea, button');
        try {
            elements.forEach(element => {
                element.disabled = true;
            });
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang lưu...';

            const response = await fetch(`/posts/api/update/${this.editingPostId}`, {
                method: 'PUT',
                body: formData
            });

            const result = await response.json();

            if (result.success) {
                // Close modal
                bootstrap.Modal.getInstance(document.getElementById('editPostModal')).hide();

                // Refresh the post
                await this.refreshPost(this.editingPostId);

                this.showNotification('Cập nhật bài viết thành công!', 'success');
            } else {
                this.showNotification(result.message || 'Có lỗi xảy ra', 'error');
            }
        } catch (error) {
            console.error('Error updating post:', error);
            this.showNotification('Có lỗi xảy ra khi cập nhật bài viết', 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
            elements.forEach(element => {
                element.disabled = false;
            });
        }
    }

    async refreshPost(postId) {
        try {
            const response = await fetch(`/posts/api/${postId}`);
            const post = await response.json();

            const postElement = document.querySelector(`[data-post-id="${postId}"]`);
            const newPostHtml = this.createPostElement(post);
            postElement.outerHTML = newPostHtml;
        } catch (error) {
            console.error('Error refreshing post:', error);
        }
    }

    resetEditForm() {
        this.editingPostId = null;
        this.imagesToDelete = [];
        document.getElementById('edit-new-images').value = '';
    }

    async deletePost(postId) {
        const result = await Swal.fire({
            title: 'Xác nhận xóa',
            text: 'Bạn có chắc chắn muốn xóa bài viết này?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Xóa',
            cancelButtonText: 'Hủy'
        });

        if (result.isConfirmed) {
            try {
                const response = await fetch(`/posts/api/${postId}`, {
                    method: 'DELETE',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });

                const data = await response.json();

                if (data.success) {
                    // Remove post from DOM
                    const postElement = document.querySelector(`[data-post-id="${postId}"]`);
                    postElement.remove();

                    this.showNotification('Xóa bài viết thành công!', 'success');
                    this.updatePostsCount();
                } else {
                    this.showNotification(data.message || 'Có lỗi xảy ra', 'error');
                }
            } catch (error) {
                console.error('Error deleting post:', error);
                this.showNotification('Có lỗi xảy ra khi xóa bài viết', 'error');
            }
        }
    }

// Thuộc tính của class
    commentState = {}; // { [postId]: { page, hasMore, loading, size } }

// ===== Toggle + bind scroll =====
    async toggleComments(postId) {
        const section = document.getElementById(`comments-${postId}`);
        const isVisible = window.getComputedStyle(section).display !== 'none';

        if (isVisible) {
            // Đang mở thì ẩn đi
            section.style.display = 'none';
            return;
        }

        // Đang ẩn thì mở
        section.style.display = 'block';

        // Khởi tạo state 1 lần cho post
        if (!this.commentState[postId]) {
            this.commentState[postId] = {page: 0, hasMore: true, loading: false, size: 3, loadingPromise: null};
        } else {
            // Mỗi lần mở lại muốn load từ đầu: reset nếu cần
            this.commentState[postId].page = 0;
            this.commentState[postId].hasMore = true;
        }

        const $container = $(`#comments-list-${postId}`);

        // Load trang đầu
        await this.loadComments(postId, /*append*/ false);
        // Nếu nội dung chưa đủ tạo thanh cuộn, tự fill thêm đến khi đủ (hoặc hết dữ liệu)
        await this._prefillViewport(postId);  // Giả sử _prefillViewport là async nếu nó load thêm

        // Gắn scroll 1 lần
        if (!$container.data('scrollBound')) {
            $container.on('scroll', () => {
                const el = $container[0];
                // gần chạm đáy
                if (el.scrollTop + el.clientHeight >= el.scrollHeight - 10) {
                    const st = this.commentState[postId];
                    if (!st.loading && st.hasMore) {
                        this.loadComments(postId, /*append*/ true);
                    }
                }
            });
            $container.data('scrollBound', true);
        }
    }

    async goToComment(commentId, postId) {
        const section = document.getElementById(`comments-${postId}`);
        if (!section) {
            console.error(`Comments section for post ${postId} not found`);
            return;
        }

        // Hiển thị danh sách bình luận nếu chưa hiển thị
        if (window.getComputedStyle(section).display === 'none') {
            await this.toggleComments(postId);
        }

        // Khởi tạo state nếu chưa có (trường hợp section đã visible nhưng chưa init)
        if (!this.commentState[postId]) {
            this.commentState[postId] = {page: 0, hasMore: true, loading: false, size: 3, loadingPromise: null};
            await this.loadComments(postId, /*append*/ false);
        }

        const HIGHLIGHT_DURATION = 2000;

        const tryScrollToComment = async () => {
            let commentElement = document.getElementById(`comment-${commentId}`);
            const state = this.commentState[postId];

            while (!commentElement && state.hasMore) {
                // Vì loadComments có lock, nó sẽ await nếu đang loading
                await this.loadComments(postId, true); // load thêm
                commentElement = document.getElementById(`comment-${commentId}`);
            }

            if (commentElement) {
                commentElement.scrollIntoView({behavior: 'smooth', block: 'center'});
                commentElement.style.backgroundColor = '#e3f2fd';
                setTimeout(() => {
                    commentElement.style.backgroundColor = '';
                }, HIGHLIGHT_DURATION);
            } else {
                console.error(`Comment ${commentId} not found after loading all available comments`);
            }
        };

        // Thực thi tìm kiếm và cuộn
        try {
            await tryScrollToComment();
        } catch (error) {
            console.error(`Error while trying to scroll to comment ${commentId}:`, error);
        }
    }

// ===== Load comments theo trang =====
    loadComments(postId, append = true) {
        const st = this.commentState[postId];
        if (!st || st.loading || (!append && st.page !== 0)) return Promise.resolve();

        st.loading = true;

        const page = st.page;
        const size = st.size;

        return $.ajax({
            url: `/api/comments/${postId}`,
            type: 'GET',
            data: {page, size}
        }).done((data) => {
            console.log(data)
            const $container = $(`#comments-list-${postId}`);
            if (!append) $container.empty();

            (data.content || []).forEach(c => {
                this.appendCommentToUI(postId, c, 'append');
            });

            // cập nhật phân trang
            st.page = page + 1;
            st.hasMore = (data.content || []).length === size;
        }).fail((xhr) => {
            console.error('Lỗi load comments:', xhr?.responseText || xhr?.statusText);
        }).always(() => {
            st.loading = false;
        });
    }

// ===== Tự lấp đầy cho đến khi có thanh cuộn (đề phòng size quá nhỏ) =====
    _prefillViewport(postId) {
        const $container = $(`#comments-list-${postId}`);
        const el = $container[0];
        const st = this.commentState[postId];
        let guard = 0;

        const fill = () => {
            if (!st || guard++ > 5) return; // tối đa 5 lần để tránh vòng lặp vô hạn
            if (el.scrollHeight <= el.clientHeight && st.hasMore && !st.loading) {
                this.loadComments(postId, true).then(fill);
            }
        };
        fill();
    }

    handleCommentKeyPress(event, postId, key = `main-${postId}`) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            if (key.startsWith('main-')) {
                this.submitComment(postId);
            } else if (key.startsWith('reply-')) {
                const parentCommentId = parseInt(key.split('-')[1]);
                const btn = event.target.parentElement.querySelector('button');
                this.submitReply(postId, parentCommentId, btn);
            } else if (key.startsWith('edit-')) {
                const commentId = parseInt(key.split('-')[1]);
                const saveBtn = event.target.parentElement.querySelector('.btn-primary');
                saveBtn.click();
            }
        }
    }

    static processMentions(text, mentionUsers = []) {
        let processed = text;
        mentionUsers.forEach(user => {
            const escapedFullName = user.fullName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const regex = new RegExp(`@${escapedFullName}(?![\\w])`, 'g'); // Stop if next is word char
            processed = processed.replace(regex, `<a href="/profile/${user.username}" class="mention-tag">@${user.fullName}</a>`);
        });
        return processed;
    }

    appendCommentToUI(postId, c, type = 'append', parentId = null) {
        const timeAgo = formatTimeAgo(c.createdAt);
        const canEdit = !!c.canEdit;
        const canDelete = !!c.canDelete || !!c.canDeleted || false;
        const likeCount = c.likeCount || 0;
        this.subscribeToCommentLikes(c.commentId);
        const actionButtons = (canEdit || canDelete) ? `
<div style="position: absolute; top: 5px; right: 5px; z-index: 10;" class="dropdown comment-actions-dropdown">
    <button class="btn btn-light btn-sm" type="button" data-bs-toggle="dropdown" aria-expanded="false">
        <i class="fas fa-ellipsis-h"></i>
    </button>
    <ul class="dropdown-menu dropdown-menu-end" style="min-width: auto; width: 120px;">
        ${canEdit ? `
            <li><button class="dropdown-item" style="font-size: 14px; padding: 5px 10px;" onclick="postManager.editCommentUI(${postId}, ${c.commentId})">
                <i class="fas fa-edit"></i> Sửa
            </button></li>` : ''}
        ${canDelete ? `
            <li><button class="dropdown-item text-danger" style="font-size: 14px; padding: 5px 10px;" onclick="postManager.deleteComment(${postId}, ${c.commentId})">
                <i class="fas fa-trash"></i> Xóa
            </button></li>` : ''}
    </ul>
</div>
` : '';

        const $card = $(`
<div style="position: relative" class="comment-wrapper">
    ${parentId ? `<div class="line-reply"></div>` : ''}  <!-- horizontal connector -->
    <div style="position: relative; max-width: 100%; overflow: hidden;" class="comment-card d-flex ${parentId ? 'reply' : ''}" id="comment-${c.commentId}" data-comment-id="${c.commentId}">
        ${actionButtons} 
        <img src="${c.userAvatarUrl || '/images/default-avatar.jpg'}" alt="avatar" class="comment-avatar">
        <div class="comment-body">
            <strong>${c.userFullName || c.username}</strong>
            <span class="comment-time">${timeAgo}</span>
            <p class="comment-text" >
                ${PostManager.processMentions(c.comment, c.mentionUsers || [])}
            </p>
            <div class="comment-actions d-flex align-items-center mb-2">
                <span class="like-btn ${c.likedByCurrentUser ? 'liked' : ''}" id="comment-like-${c.commentId}" 
                      style="cursor: pointer; width: 50px"
                      onclick="postManager.toggleLike(${c.commentId}, 'comment')" >
                    <i class="fas fa-heart"></i>
                    <span>${likeCount}</span>
                </span>
                
                ${c.canReply ? `<button class="btn btn-link btn-sm text-primary reply-btn ms-3" 
                        onclick="postManager.showReplyBox(${postId}, ${c.commentId})" 
                        title="Trả lời">
                    <i class="fas fa-reply"></i>
                </button>` : ''}
                
            </div>
        </div>
    </div>
</div>
`);

        const $container = $(`#comments-list-${postId}`);
        let $target = parentId ? $(`#replies-group-${parentId}`) : $container;

        if (type != 'append') {
            $target.prepend($card);
        } else {
            $target.append($card);
        }

        // Căn line ngang (nối ngang) so với card bên trong wrapper (dùng position => relative trong wrapper)
        function positionLineReply($cardElem) {
            const $commentCard = $cardElem.find('.comment-card');
            if ($commentCard.length === 0) return;

            // position() trả về tọa độ relative tới offsetParent (ở đây wrapper)
            const cardTopRel = $commentCard.position().top || 0;
            const cardMidRel = Math.round(cardTopRel + $commentCard.outerHeight() / 2);

            // đảm bảo .line-reply tồn tại trước khi set
            const $line = $cardElem.find('.line-reply');
            if ($line.length) {
                $line.css({
                    top: cardMidRel + 'px',
                    transition: 'top 0.3s ease'
                });
            }
        }


        // Cập nhật vertical-line của parent: nối từ midpoint của parent -> midpoint của **last direct child reply**
        function updateConnectorsForParent($parentCardWrapper, $repliesGroup) {
            if ($repliesGroup.length === 0) return;
            const $commentCard = $parentCardWrapper.find('.comment-card');
            if ($commentCard.length === 0) return;

            // midpoint tuyệt đối của parent
            const parentMidAbs = $commentCard.offset().top + $commentCard.outerHeight() / 2;

            // top tuyệt đối của replies-group
            const repliesGroupTopAbs = $repliesGroup.offset().top;

            // --- LẤY last DIRECT CHILD (không include các .comment-card nằm trong nested replies-group)
            const $directWrappers = $repliesGroup.children('.comment-wrapper').filter(':visible');
            let lastCenterAbs;
            if ($directWrappers.length > 0) {
                const $lastDirectWrapper = $directWrappers.last();
                const $lastDirectCard = $lastDirectWrapper.find('.comment-card').first();
                if ($lastDirectCard.length > 0) {
                    lastCenterAbs = $lastDirectCard.offset().top + $lastDirectCard.outerHeight() / 2;
                } else {
                    // fallback: nếu không có .comment-card trong wrapper, dùng center của wrapper
                    lastCenterAbs = $lastDirectWrapper.offset().top + $lastDirectWrapper.outerHeight() / 2;
                }
            } else {
                // fallback: nếu không có direct wrapper (hiếm) -> dùng trung tâm replies-group
                lastCenterAbs = repliesGroupTopAbs + $repliesGroup.outerHeight() / 2;
            }

            // top của vertical-line tương đối so với repliesGroup
            let topRelative = Math.round(parentMidAbs - repliesGroupTopAbs);
            if (topRelative < 0) topRelative = 0; // clamp để không có top âm

            // height từ midpoint parent tới midpoint last direct child
            let heightRelative = Math.round(lastCenterAbs - parentMidAbs);
            if (heightRelative < 8) heightRelative = 8; // min height để thấy rõ đường

            const parentHeight = $commentCard.outerHeight(true);
            const parentHalfHeight = parentHeight / 2;

            $repliesGroup.find('.vertical-line').css({
                top: (topRelative - 10) + 'px',
                height: (heightRelative - parentHalfHeight + 7) + 'px'
            });

            // cập nhật luôn line ngang của parent
            positionLineReply($parentCardWrapper);
        }



        // Nếu comment có replies, tạo replies-group và render replies
        if (c.replies && c.replies.length > 0) {
            let $repliesGroup = $(`#replies-group-${c.commentId}`);
            if ($repliesGroup.length === 0) {
                $repliesGroup = $(`<div class="replies-group" id="replies-group-${c.commentId}" style="position: relative"><div class="vertical-line"></div></div>`);
                $card.after($repliesGroup);
            }
            c.replies.forEach(reply => this.appendCommentToUI(postId, reply, 'append', c.commentId));

            // Sau khi DOM đã chèn (ảnh có thể chưa load) => tính connector
            // dùng requestAnimationFrame để chạy sau khi browser layout xong
            const self = this;
            requestAnimationFrame(() => {
                // tìm lại wrapper của parent (vì $card có thể đã được jQuery clone etc)
                const $parentWrapper = $(`#comment-${c.commentId}`).closest('.comment-wrapper');
                const $rg = $(`#replies-group-${c.commentId}`);
                updateConnectorsForParent($parentWrapper, $rg);
            });

            // Reposition khi avatar load (ảnh có thể ảnh hưởng height)
            $card.find('img.comment-avatar').on('load', () => {
                const $parentWrapper = $(`#comment-${c.commentId}`).closest('.comment-wrapper');
                const $rg = $(`#replies-group-${c.commentId}`);
                requestAnimationFrame(() => updateConnectorsForParent($parentWrapper, $rg));
            });
        } else {
            // Nếu không có replies: vẫn set line-reply trung tâm nếu là reply
            if (parentId) {
                positionLineReply($card);
            }
        }

        return $card;
    }

    // Sửa comment trực tiếp trên UI
    async editCommentUI(postId, commentId) {
        const commentCard = document.getElementById(`comment-${commentId}`);
        const commentTextEl = commentCard.querySelector('.comment-text');
        const originalElement = commentTextEl.cloneNode(true);

        try {
            const response = await fetch(`/api/comment/${commentId}`);
            if (!response.ok) throw new Error('Failed to fetch comment');
            const comment = await response.json();

            // Tạo div contenteditable
            const input = document.createElement('div');
            input.contentEditable = true;
            input.className = 'comment-input mb-1';
            input.setAttribute('onkeypress', `postManager.handleCommentKeyPress(event, ${postId}, 'edit-${commentId}')`);
            input.setAttribute('onkeydown', `postManager.handleMentionKeyDown(event, ${postId}, 'edit-${commentId}')`);
            input.setAttribute('oninput', `postManager.showMentionSuggestions(${postId}, this, 'edit-${commentId}')`);

            const mentionUsers = comment.mentions || [];
            let commentContent = comment.comment || "";

            // --- 1. Nếu backend trả về HTML có <a class="mention"> ---
            if (commentContent.includes("<a") && commentContent.includes("class=\"mention\"")) {
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = commentContent;

                tempDiv.childNodes.forEach(node => {
                    if (node.nodeType === Node.TEXT_NODE) {
                        input.appendChild(document.createTextNode(node.textContent));
                    } else if (node.nodeType === Node.ELEMENT_NODE && node.classList.contains('mention')) {
                        const span = document.createElement('span');
                        span.className = 'mention';
                        span.contentEditable = 'false';
                        span.dataset.userId = node.dataset.userId || node.getAttribute("data-userid");
                        span.textContent = node.textContent;
                        input.appendChild(span);
                    } else {
                        input.appendChild(node.cloneNode(true));
                    }
                });
            }
            // --- 2. Nếu backend trả về plain text có @mention ---
            else if (mentionUsers.length > 0) {
                const mentionMap = new Map(mentionUsers.map(u => [u.fullName.toLowerCase(), u]));
                const regexPatterns = mentionUsers
                    .map(u => `@${u.fullName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?![\\w])`)
                    .join('|');
                const regexGlobal = new RegExp(regexPatterns, 'gi');

                let lastIndex = 0, match;
                while ((match = regexGlobal.exec(commentContent)) !== null) {
                    const textBefore = commentContent.substring(lastIndex, match.index);
                    if (textBefore) input.appendChild(document.createTextNode(textBefore));

                    const matchedText = match[0];
                    const fullName = matchedText.substring(1);
                    const user = mentionMap.get(fullName.toLowerCase());

                    if (user) {
                        const span = document.createElement('span');
                        span.className = 'mention';
                        span.contentEditable = 'false';
                        span.dataset.userId = user.id;
                        span.textContent = matchedText;
                        input.appendChild(span);
                    } else {
                        input.appendChild(document.createTextNode(matchedText));
                    }

                    lastIndex = match.index + match[0].length;
                }
                const textAfter = commentContent.substring(lastIndex);
                if (textAfter) input.appendChild(document.createTextNode(textAfter));
            }
            // --- 3. Nếu không có mention ---
            else {
                input.textContent = commentContent;
            }

            commentTextEl.replaceWith(input);

            // Tạo dropdown cho mention
            const dropdown = document.createElement('ul');
            dropdown.id = `mentions-dropdown-edit-${commentId}`;
            dropdown.className = 'mentions-dropdown';
            const commentBody = commentCard.querySelector('.comment-body');
            commentBody.style.position = 'relative';
            commentBody.appendChild(dropdown);

            // Tạo nút Lưu & Hủy
            const actionsDiv = commentCard.querySelector('.comment-actions');
            const saveBtn = document.createElement('button');
            saveBtn.className = 'btn btn-primary btn-sm me-1';
            saveBtn.textContent = 'Lưu';

            const cancelBtn = document.createElement('button');
            cancelBtn.className = 'btn btn-secondary btn-sm';
            cancelBtn.textContent = 'Hủy';

            actionsDiv.appendChild(saveBtn);
            actionsDiv.appendChild(cancelBtn);

            // Lưu comment
            saveBtn.addEventListener('click', async () => {
                const newContent = input.innerText.trim();
                if (!newContent) return;

                try {
                    const mentionedUserIds = [...new Set(
                        Array.from(input.querySelectorAll('.mention'))
                            .map(span => parseInt(span.dataset.userId, 10))
                            .filter(id => id !== null && id !== undefined && !isNaN(id))
                    )];

                    const response = await fetch(`/api/comments/${commentId}`, {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json',
                            'X-Requested-With': 'XMLHttpRequest'
                        },
                        body: JSON.stringify({ content: newContent, mentionedUserIds })
                    });

                    if (!response.ok) throw new Error('Update failed');

                    const updated = await response.json();

                    // Cập nhật UI
                    const p = document.createElement('p');
                    p.className = 'comment-text';
                    p.innerHTML = PostManager.processMentions(updated.comment, updated.mentionUsers || []);
                    input.replaceWith(p);

                    saveBtn.remove();
                    cancelBtn.remove();

                    dropdown.remove();
                    commentBody.style.position = '';

                    postManager.showNotification('Cập nhật comment thành công!', 'success');

                } catch (error) {
                    console.error('Error updating comment:', error);
                    postManager.showNotification('Có lỗi xảy ra khi cập nhật comment', 'error');
                }
            });

            // Hủy sửa
            cancelBtn.addEventListener('click', () => {
                input.replaceWith(originalElement);
                saveBtn.remove();
                cancelBtn.remove();
                dropdown.remove();
                commentBody.style.position = '';
            });

        } catch (error) {
            console.error('Error loading comment for edit:', error);
            this.showNotification('Không thể tải thông tin bình luận', 'error');
        }
    }


    async deleteComment(postId, commentId) {
        try {
            const confirmed = await Swal.fire({
                title: 'Xác nhận xóa',
                text: 'Bạn có chắc chắn muốn xóa bình luận này?',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Xóa',
                cancelButtonText: 'Hủy'
            });

            if (!confirmed.isConfirmed) return;

            const response = await fetch(`/api/comments/${commentId}`, {
                method: 'DELETE',
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            });

            let data = {};
            if (response.ok) {
                const text = await response.text();
                data = text ? JSON.parse(text) : {};
            } else {
                this.showNotification('Xóa bình luận thất bại (lỗi server)', 'error');
                return;
            }

            if (data.success) {
                // Xóa comment khỏi UI
                const commentEl = document.getElementById(`comment-${commentId}`);
                if (commentEl) commentEl.remove();

                if (!data.deletedComment?.parentCommentId) {
                    const repliesGroup = document.getElementById(`replies-group-${commentId}`);
                    if (repliesGroup) repliesGroup.remove();

                    const st = this.commentState[postId];
                    if (st) {
                        st.total = Math.max(0, (st.total || 1) - 1);
                        st.hasMore = true;
                    }
                }

                this.showNotification('Xóa bình luận thành công!', 'success');
            } else {
                this.showNotification(data.message || 'Có lỗi xảy ra khi xóa comment', 'error');
            }
        } catch (error) {
            console.error('Error deleting comment:', error);
            this.showNotification('Có lỗi xảy ra khi xóa comment', 'error');
        }
    }

    async submitComment(postId, key = `main-${postId}`) {
        const commentInput = document.querySelector(`#comments-${postId} .comment-input`);
        const content = commentInput.innerText.trim();
        if (!content) return;

        // Disable input + button
        commentInput.contentEditable = 'false';
        const submitBtn = commentInput.parentElement.querySelector('.comment-submit');
        if (submitBtn) submitBtn.disabled = true;

        try {
            const mentionedUserIds = [...new Set(Array.from(commentInput.querySelectorAll('.mention')).map(span => parseInt(span.dataset.userId)))];
            const res = await fetch('/api/comments/add', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({postId, content, mentionedUserIds})
            });

            if (!res.ok) {
                const result = await res.json();
                throw new Error(result.message || 'Failed to add comment');
            }

            const newComment = await res.json();
            this.appendCommentToUI(postId, newComment, 'pre');
            commentInput.innerHTML = '';
            this.showNotification('Bình luận đã được thêm!', 'success');
        } catch (e) {
            console.error(e);
            this.showNotification('Không thể gửi bình luận: ' + e.message, 'error');
        } finally {
            commentInput.contentEditable = 'true';
            if (submitBtn) submitBtn.disabled = false;
        }
    }

    showReplyBox(postId, parentCommentId) {
        let $replyContainer = $(`#replies-group-${parentCommentId}`);
        if ($replyContainer.length === 0) {
            $replyContainer = $(`<div class="replies-group" id="replies-group-${parentCommentId}" style="position: relative"><div class="vertical-line"></div></div>`);
            $(`#comment-${parentCommentId}`).after($replyContainer);
        }

        const existingBox = $replyContainer.find(".reply-box");
        if (existingBox.length > 0) {
            existingBox.remove();
            return;
        }
        const $replyBox = $(`
            <div class="comment-form reply-box">
              <img src="${this.getCurrentUserAvatar()}" alt="Avatar" class="comment-avatar">
              <div class="comment-input-container" style="position: relative">
                <div contenteditable="true" class="comment-input" data-placeholder="Viết bình luận..."
                     onkeypress="postManager.handleCommentKeyPress(event, ${postId}, 'reply-${parentCommentId}')"
                     onkeydown="postManager.handleMentionKeyDown(event, ${postId}, 'reply-${parentCommentId}')"
                     oninput="postManager.showMentionSuggestions(${postId}, this, 'reply-${parentCommentId}')"></div>
                <ul id="mentions-dropdown-reply-${parentCommentId}" class="mentions-dropdown"></ul>
                <button class="comment-submit" onclick="postManager.submitReply(${postId}, ${parentCommentId}, this)">
                  <i class="fas fa-paper-plane"></i>
                </button>
              </div>
            </div>
                `);

        $replyContainer.prepend($replyBox);
    }

    // submitReply (robust, xử lý jQuery/DOM, thay NBSP, fallback)
    async submitReply(postId, parentCommentId, btn, key = `reply-${parentCommentId}`) {
        try {
            // nếu btn là jQuery object => lấy phần tử DOM
            if (btn && btn.jquery) btn = btn[0];

            // lấy reply-box một cách an toàn (không phụ thuộc vào previousElementSibling chain)
            const replyBox = (btn && typeof btn.closest === 'function') ? btn.closest('.reply-box') : null;
            let input = replyBox ? replyBox.querySelector('.comment-input') : null;

            // fallback: cố gắng dò bằng previousElementSibling như trước (nếu cần)
            if (!input && btn) {
                const prev1 = btn.previousElementSibling;
                const prev2 = prev1 ? prev1.previousElementSibling : null;
                if (prev2 && prev2.classList && prev2.classList.contains('comment-input')) {
                    input = prev2;
                }
            }

            if (!input) {
                console.error('Không tìm thấy .comment-input cho reply (submitReply).');
                this.showNotification("Lỗi: không tìm thấy ô nhập.", "error");
                return;
            }

            // disable button + khóa input
            btn.disabled = true;
            input.contentEditable = 'false';

            // lấy text an toàn: replace NBSP rồi trim
            const rawText = (input.textContent ?? '').replace(/\u00A0/g, ' ').trim();
            if (!rawText) {
                // nếu rỗng sau khi trim thì bỏ qua (bật lại trạng thái trước khi return)
                btn.disabled = false;
                input.contentEditable = 'true';
                return;
            }

            // lấy mention ids (unique)
            const mentionedUserIds = [...new Set(
                Array.from(input.querySelectorAll('.mention'))
                    .map(span => Number(span.dataset.userId))
                    .filter(id => !Number.isNaN(id) && id > 0)
            )];

            const payload = {
                postId,
                content: rawText,
                mentionedUserIds
            };

            const res = await fetch(`/api/comments/${parentCommentId}/reply`, {
                method: "POST",
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                const result = await res.json().catch(() => ({}));
                throw new Error(result.message || 'Failed to add reply');
            }

            const data = await res.json();
            if (data && data.reply) {
                let $repliesGroup = $(`#replies-group-${parentCommentId}`);
                if ($repliesGroup.length === 0) {
                    $repliesGroup = $(`<div class="replies-group" id="replies-group-${parentCommentId}" style="position: relative"><div class="vertical-line"></div></div>`);
                    $(`#comment-${parentCommentId}`).after($repliesGroup);
                }
                this.appendCommentToUI(postId, data.reply, 'prepend', parentCommentId);
                // xóa ô nhập
                input.closest(".reply-box")?.remove();
                this.showNotification("Phản hồi đã được gửi!", "success");
            }

        } catch (e) {
            console.error(e);
            this.showNotification("Có lỗi xảy ra khi gửi phản hồi: " + e.message, "error");
        } finally {
            // chỉ enable lại button ở đây (input có thể đã bị remove)
            try { btn.disabled = false; } catch (e) {}
        }
    }

    viewImage(imageUrl) {
        // Open image in modal or lightbox
        const modal = document.createElement('div');
        modal.className = 'image-modal';
        modal.innerHTML = `
            <div class="image-modal-backdrop" onclick="this.parentElement.remove()">
                <div class="image-modal-content">
                    <img src="${imageUrl}" alt="Full size image">
                    <button class="image-modal-close" onclick="this.closest('.image-modal').remove()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            </div>
        `;

        // Add modal styles
        modal.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.9);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 10000;
        `;

        const content = modal.querySelector('.image-modal-content');
        content.style.cssText = `
            position: relative;
            max-width: 90%;
            max-height: 90%;
        `;

        const img = modal.querySelector('img');
        img.style.cssText = `
            max-width: 100%;
            max-height: 100%;
            object-fit: contain;
        `;

        const closeBtn = modal.querySelector('.image-modal-close');
        closeBtn.style.cssText = `
            position: absolute;
            top: -40px;
            right: 0;
            background: rgba(255, 255, 255, 0.2);
            border: none;
            color: white;
            font-size: 24px;
            padding: 8px 12px;
            border-radius: 4px;
            cursor: pointer;
        `;

        document.body.appendChild(modal);
    }

    viewImages(imageUrls, startIndex = 0) {
        // Open image gallery modal
        console.log('Opening gallery:', imageUrls, 'starting at:', startIndex);
        this.viewImage(imageUrls[startIndex]);
    }

    getCurrentUserAvatar() {
        // Get current user avatar from page context
        const userAvatar = document.querySelector('.navbar .dropdown img');
        return userAvatar ? userAvatar.src : '/images/default-avatar.jpg';
    }

    showLoading(show) {
        const loadingIndicator = document.getElementById('loading-indicator');
        if (loadingIndicator) {
            loadingIndicator.style.display = show ? 'block' : 'none';
        }
    }

    showNotification(message, type = 'info') {
        // Use Toast notification
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-white bg-${type === 'success' ? 'success' : type === 'error' ? 'danger' : 'info'} border-0`;
        toast.setAttribute('role', 'alert');
        toast.style.cssText = 'position: fixed; top: 80px; right: 20px; z-index: 9999;';

        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" 
                        data-bs-dismiss="toast"></button>
            </div>
        `;

        document.body.appendChild(toast);

        const bsToast = new bootstrap.Toast(toast);
        bsToast.show();

        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    }

    updatePostsCount() {
        // Update posts count in sidebar
        const postsCountEl = document.getElementById('posts-count');
        if (postsCountEl) {
            const currentCount = parseInt(postsCountEl.textContent) || 0;
            postsCountEl.textContent = currentCount + 1;
        }
    }

    getMentionQuery(element) {
        const sel = window.getSelection();
        if (!sel.rangeCount) return null;
        const range = sel.getRangeAt(0);
        if (!range.collapsed) return null;
        const node = range.startContainer;
        if (node.nodeType !== Node.TEXT_NODE) return null;
        const text = node.textContent.substring(0, range.startOffset);
        const match = text.match(/@([^\s]*)$/);
        if (!match) return null;
        return {
            query: match[1],
            start: text.length - match[0].length,
            length: match[0].length
        };
    }

    getCursorRect(element) {
        const sel = window.getSelection();
        if (!sel.rangeCount) return null;
        const range = sel.getRangeAt(0).cloneRange();
        range.collapse(true);
        const span = document.createElement('span');
        span.appendChild(document.createTextNode('\u200b')); // zero-width space
        range.insertNode(span);
        const rect = span.getBoundingClientRect();
        span.remove();
        return rect;
    }

    showMentionSuggestions(postId, inputElement, key) {
        const mention = this.getMentionQuery(inputElement);
        const dropdown = document.getElementById(`mentions-dropdown-${key}`);

        if (!mention) {
            if (dropdown) dropdown.style.display = "none";
            this.cleanupMentionListeners();
            return;
        }

        this._mentionNode = window.getSelection().getRangeAt(0).startContainer;
        this._mentionStart = mention.start;
        this._mentionLength = mention.length;
        const query = mention.query.toLowerCase();

        fetch(`/api/friends/search?keyword=${query}`)
            .then(res => res.json())
            .then(users => {
                if (dropdown) dropdown.innerHTML = "";

                users.forEach((user, i) => {
                    const li = document.createElement("li");
                    li.className = `mention-item ${i === 0 ? 'selected' : ''}`;
                    li.innerHTML = `
                    <img src="${user.avatarUrl || '/images/default-avatar.png'}" 
                         alt="${user.fullName}" class="mention-avatar">
                    <span class="mention-name">${user.fullName}</span>
                `;
                    li.onclick = () => this.insertMention(inputElement, user, key);
                    if (dropdown) dropdown.appendChild(li);
                });

                if (users.length > 0 && dropdown) {
                    // Append to body if not already
                    if (dropdown.parentNode !== document.body) {
                        document.body.appendChild(dropdown);
                    }

                    const updatePosition = () => {
                        if (!inputElement.isConnected) {
                            this.cleanupMentionListeners();
                            dropdown.style.display = "none";
                            return;
                        }
                        const rect = this.getCursorRect(inputElement);
                        if (rect) {
                            dropdown.style.position = 'absolute';
                            dropdown.style.padding = 0;
                            dropdown.style.zIndex = '10000';
                            dropdown.style.top = `${rect.bottom + window.scrollY - 60}px`;
                            dropdown.style.left = `${rect.left + window.scrollX}px`;
                            dropdown.style.width = '25%';
                            dropdown.style.height = '60%';
                            dropdown.style.overflowY = 'auto';
                            dropdown.style.overflowX = 'hidden';
                        }
                    };

                    updatePosition();
                    dropdown.style.display = "block";

                    // Thêm listener để ẩn dropdown khi click ngoài
                    const handleClickOutside = (event) => {
                        if (!dropdown.contains(event.target) && event.target !== inputElement) {
                            dropdown.style.display = "none";
                            this.cleanupMentionListeners();
                            document.removeEventListener('mousedown', handleClickOutside);
                        }
                    };

                    // Ngăn chặn propagation từ mention-item
                    dropdown.querySelectorAll('.mention-item').forEach(item => {
                        item.addEventListener('mousedown', (e) => e.stopPropagation());
                    });

                    document.addEventListener('mousedown', handleClickOutside);

                    // Cập nhật vị trí khi scroll hoặc resize
                    let scroller = inputElement.parentElement;
                    while (scroller) {
                        const style = getComputedStyle(scroller);
                        if (style.overflow === 'auto' || style.overflowY === 'auto' || style.overflow === 'scroll' || style.overflowY === 'scroll') {
                            break;
                        }
                        scroller = scroller.parentElement;
                    }
                    this.mentionScrollContainer = scroller || window;
                    this.mentionScrollListener = updatePosition;
                    this.mentionScrollContainer.addEventListener('scroll', this.mentionScrollListener);
                    window.addEventListener('resize', this.mentionScrollListener);
                } else {
                    if (dropdown) dropdown.style.display = "none";
                    this.cleanupMentionListeners();
                }
            });
    }

    insertMention(inputElement, user, key) {
        const sel = window.getSelection();
        if (!sel.rangeCount) return;
        const range = sel.getRangeAt(0);

        // Xác định vị trí bắt đầu và kết thúc của @query
        range.setStart(this._mentionNode, this._mentionStart);
        range.setEnd(this._mentionNode, this._mentionStart + this._mentionLength);
        range.deleteContents();

        // Tạo span cho mention
        const span = document.createElement('span');
        span.className = 'mention';
        span.contentEditable = 'false';
        span.dataset.userId = user.id;
        span.textContent = `@${user.fullName}`;

        // Tạo khoảng trắng sau mention
        const space = document.createTextNode(' ');

        // Gom span + space vào fragment
        const frag = document.createDocumentFragment();
        frag.appendChild(span);
        frag.appendChild(space);

        // Chèn fragment
        range.insertNode(frag);

        // Move caret ra sau khoảng trắng
        range.setStartAfter(space);
        range.setEndAfter(space);

        sel.removeAllRanges();
        sel.addRange(range);

        // Đảm bảo focus vào input
        inputElement.focus();

        // Ẩn dropdown
        const dropdown = document.getElementById(`mentions-dropdown-${key}`);
        if (dropdown) {
            dropdown.style.display = "none";
            this.cleanupMentionListeners();
        }

        // Trigger input event để cập nhật UI
        inputElement.dispatchEvent(new Event('input'));
    }

    handleMentionKeyDown(evt, postId, key) {
        const dropdown = document.getElementById(`mentions-dropdown-${key}`);
        if (!dropdown || dropdown.style.display !== 'block') return;

        const items = dropdown.querySelectorAll('.mention-item');
        let idx = Array.from(items).findIndex(x => x.classList.contains('selected'));

        switch (evt.key) {
            case 'ArrowDown':
                evt.preventDefault();
                idx = Math.min(idx + 1, items.length - 1);
                items.forEach((it, i) => it.classList.toggle('selected', i === idx));
                break;
            case 'ArrowUp':
                evt.preventDefault();
                idx = Math.max(idx - 1, 0);
                items.forEach((it, i) => it.classList.toggle('selected', i === idx));
                break;
            case 'Enter':
            case 'Tab':
                if (idx >= 0) {
                    evt.preventDefault();
                    items[idx].click();
                }
                break;
            case 'Escape':
                dropdown.style.display = 'none';
                this.cleanupMentionListeners();
                break;
        }
    }
}

let stompClient = null;
// Initialize PostManager when DOM is loaded
document.addEventListener('DOMContentLoaded', function () {
    // connect WebSocket
    const socket = new SockJS('/ws'); // endpoint websocket spring boot
    stompClient = Stomp.over(socket);

    stompClient.connect({}, (frame) => {
        console.log('Connected: ' + frame);
        window.postManager = new PostManager();
    });
});