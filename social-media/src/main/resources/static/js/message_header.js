class MessageDropdown {
    constructor() {
        this.messageIcon = document.getElementById('messageIcon');
        this.messageDropdown = document.getElementById('messageDropdown');
        this.conversationList = document.getElementById('conversationListHeader');
        this.isOpen = false;
        this.modal = null;
        this.currentPage = 0;
        this.hasMore = true;
        this.isLoading = false;
        // Danh sách user chọn để tạo nhóm - Giữ trong class thay vì global
        this.selectedUsers = new Map(); // Sử dụng Map để lưu thêm info: id -> {name, avatar}
        this.init();
    }

    init() {
        if (!this.messageIcon || !this.messageDropdown || !this.conversationList) {
            console.error('Message dropdown elements not found!');
            return;
        }

        const modalEl = document.getElementById("createGroupModal");
        if (modalEl) {
            this.modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        }

        this.messageIcon.removeAttribute('data-bs-toggle');
        this.messageIcon.removeAttribute('data-bs-target');
        this.setupEventListeners();
        this.addCreateGroupStyles();
    }

    setupEventListeners() {
        // Toggle dropdown on icon click
        this.messageIcon.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            closeAllDropdowns(this.messageIcon); // Close other dropdowns before toggling this one
            this.toggleDropdown();
        });
    }

    toggleDropdown() {
        this.isOpen ? this.closeDropdown() : this.openDropdown();
    }

    openDropdown() {
        this.isOpen = true;
        this.messageDropdown.classList.add('show');
        this.positionDropdown();
        this.loadContacts(); // Load fresh data each time
    }

    closeDropdown() {
        this.isOpen = false;
        this.messageDropdown.classList.remove('show');
    }

    positionDropdown() {
        const rect = this.messageIcon.getBoundingClientRect();
        const dropdownRect = this.messageDropdown.getBoundingClientRect();
        let top = rect.bottom + 12;
        let right = window.innerWidth - rect.right;
        if (right + dropdownRect.width > window.innerWidth) right = 16;
        this.messageDropdown.style.top = `${top}px`;
        this.messageDropdown.style.right = `${right}px`;
    }

    async loadContacts() {
        this.currentPage = 0;
        this.hasMore = true;
        this.conversationList.innerHTML = '';  // Reset list
        this.loadMoreContacts();

        // Thêm infinite scroll listener nếu chưa có
        if (!this.conversationList.dataset.scrollBound) {
            this.conversationList.addEventListener('scroll', () => this.handleScroll());
            this.conversationList.dataset.scrollBound = 'true';
        }
    }

    handleScroll() {
        if (this.isLoading || !this.hasMore) return;
        const { scrollTop, scrollHeight, clientHeight } = this.conversationList;
        if (scrollTop + clientHeight >= scrollHeight - 50) {  // Gần cuối (threshold 50px)
            this.loadMoreContacts();
        }
    }

    async loadMoreContacts() {
        if (this.isLoading || !this.hasMore) return;
        this.isLoading = true;
        this.showLoading(true, true);  // Show loader and append

        try {
            const data = await this.fetchContacts(this.currentPage);
            this.renderContacts(data.content, true);  // Append mode
            this.hasMore = data.last === false;  // Kiểm tra có page tiếp không (Page object có 'last')
            this.currentPage++;
        } catch (error) {
            console.error('Error loading contacts:', error);
            this.showErrorState();
        } finally {
            this.isLoading = false;
            this.showLoading(false);  // Hide loader
        }
    }

    async fetchContacts(page) {
        const response = await fetch(`/api/conversations?page=${page}&size=8`, {
            headers: {'X-Requested-With': 'XMLHttpRequest'}
        });
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        return await response.json();
    }

    renderContacts(data, append = false) {
        let html = '';

        // Đảm bảo luôn có nút "Tạo nhóm mới" ở đầu
        if (!append) {
            html += this.createGroupButtonHtml();
        } else {
            // Nếu append, chỉ cần thêm friend, KHÔNG đụng tới createGroup đã render từ trước
        }

        if (data.length > 0) {
            html += data.map(friend => this.createFriendItemHtml(friend)).join('');
        } else if (!append) {
            this.showEmptyState();
            return;
        }

        if (append) {
            // Chèn ngay SAU nút createGroup
            const createGroupEl = this.conversationList.querySelector('.create-group-item');
            if (createGroupEl) {
                createGroupEl.insertAdjacentHTML('afterend', html);
            } else {
                // fallback nếu lỡ bị mất thì render lại nút + data
                this.conversationList.innerHTML = this.createGroupButtonHtml() + html;
            }
        } else {
            this.conversationList.innerHTML = html;
        }
    }

    createGroupButtonHtml() {
        return `
            <div class="conversation-item create-group-item" onclick="messageDropdown.showCreateGroupModal()">
                <div style="position: relative;">
                    <div class="create-group-icon">
                        <i class="fas fa-plus"></i>
                    </div>
                </div>
                <div class="conversation-info">
                    <div class="conversation-name" style="color: #1877f2;">
                        <i class="fas fa-users"></i> Tạo nhóm mới
                    </div>
                    <div class="conversation-preview">Tạo cuộc trò chuyện nhóm</div>
                </div>
            </div>
        `;
    }

    createFriendItemHtml(friend) {
        const nameEsc = (friend.name || '').replace(/'/g, "\\'");
        const avatar = friend.avatar || '/images/default-avatar.jpg';
        const type = friend.type || 'private';
        const preview = friend.lastMessage
            ? `${friend.lastMessage} • ${friend.timeAgo || ''}`
            : 'Chưa có tin nhắn • Vừa tạo';
        const spanStyle = friend.unreadCount && friend.unreadCount > 0 ? '' : 'display: none;';

        // Hiển thị trạng thái activity với màu sắc rõ ràng
        const onlineClass = friend.isOnline || friend.online ? 'online' : 'offline';
        const activityText = friend.isOnline || friend.online ? 'Đang hoạt động' : 'Không hoạt động';

        return `
        <div class="conversation-item" 
             onclick="messageDropdown.openChat('${friend.id}', '${nameEsc}', '${avatar}', '${type}')">
            <div style="position: relative;">
                <img src="${avatar}" alt="Avatar" class="conversation-avatar">
            </div>
            <div class="conversation-info">
                <div class="conversation-name d-flex justify-content-between align-items-center">
                    <span>${friend.name}</span>
                     <span class="badge bg-danger ms-2 span-conversation-id-${friend.id}" style="${spanStyle}">
                        ${friend.unreadCount}
                      </span>
                </div>
                <div class="conversation-preview">${preview}</div>
                <div class="user-activity-status ${onlineClass}">
                    <span class="activity-dot"></span>
                    <span>${activityText}</span>
                </div>
            </div>
        </div>
    `;
    }

    openChat(id, name, avatar, type = 'private') {
        this.closeDropdown();
        if (typeof chatManager !== 'undefined') {
            chatManager.openExistingConversation(id, name, avatar, type);
        } else {
            console.log('Opening chat with:', name, type);
            alert(`Đang mở chat với ${name} (${type})`);
        }
    }

    showCreateGroupModal() {
        this.closeDropdown();
        // Reset form
        document.getElementById('cg-name').value = 'Nhóm mới';
        document.getElementById('cg-search').value = '';
        document.getElementById('cg-results').innerHTML =
            '<div class="text-muted text-center">Gõ tên để tìm bạn bè...</div>';
        document.getElementById('cg-avatar').value = '';
        this.selectedUsers.clear();
        this.updateSelectedUsersDisplay();
        if (this.modal) {
            this.modal.show();
        }
    }

    async searchUsersForGroup(query) {
        const resultsDiv = document.getElementById('cg-results');
        if (!query || query.trim().length < 2) {
            resultsDiv.innerHTML = '<div class="text-muted text-center">Gõ ít nhất 2 ký tự để tìm...</div>';
            return;
        }
        try {
            const response = await fetch(`/api/chat/search-friends?query=${encodeURIComponent(query)}`);
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const users = await response.json();
            if (users.length === 0) {
                resultsDiv.innerHTML = '<div class="text-muted text-center">Không tìm thấy bạn bè nào</div>';
                return;
            }
            let html = '';
            users.forEach(user => {
                const isSelected = this.selectedUsers.has(user.id);
                html += `
                <div class="d-flex align-items-center p-2 border-bottom user-search-item ${isSelected ? 'bg-light' : ''}"
                     onclick="messageDropdown.toggleUserSelection(${user.id}, '${user.fullName.replace(/'/g, "\\'")}', '${user.avatarUrl || '/images/default-avatar.jpg'}', this)"
                     style="cursor: pointer;">
                    <img src="${user.avatarUrl || '/images/default-avatar.jpg'}"
                         class="rounded-circle me-2" width="32" height="32"
                         onerror="this.src='/images/default-avatar.jpg'">
                    <div class="flex-grow-1">
                        <div class="fw-bold">${user.fullName}</div>
                        <small class="text-muted">@${user.username}</small>
                    </div>
                     ${isSelected ? '<i class="fas fa-check text-primary"></i>' : ''}
                </div>`;
            });
            resultsDiv.innerHTML = html;
        } catch (error) {
            console.error('Error searching users:', error);
            resultsDiv.innerHTML = '<div class="text-danger text-center">Lỗi tìm kiếm bạn bè</div>';
        }
    }

    toggleUserSelection(userId, fullName, avatarUrl, element) {
        if (this.selectedUsers.has(userId)) {
            this.selectedUsers.delete(userId);
            element.classList.remove('bg-light');
            element.querySelector('.fas')?.remove();
        } else {
            this.selectedUsers.set(userId, {fullName, avatarUrl});
            element.classList.add('bg-light');
            element.innerHTML += '<i class="fas fa-check text-primary"></i>';
        }
        this.updateSelectedUsersDisplay();
    }

    updateSelectedUsersDisplay() {
        const countEl = document.getElementById('cg-picked');
        const listEl = document.getElementById('cg-selected-list'); // Giả sử có div này trong modal để hiển thị danh sách
        if (countEl) {
            countEl.textContent = this.selectedUsers.size;
        }
        if (listEl) {
            let html = '';
            if (this.selectedUsers.size === 0) {
                html = '<div class="text-muted text-center">Chưa chọn thành viên nào</div>';
            } else {
                this.selectedUsers.forEach(({fullName, avatarUrl}, userId) => {
                    html += `
                    <div class="d-flex align-items-center p-1 border-bottom selected-user-item">
                        <img src="${avatarUrl}" class="rounded-circle me-2" width="24" height="24" onerror="this.src='/images/default-avatar.jpg'">
                        <div class="flex-grow-1">${fullName}</div>
                        <i class="fas fa-times text-danger ms-2" style="cursor: pointer;" onclick="messageDropdown.removeSelectedUser(${userId}, this.closest('.selected-user-item'))"></i>
                    </div>`;
                });
            }
            listEl.innerHTML = html;
        }
    }

    removeSelectedUser(userId, element) {
        this.selectedUsers.delete(userId);
        element.remove();
        this.updateSelectedUsersDisplay();
        // Cập nhật lại item trong search results nếu đang hiển thị
        const searchItems = document.querySelectorAll('.user-search-item');
        searchItems.forEach(item => {
            if (item.onclick.toString().includes(userId)) {
                item.classList.remove('bg-light');
                item.querySelector('.fas')?.remove();
            }
        });
    }

    async createGroup() {
        const groupName = document.getElementById('cg-name').value.trim() || 'Nhóm mới';
        const participantIds = Array.from(this.selectedUsers.keys());
        if (participantIds.length < 2) {
            alert('Vui lòng chọn ít nhất 2 thành viên');
            return;
        }
        const createBtn = document.getElementById('cg-create');
        createBtn.disabled = true;
        createBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang tạo...';
        try {
            const response = await fetch('/api/chat/create-group-from-ids', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'},
                body: JSON.stringify({groupName, participantIds})
            });
            const result = await response.json();
            if (result.success) {
                let avatarUrl = "https://res.cloudinary.com/dryyvmkwo/image/upload/v1748588721/samples/cloudinary-group.jpg"; // Avatar mặc định ban đầu
                // Upload avatar nếu có
                const avatarFile = document.getElementById('cg-avatar').files[0];
                const formData = new FormData();
                formData.append('avatar', avatarFile);
                const uploadResponse = await fetch(`/api/chat/conversation/${result.conversation.id}/avatar`, {
                    method: 'POST',
                    body: formData
                });
                const uploadResult = await uploadResponse.json(); // Giả sử server trả về { success: true, avatar: newUrl }
                if (uploadResult.success && uploadResult.avatarUrl) {
                    avatarUrl = uploadResult.avatarUrl; // Cập nhật avatar mới
                }
                this.modal.hide();
                if (window.chatManager) {
                    chatManager.openExistingConversation(
                        result.conversation.id,
                        result.conversation.name,
                        avatarUrl, // Sử dụng avatar đã cập nhật
                        'group'
                    );
                }
                if (window.newsFeedManager) {
                    newsFeedManager.loadGroupChats();
                }
            } else {
                alert('Tạo nhóm thất bại: ' + (result.error || 'Lỗi không xác định'));
            }
        } catch (error) {
            console.error('Error creating group:', error);
            alert('Có lỗi xảy ra khi tạo nhóm');
        } finally {
            createBtn.disabled = false;
            createBtn.innerHTML = 'Tạo nhóm';
        }
    }

    showLoading(show, append = false) {
        const loaderClass = 'loading-state';
        if (!show) {
            // Hide: Remove all loader elements
            const loaders = this.conversationList.querySelectorAll(`.${loaderClass}`);
            loaders.forEach(loader => loader.remove());
            return;
        }
        // Show: Add loader
        const loader = `<div class="${loaderClass} text-center p-3"><i class="fas fa-spinner fa-spin"></i> Đang tải...</div>`;
        if (append) {
            this.conversationList.innerHTML += loader;
        } else {
            this.conversationList.innerHTML = loader;
        }
    }

    showEmptyState() {
        this.conversationList.innerHTML = `
            <div class="text-center p-3 text-muted">
                <i class="fas fa-users"></i><br>
                Không có liên hệ nào
            </div>
        `;
    }

    showErrorState() {
        this.conversationList.innerHTML = `
            <div class="text-center p-3 text-danger">
                <i class="fas fa-exclamation-triangle"></i><br>
                Lỗi tải danh sách liên hệ
                <br><a href="#" onclick="messageDropdown.loadContacts()" style="color: #1877f2;">Thử lại</a>
            </div>
        `;
    }

    addCreateGroupStyles() {
        if (!document.getElementById('create-group-styles')) {
            const style = document.createElement('style');
            style.id = 'create-group-styles';
            style.textContent = `
                .create-group-item { border-bottom: 1px solid #e4e6ea; margin-bottom: 8px; padding-bottom: 12px !important; }
                .create-group-item:hover { background-color: #f0f8ff !important; }
                .create-group-icon { width: 56px; height: 56px; border-radius: 50%; background: linear-gradient(135deg, #1877f2, #42a5f5); display: flex; align-items: center; justify-content: center; color: white; font-size: 20px; font-weight: bold; }
            `;
            document.head.appendChild(style);
        }
    }
}

async function fetchTotalUnread() {
    try {
        const response = await fetch(`/api/chat/unread/total/${currentUserId}`, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });
        if (!response.ok) throw new Error('Network response was not ok');
        const count = await response.json();
        updateMessageBadge(count);
    } catch (err) {
        console.error('Error fetching total unread:', err);
    }
}

function updateMessageBadge(totalUnread) {
    const badge = document.getElementById('messageBadge');
    if (!badge) return;
    if (totalUnread > 0) {
        badge.textContent = totalUnread;
        badge.style.display = 'inline-block';
    } else {
        badge.style.display = 'none';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.messageDropdown = new MessageDropdown();
    window.searchUsersForGroup = (q) => messageDropdown.searchUsersForGroup(q);
    window.createGroup = () => messageDropdown.createGroup();
    fetchTotalUnread();
});