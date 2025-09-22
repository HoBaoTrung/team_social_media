let stompClient = null;
let chatManager = null;
let isSubscribed = false;
function getCurrentUserId() {
    return (
        document.querySelector('meta[name="user-id"]')?.content ||
        document.querySelector('[data-user-id]')?.getAttribute('data-user-id') ||
        '0'
    );
}

class ChatManager {
    static openChats = new Map();
    constructor() {
        this.chatBubbles = new Map();
        this.conversationCache = new Map();
        this.pendingFiles = {};
        this.subscriptions = new Map(); // Track subscriptions per conversation
        this.currentPages = new Map();
        this.hasMore = new Map();
        this.isLoadingHistory = new Map();
        this.maxChats = 3;
        this.maxBubbles = 4;
        this.compactBubbleId = 'chat-bubble-compact';
        this.setupContainers();
        this.setupGlobalListeners();
    }

    setupContainers() {
        let bubbles = document.getElementById('chatBubblesContainer');
        if (!bubbles) {
            bubbles = document.createElement('div');
            bubbles.id = 'chatBubblesContainer';
            bubbles.className = 'chat-bubbles-container';
            document.body.appendChild(bubbles);
        }
        let windows = document.getElementById('chatWindowsContainer');
        if (!windows) {
            windows = document.createElement('div');
            windows.id = 'chatWindowsContainer';
            windows.className = 'chat-windows-container';
            document.body.appendChild(windows);
        }
    }

    setupGlobalListeners() {
        document.addEventListener('click', (e) => {
            const dd = document.getElementById('messageDropdown');
            const icon = document.getElementById('messageIcon');
            if (dd && icon && !dd.contains(e.target) && !icon.contains(e.target)) {
                dd.classList.remove('show');
            }
        });
    }

    // Method hỗ trợ để notify user mở chat
    notifyUserToOpenChat(participant, conversationId, message) {
        // Gửi thông báo qua WebSocket (nếu có kết nối)
        if (stompClient && stompClient.connected) {
            const notificationData = {
                type: 'EVERYONE_MENTION',
                conversationId: conversationId,
                participantId: participant.id,
                participantName: participant.fullName,
                message: message.substring(0, 100) + (message.length > 100 ? '...' : ''),
                senderName: this.getCurrentUserName() // Bạn cần implement method này
            };

            // Gửi qua topic chung hoặc queue riêng cho user
            stompClient.send("/app/everyoneMention", {}, JSON.stringify(notificationData));
        }
    }

    // Method hỗ trợ lấy tên user hiện tại
    getCurrentUserName() {
        // Lấy từ thông tin user hiện tại, có thể từ DOM hoặc biến global
        const nameElement = document.querySelector('[data-current-user-name]');
        if (nameElement) {
            return nameElement.textContent || nameElement.getAttribute('data-current-user-name');
        }

        // Fallback: lấy từ authentication principal nếu có
        return window.currentUserName || 'Người dùng';
    }



    async handleEveryoneMention(conversationId, message) {
        try {
            // Lấy danh sách participants
            const response = await fetch(`/api/chat/conversation/${conversationId}/participants`);
            const participants = await response.json();

            const currentUserId = getCurrentUserId();

            // Lọc ra những người khác (không phải người gửi)
            const otherParticipants = participants.filter(p => p.id != currentUserId);

            if (otherParticipants.length === 0) return;

            // Kiểm tra ai đang online và mở chat cho họ
            for (const participant of otherParticipants) {
                try {
                    // Kiểm tra trạng thái online
                    const statusResponse = await fetch(`/api/activity/status/${participant.id}`);
                    const statusData = await statusResponse.json();

                    if (statusData.online) {
                        // Nếu online, gửi notification để mở chat
                        this.notifyUserToOpenChat(participant, conversationId, message);
                    }
                } catch (error) {
                    console.error(`Error checking status for user ${participant.id}:`, error);
                }
            }

            console.log(`@everyone notification sent to ${otherParticipants.length} participants`);

        } catch (error) {
            console.error('Error handling @everyone mention:', error);
        }
    }

    async loadActivityStatus(conversationId) {
        try {
            // Lấy thông tin participants để tìm user khác (trong private chat)
            const response = await fetch(`/api/chat/conversation/${conversationId}/participants`);
            const participants = await response.json();

            const currentUserId = getCurrentUserId();
            const otherUser = participants.find(p => p.id != currentUserId);

            if (otherUser) {
                // Gọi API để lấy trạng thái activity
                const activityResponse = await fetch(`/api/activity/status/${otherUser.id}`);
                const activityData = await activityResponse.json();

                const statusEl = document.getElementById(`chat-status-${conversationId}`);
                if (statusEl) {
                    statusEl.textContent = activityData.lastActivity;
                    statusEl.className = activityData.online ? 'sub online' : 'sub offline';
                }
            }
        } catch (error) {
            console.error('Error loading activity status:', error);
            const statusEl = document.getElementById(`chat-status-${conversationId}`);
            if (statusEl) {
                statusEl.textContent = 'Không xác định';
            }
        }
    }

    // Thêm method để update activity status realtime
    updateActivityStatus(conversationId, isOnline, lastActivity) {
        const statusEl = document.getElementById(`chat-status-${conversationId}`);
        if (statusEl) {
            statusEl.textContent = lastActivity;
            statusEl.className = isOnline ? 'sub online' : 'sub offline';
        }
    }

    async openChat(targetKey, displayName, avatar, type = 'private') {
        let chatType = (type || 'private').toLowerCase();
        let domKey = String(targetKey);

        if (chatType === 'private' && this.conversationCache.has(`user:${domKey}`)) {
            const conv = this.conversationCache.get(`user:${domKey}`);
            domKey = String(conv.id);
        }

        if (ChatManager.openChats.has(domKey)) {
            const existing = ChatManager.openChats.get(domKey);
            existing.style.display = 'flex';
            if (this.chatBubbles.has(domKey)) {
                this.chatBubbles.get(domKey).remove();
                this.chatBubbles.delete(domKey);
                this.updateBubblesCompact();
            }
            return;
        }

        const visible = Array.from(ChatManager.openChats.values()).filter(c => c.style.display !== 'none');
        if (visible.length >= this.maxChats) {
            const oldest = visible[0];
            this.minimizeChat(oldest.id.replace('chat-', ''));
        }

        let convDto = null;
        if (chatType === 'private') {
            convDto = await this.findOrCreateConversation(domKey);
            if (!convDto) {
                this.toast('Không thể tạo cuộc trò chuyện', 'error');
                return;
            }
            this.conversationCache.set(String(convDto.id), convDto);
            this.conversationCache.set(`user:${targetKey}`, convDto);
            domKey = String(convDto.id);
        }

        const chatWin = this.createChatWindow(domKey, displayName, avatar, chatType, convDto);
        document.getElementById('chatWindowsContainer').appendChild(chatWin);
        ChatManager.openChats.set(domKey, chatWin);
        await this.loadHistory(domKey, 0, true);

        if (chatType === 'group') this.loadGroupParticipants(domKey);

        // Subscribe to the conversation topic for real-time messages
        this.subscribeToConversation(domKey);
    }

    async markConversationAsRead(conversationId) {
        try {
            await fetch(`/api/chat/mark-read/${conversationId}/${getCurrentUserId()}`, { method: 'POST' });
            await fetchTotalUnread(); // reload danh sách + badge
        } catch (e) {
            console.error('Error marking as read:', e);
        }
    }

    async openExistingConversation(conversationId, name, avatar, type) {
        const id = String(conversationId);
        if (ChatManager.openChats.has(id)) {
            const existing = ChatManager.openChats.get(id);
            existing.style.display = 'flex';
            if (this.chatBubbles.has(id)) {
                this.chatBubbles.get(id).remove();
                this.chatBubbles.delete(id);
                this.updateBubblesCompact();
            }
            return;
        }
        const visible = Array.from(ChatManager.openChats.values()).filter(c => c.style.display !== 'none');
        if (visible.length >= this.maxChats) {
            const oldest = visible[0];
            this.minimizeChat(oldest.id.replace('chat-', ''));
        }
        const chatWin = this.createChatWindow(id, name, avatar, (type || 'private'));
        document.getElementById('chatWindowsContainer').appendChild(chatWin);
        ChatManager.openChats.set(id, chatWin);
        setTimeout(() => this.loadHistory(id, 0, true), 100);
        if ((type || '').toLowerCase() === 'group') this.loadGroupParticipants(id);

        const badges = document.querySelectorAll(`.span-conversation-id-${conversationId}`);
        badges.forEach(badge => {
            badge.style.display = 'none';
        });

        // Subscribe to the conversation topic for real-time messages
        this.subscribeToConversation(id);
        await this.markConversationAsRead(id);
    }

    subscribeToConversation(conversationId) {
        if (!stompClient || !stompClient.connected) {
            console.warn('Stomp client not connected');
            return;
        }
        // Avoid duplicate subscriptions if already subscribed
        if (this.subscriptions.has(conversationId)) {
            console.log(`Already subscribed to conversation ${conversationId}`);
            return;
        }
        const sub = stompClient.subscribe(`/topic/conversation/${conversationId}`, (message) => {
            const messageData = JSON.parse(message.body);
            this.handleIncomingMessage(messageData);
        });
        this.subscriptions.set(conversationId, sub); // Store the subscription
    }

    async findOrCreateConversation(targetUserId) {
        try {
            const res = await fetch('/api/chat/find-or-create-conversation', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'},
                body: JSON.stringify({targetUserId: parseInt(targetUserId)})
            });
            const result = await res.json();
            return result.success ? result.conversation : null;
        } catch (e) {
            console.error(e);
            return null;
        }
    }

    createChatWindow(chatId, name, avatar, type = 'private') {
        const wrap = document.createElement('div');
        wrap.className = 'chat-window';
        wrap.id = `chat-${chatId}`;
        wrap.setAttribute('data-conversation-id', chatId);
        wrap.setAttribute('data-conversation-type', type);

        wrap.innerHTML = `
  <div class="chat-header ${type === 'group' ? 'is-group' : ''}">
    <div class="chat-user">
      <img class="chat-avatar ${type === 'group' ? 'group' : ''}" src="${avatar || (type === 'group' ? '/images/default-group-avatar.jpg' : '/images/default-avatar.jpg')}" alt="${name}">
      <div class="meta">
        <div class="name">${name || ''}</div>
        <div class="sub" id="chat-status-${chatId}">${type === 'group' ? 'Nhóm' : 'Đang kiểm tra...'}</div>
      </div>
    </div>
    <div class="chat-ctl"> 
      <button class="chat-btn" title="Thu nhỏ" onclick="chatManager.minimizeChat('${chatId}')"><i class="fa-solid fa-minus"></i></button>
      <button class="chat-btn" title="Đóng" onclick="chatManager.closeChat('${chatId}')"><i class="fa-solid fa-xmark"></i></button>
      ${type === 'group'
            ? `<button class="chat-btn" title="Đổi ảnh nhóm" onclick="chatManager.changeGroupAvatar('${chatId}')"><i class="fa-solid fa-image"></i></button>`
            : ``}
      <button class="chat-btn" title="Video call"  onclick="chatManager.toggleVideo('${chatId}')"><i class="fa-solid fa-video"></i></button>
     </div>
  </div>
  <div class="chat-messages" id="messages-${chatId}">
  </div>
  <div class="mention-suggestions" id="mentions-${chatId}" style="display:none"></div>
  <div class="chat-input">
    <div class="input-wrap">
      <textarea id="input-${chatId}" rows="1" placeholder="Nhập tin nhắn... ${type === 'group' ? '(Dùng @ để tag)' : ''}"
        data-chat-type="${type}"
        oninput="chatManager.handleInput(event, '${chatId}')"
        onkeydown="chatManager.handleKeyDown(event, '${chatId}')"
        onkeypress="chatManager.handleKeyPress(event,'${chatId}')"></textarea>
      <div class="input-icons">
        <i class="fa-regular fa-face-smile input-icon" onclick="chatManager.toggleEmoji('${chatId}')"></i>
        <i class="fa-solid fa-paperclip input-icon" onclick="chatManager.attachFile('${chatId}')"></i>
        <i class="fa-solid fa-paper-plane input-icon" onclick="chatManager.sendMessage('${chatId}')"></i>
      </div>
    </div>
    <div class="file-preview-box" id="preview-${chatId}"></div>
  </div>
  <emoji-picker id="emojiPicker-${chatId}" class="emoji-popup" style="display:none;"></emoji-picker>
`;

        // Setup scroll listener for infinite scrolling
        const messagesBox = wrap.querySelector(`#messages-${chatId}`);
        messagesBox.addEventListener('scroll', () => this.handleScroll(chatId));

        // Load activity status cho private chat
        if (type === 'private') {
            this.loadActivityStatus(chatId);
        }

        return wrap;
    }

    handleScroll(conversationId) {
        const box = document.getElementById(`messages-${conversationId}`);
        if (!box) return;

        if (this.isLoadingHistory.get(conversationId)) return;

        if (box.scrollTop < 100 && this.hasMore.get(conversationId)) {
            const page = (this.currentPages.get(conversationId) || 0) + 1;
            this.loadHistory(conversationId, page, false);
        }
    }

    minimizeChat(conversationId) {
        const id = String(conversationId);
        const win = ChatManager.openChats.get(id);
        if (!win) return;
        win.style.display = 'none';

        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble';
        bubble.dataset.id = id;
        bubble.onclick = () => this.restoreChat(id);

        const name = win.querySelector('.name')?.textContent || '';
        const avatar = win.querySelector('.chat-avatar')?.src || '/images/default-avatar.jpg';
        bubble.innerHTML = `<img src="${avatar}" alt="${name}"><div class="online-indicator"></div>`;

        document.getElementById('chatBubblesContainer').appendChild(bubble);
        this.chatBubbles.set(id, bubble);
        this.updateBubblesCompact();
    }

    restoreChat(conversationId) {
        const id = String(conversationId);
        const win = ChatManager.openChats.get(id);
        const bubble = this.chatBubbles.get(id);

        const visible = Array.from(ChatManager.openChats.values()).filter(c => c.style.display !== 'none');
        if (visible.length >= this.maxChats) {
            const oldest = visible[0];
            this.minimizeChat(oldest.id.replace('chat-', ''));
        }

        if (win) win.style.display = 'flex';
        if (bubble) {
            bubble.remove();
            this.chatBubbles.delete(id);
        }
        this.updateBubblesCompact();
    }

    closeChat(conversationId) {
        const id = String(conversationId);
        const win = ChatManager.openChats.get(id);
        if (win) {
            win.remove();
            ChatManager.openChats.delete(id);
        }
        const bubble = this.chatBubbles.get(id);
        if (bubble) {
            bubble.remove();
            this.chatBubbles.delete(id);
        }
        if (this.pendingFiles[id]) delete this.pendingFiles[id];

        // Unsubscribe from the conversation topic
        const sub = this.subscriptions.get(id);
        if (sub) {
            sub.unsubscribe();
            this.subscriptions.delete(id);
        }

        this.updateBubblesCompact();
    }

    updateBubblesCompact() {
        const container = document.getElementById('chatBubblesContainer');
        if (!container) return;

        const old = document.getElementById(this.compactBubbleId);
        if (old) old.remove();

        const bubbles = Array.from(container.querySelectorAll('.chat-bubble'));
        if (bubbles.length <= this.maxBubbles) return;

        bubbles.forEach((b, i) => {
            b.style.display = i < (bubbles.length - this.maxBubbles) ? 'none' : 'block';
        });

        const hiddenCount = bubbles.length - this.maxBubbles;
        const compact = document.createElement('div');
        compact.id = this.compactBubbleId;
        compact.className = 'chat-bubble compact';
        compact.textContent = `+${hiddenCount}`;
        compact.title = `${hiddenCount} cuộc trò chuyện`;
        compact.onclick = () => {
            bubbles.forEach(b => b.style.display = 'block');
            compact.remove();
        };
        container.insertBefore(compact, container.firstChild);
    }

    async sendMessage(chatId) {
        const input = document.getElementById(`input-${chatId}`);
        if (!input) return;
        const msg = (input.value || '').trim();

        // Lấy file pending
        const files = this.pendingFiles[chatId] || [];

        if (!msg && files.length === 0) return; // tránh gửi rỗng

        const formData = new FormData();
        formData.append("conversationId", chatId);
        formData.append("content", msg);

        files.forEach(f => formData.append("files", f));

        try {
            const res = await fetch('/api/chat/send-message', {
                method: 'POST',
                body: formData
            });
            if (!res.ok) {
                const result = await res.json();
                throw new Error(result.error || 'Unknown error');
            }
            const result = await res.json();
            if (!result.success) throw new Error(result.error);

            // Clear input + preview + pending
            input.value = '';
            delete this.pendingFiles[chatId];
            const previewBox = document.getElementById(`preview-${chatId}`);
            if (previewBox) previewBox.innerHTML = '';

            // XỬ LÝ @EVERYONE - THÊM PHẦN NÀY
            if (msg.includes('@everyone')) {
                await this.handleEveryoneMention(chatId, msg);
            }

        } catch (e) {
            console.error(e);
            this.toast('Không thể gửi tin nhắn: ' + e.message, 'error');
        }
    }

    async loadHistory(conversationId, page, isInitial = false) {
        if (isInitial) {
            this.currentPages.set(conversationId, 0);
            this.hasMore.set(conversationId, false);
            this.isLoadingHistory.set(conversationId, false);
        } else {
            if (this.isLoadingHistory.get(conversationId)) return;
            this.isLoadingHistory.set(conversationId, true);
        }

        const box = document.getElementById(`messages-${conversationId}`);
        if (!box) {
            if (!isInitial) this.isLoadingHistory.set(conversationId, false);
            return;
        }

        let loader = null;
        if (!isInitial) {
            loader = document.createElement('div');
            loader.className = 'message-loader';
            loader.textContent = 'Đang tải thêm...';
            box.insertBefore(loader, box.firstChild);
        }

        const oldScrollHeight = box.scrollHeight;

        try {
            const res = await fetch(`/api/chat/conversation/${conversationId}/messages?page=${page}&size=20`);
            if (!res.ok) throw new Error('Failed to load history');
            const messages = await res.json();

            if (isInitial) {
                box.innerHTML = '';
            }

            const fragment = document.createDocumentFragment();
            const me = String(getCurrentUserId());

            for (let i = messages.length - 1; i >= 0; i--) {
                const m = messages[i];
                const type = String(m.senderId) === me ? 'sent' : 'received';
                const sender = type === 'received' ? {name: m.senderName, avatar: m.senderAvatar} : null;
                const row = this.createMessageRow(m, type, sender);
                fragment.appendChild(row);
            }

            if (isInitial) {
                box.appendChild(fragment);
                box.scrollTop = box.scrollHeight;
            } else {
                box.insertBefore(fragment, loader.nextSibling);
                const newScrollHeight = box.scrollHeight;
                box.scrollTop += (newScrollHeight - oldScrollHeight);
            }

            this.hasMore.set(conversationId, messages.length === 20);
            if (!isInitial) {
                this.currentPages.set(conversationId, page);
            }
        } catch (e) {
            console.error(e);
            this.toast('Lỗi tải lịch sử chat', 'error');
        } finally {
            if (loader) loader.remove();
            this.isLoadingHistory.set(conversationId, false);
        }
    }

    createMessageRow(message, type, sender = null) {
        const row = document.createElement('div');
        row.className = `message ${type}`;
        const el = this.renderMessage(message);

        if (type === 'received' && sender) {
            row.innerHTML = `
        <img class="message-avatar" src="${sender.avatar || '/images/default-avatar.jpg'}" alt="${sender.name}">
        <div class="message-content" >
            <div class="message-sender">${sender.name}</div>
            ${el}
        </div>`;
        } else {
            row.innerHTML = `
        <div class="message-content">
            ${el}
        </div>`;
        }

        return row;
    }

    addMessageToUI(conversationId, message, type, sender = null) {
        const box = document.getElementById(`messages-${conversationId}`);
        if (!box) return;

        const row = this.createMessageRow(message, type, sender);
        box.appendChild(row);
        box.scrollTop = box.scrollHeight;
    }

    renderMessage(message) {
        let html = '';

        // Render attachments (multiple supported)
        (message.attachments || []).forEach(att => {
            switch (att.type) {
                case "IMAGE":
                    html += `<img src="${att.attachmentUrl}" alt="image" class="message-image">`;
                    break;
                case "VIDEO":
                    html += `<video src="${att.attachmentUrl}" controls class="message-video"></video>`;
                    break;
                case "AUDIO":
                    html += `<audio controls src="${att.attachmentUrl}" class="message-audio"></audio>`;
                    break;
                case "FILE":
                    const ext = att.attachmentUrl.split('.').pop().toLowerCase();
                    html += `
                    <div class="message-file">
                          <a href="${att.attachmentUrl}" download="${att.fileName}" class="file-link">
                            <div class="file-icon ${ext}"></div>
                            <div class="file-info">
                              <div class="file-name">${att.fileName}</div>
                              <div class="file-size">${att.fileSize}</div>
                            </div>
                          </a>
                        </div>
                                `;
                    break;
                default:
                    html += `<div class="message-unknown">[Unsupported attachment]</div>`;
            }
        });

        // If CALL type without attachments/content
        if (message.type === "CALL" && !html) {
            html = `<div class="message-call"> Cuộc gọi: ${message.content || 'Không xác định'}</div>`;
        }

        // Render content if present (TEXT or CALL description)
        if (message.content) {
            html += `<div class="message-text">${ChatManager.processMentions(message.content)}</div>`;
        }

        return html || `<div class="message-unknown">[Empty message]</div>`;
    }

    toggleVideo(conversation_id) {
        const url = `/video_call/${conversation_id}`;
        window.open(url, `VideoPopup${conversation_id}`, 'width=1070,height=600,resizable=yes,scrollbars=no');

        // Send startCall to trigger invites (no need for isCaller here)
        stompClient.send("/app/startCall", {}, JSON.stringify({
            conversationId: conversation_id
        }));
    }

    toggleEmoji(chatId) {
        const picker = document.getElementById(`emojiPicker-${chatId}`);
        picker.style.display = picker.style.display === 'none' ? 'block' : 'none';

        // Chỉ gắn listener 1 lần
        if (!picker.dataset.bound) {
            picker.addEventListener('emoji-click', (event) => {
                const emoji = event.detail.unicode;
                const textarea = document.getElementById(`input-${chatId}`);

                // Chèn vào đúng vị trí con trỏ
                const start = textarea.selectionStart;
                const end = textarea.selectionEnd;
                const text = textarea.value;

                textarea.value = text.slice(0, start) + emoji + text.slice(end);
                textarea.selectionStart = textarea.selectionEnd = start + emoji.length;
                textarea.focus();
            });
            picker.dataset.bound = "true";
        }
    }

    async attachFile(chatId) {
        const input = document.createElement('input');
        input.type = 'file';
        input.multiple = true;
        input.accept = 'image/*,video/*,.pdf,.doc,.docx';
        input.onchange = (e) => {
            const files = Array.from(e.target.files);
            if (!files.length) return;
            if (files.length > 10) {
                this.toast('Tối đa 10 files mỗi lần gửi', 'error');
                return;
            }

            if (!this.pendingFiles[chatId]) {
                this.pendingFiles[chatId] = [];
            }
            this.pendingFiles[chatId].push(...files);

            // Hiển thị preview
            const previewBox = document.getElementById(`preview-${chatId}`);
            if (previewBox) {
                previewBox.innerHTML = '';

                files.forEach((f, index) => {
                    const ext = f.name.split('.').pop().toLowerCase();

                    // Tạo khối preview
                    const div = document.createElement("div");
                    div.className = "file-preview";

                    let previewEl;

                    if (f.type.startsWith("image")) {
                        previewEl = document.createElement("img");
                        previewEl.src = URL.createObjectURL(f);
                        previewEl.className = "preview-thumb";
                    } else if (f.type.startsWith("video")) {
                        previewEl = document.createElement("video");
                        previewEl.src = URL.createObjectURL(f);
                        previewEl.className = "preview-video";
                        previewEl.controls = true;
                    } else if (f.type.startsWith("audio")) {
                        previewEl = document.createElement("audio");
                        previewEl.src = URL.createObjectURL(f);
                        previewEl.className = "preview-audio";
                        previewEl.controls = true;
                    } else {
                        let iconPath = "/icons/file.png";
                        if (["pdf"].includes(ext)) iconPath = "/icons/pdf.png";
                        else if (["doc", "docx"].includes(ext)) iconPath = "/icons/word.png";
                        else if (["xls", "xlsx"].includes(ext)) iconPath = "/icons/excel.png";
                        else if (["zip", "rar", "7z"].includes(ext)) iconPath = "/icons/zip.png";

                        previewEl = document.createElement("img");
                        previewEl.src = iconPath;
                        previewEl.className = "file-icon";
                    }

                    const info = document.createElement("span");
                    info.innerText = `${f.name} (${(f.size / 1024).toFixed(1)} KB)`;

                    const removeBtn = document.createElement("button");
                    removeBtn.className = "remove-btn";
                    removeBtn.innerText = "";
                    removeBtn.title = "Xóa file này";
                    removeBtn.onclick = () => {
                        files.splice(index, 1);
                        div.remove();
                    };

                    div.appendChild(previewEl);
                    div.appendChild(info);
                    div.appendChild(removeBtn);
                    previewBox.appendChild(div);
                });
            }
        };
        input.click();
    }

    loadOnlineFriends() {
        const el = document.getElementById('onlineFriendsList');
        if (!el) return;

        fetch('/api/chat/online-friends')
            .then(r => r.json())
            .then(data => {
                const friends = data;
                if (!Array.isArray(friends)) {
                    console.error('Invalid API response:', data);
                    el.innerHTML = `<div class="text-center text-danger p-3">Dữ liệu không hợp lệ</div>`;
                    return;
                }

                if (friends.length === 0) {
                    el.innerHTML = `<div class="text-center p-3 text-muted">Không có bạn bè online</div>`;
                    return;
                }

                el.innerHTML = friends.map(f => {
                    const spanStyle = f.unreadCount && f.unreadCount > 0 ? '' : 'display: none;';
                    return `<div class="friend-item-enhanced" 
                   onclick="chatManager.openExistingConversation('${f.id}', '${this.escape(f.name)}', '${f.avatar || '/images/default-avatar.jpg'}', '${f.type || 'private'}')">
                <div style="position:relative">
                  <img src="${f.avatar || '/images/default-avatar.jpg'}" class="friend-avatar">
                  ${(f.isOnline || f.online) ? '<div class="online-indicator"></div>' : ''}
                </div>
                <span class="friend-name">${this.escape(f.name)}</span>
                <span class="badge bg-danger ms-2 span-conversation-id-${f.id}" style="${spanStyle}">
                    ${f.unreadCount || ''}
                </span>
            </div>`;
                }).join('');
            })
            .catch(error => {
                console.error('Error fetching online friends:', error);
                el.innerHTML = `<div class="text-center text-danger p-3">Lỗi tải danh sách</div>`;
            });
    }

    handleInput(evt, convId) {
        const ta = evt.target;
        this.autoResize(ta);
        if ((ta.dataset.chatType || 'private') !== 'group') return;

        const val = ta.value;
        const cursor = ta.selectionStart;
        const before = val.substring(0, cursor);
        const match = before.match(/@(\w*)$/);
        if (match) {
            this._mentionStart = before.lastIndexOf('@');
            this._mentionQuery = match[1];
            this.showMentionSuggestions(convId, this._mentionQuery);
        } else {
            this.hideMentionSuggestions(convId);
            this._mentionStart = -1;
        }
    }

    handleKeyDown(evt, convId) {
        const box = document.getElementById(`mentions-${convId}`);
        if (!box || box.style.display !== 'block') {
            // Nếu không có mention box, xử lý Enter bình thường
            if (evt.key === 'Enter' && !evt.shiftKey) {
                evt.preventDefault();
                this.sendMessage(convId);
            }
            return;
        }

        const items = box.querySelectorAll('.mention-item');
        if (!items.length) return;

        let idx = Array.from(items).findIndex(x => x.classList.contains('selected'));

        switch (evt.key) {
            case 'ArrowDown':
                evt.preventDefault();
                idx = (idx + 1) % items.length; // Cycle through items
                this._selectMentionItem(items, idx);
                break;
            case 'ArrowUp':
                evt.preventDefault();
                idx = idx <= 0 ? items.length - 1 : idx - 1; // Cycle through items
                this._selectMentionItem(items, idx);
                break;
            case 'Tab':
            case 'Enter':
                evt.preventDefault();
                if (idx >= 0) {
                    this.selectMention(convId, items[idx]);
                } else {
                    // Nếu không có item nào được select, chọn item đầu tiên
                    this.selectMention(convId, items[0]);
                }
                break;
            case 'Escape':
                evt.preventDefault();
                this.hideMentionSuggestions(convId);
                break;
        }
    }

    handleKeyPress(evt, convId) {
        if (evt.key === 'Enter' && !evt.shiftKey) {
            evt.preventDefault();
            this.sendMessage(convId);
        }
    }

    async loadGroupParticipants(conversationId) {
        try {
            const res = await fetch(`/api/chat/conversation/${conversationId}/participants`);
            const arr = await res.json();
            this._mentionPool = this._mentionPool || new Map();
            this._mentionPool.set(conversationId, arr || []);
            const sub = document.querySelector(`#chat-${conversationId} .sub`);
            if (sub) sub.textContent = `${(arr || []).length} thành viên`;
        } catch {
        }
    }

    showMentionSuggestions(convId, query) {
        const pool = (this._mentionPool && this._mentionPool.get(convId)) || [];
        if (!pool.length) {
            this.loadGroupParticipants(convId);
            return;
        }

        // Lấy thông tin user hiện tại
        const currentUserId = getCurrentUserId();
        const currentUser = pool.find(p => p.id == currentUserId);

        let filtered = [];

        // Nếu query rỗng, chỉ hiện @everyone và các thành viên khác (KHÔNG bao gồm bản thân)
        if (!query || query.trim() === '') {
            // Thêm @everyone option
            filtered.push({
                id: 'everyone',
                username: 'everyone',
                fullName: 'Tất cả thành viên',
                avatar: '/images/group-icon.png',
                role: 'SPECIAL',
                isSpecial: true
            });

            // Chỉ thêm các thành viên khác (LOẠI TRỪ bản thân)
            filtered.push(...pool.filter(p => p.id != currentUserId));
        } else {
            // Tìm kiếm theo query
            const lowerQuery = query.toLowerCase();

            // Kiểm tra @everyone
            if ('everyone'.includes(lowerQuery) || 'tất cả'.includes(lowerQuery)) {
                filtered.push({
                    id: 'everyone',
                    username: 'everyone',
                    fullName: 'Tất cả thành viên',
                    avatar: '/images/group-icon.png',
                    role: 'SPECIAL',
                    isSpecial: true
                });
            }

            // Tìm kiếm trong danh sách thành viên (LOẠI TRỪ bản thân trừ khi cố tình tìm)
            const memberResults = pool.filter(p => {
                const matchesQuery = (p.fullName || '').toLowerCase().includes(lowerQuery) ||
                    (p.username || '').toLowerCase().includes(lowerQuery);

                // Nếu user cố tình gõ tên mình thì mới hiện
                const isSearchingForSelf = currentUser && (
                    (currentUser.fullName || '').toLowerCase().includes(lowerQuery) ||
                    (currentUser.username || '').toLowerCase().includes(lowerQuery)
                ) && p.id == currentUserId;

                return matchesQuery && (p.id != currentUserId || isSearchingForSelf);
            });

            // Sắp xếp: nếu có bản thân (do cố tình tìm) thì lên đầu, sau đó theo tên
            memberResults.sort((a, b) => {
                if (a.id == currentUserId && b.id != currentUserId) return -1;
                if (b.id == currentUserId && a.id != currentUserId) return 1;
                return (a.fullName || '').localeCompare(b.fullName || '');
            });

            filtered.push(...memberResults);
        }

        if (!filtered.length) {
            this.hideMentionSuggestions(convId);
            return;
        }

        const el = document.getElementById(`mentions-${convId}`);
        el.innerHTML = filtered.map((p, i) => {
            const displayName = p.isSpecial ? p.fullName :
                (p.isCurrentUser ? p.fullName : p.fullName);
            const usernameDisplay = p.isSpecial ? '@everyone' : `@${p.username || ''}`;
            const avatarSrc = p.avatar || '/images/default-avatar.jpg';
            const specialClass = p.isSpecial ? 'mention-special' : '';
            const currentUserClass = p.isCurrentUser ? 'mention-current-user' : '';

            return `
        <div class="mention-item ${i === 0 ? 'selected' : ''} ${specialClass} ${currentUserClass}" 
             data-username="${p.username}" 
             data-is-special="${p.isSpecial || false}"
             onclick="chatManager.selectMention('${convId}', this)">
            <img class="mention-avatar" src="${avatarSrc}">
            <div class="mention-info">
                <div class="mention-name">${this.escape(displayName)}</div>
                <div class="mention-username">${this.escape(usernameDisplay)}</div>
            </div>
            ${p.role === 'ADMIN' ? '<i class="fa-solid fa-crown mention-admin"></i>' : ''}
            ${p.isSpecial ? '<i class="fa-solid fa-users mention-special-icon"></i>' : ''}
        </div>
        `;
        }).join('');

        el.style.display = 'block';
    }

    _selectMentionItem(items, idx) {
        items.forEach((it, i) => it.classList.toggle('selected', i === idx));
    }

    selectMention(convId, el) {
        const ta = document.getElementById(`input-${convId}`);
        const username = el.getAttribute('data-username');
        const isSpecial = el.getAttribute('data-is-special') === 'true';

        const val = ta.value;
        const before = val.substring(0, this._mentionStart);
        const after = val.substring(ta.selectionStart);

        let mention;
        if (isSpecial && username === 'everyone') {
            mention = `@everyone `;
        } else {
            mention = `@${username} `;
        }

        ta.value = before + mention + after;
        const pos = before.length + mention.length;
        ta.setSelectionRange(pos, pos);
        this.hideMentionSuggestions(convId);
        ta.focus();
        this.autoResize(ta);
    }

    hideMentionSuggestions(convId) {
        const el = document.getElementById(`mentions-${convId}`);
        if (el) el.style.display = 'none';
    }

    static processMentions(text) {
        return String(text || '').replace(/@(\w+)/g, (_, u) => `<span class="mention-tag">@${u}</span>`);
    }

    changeGroupAvatar(conversationId) {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.onchange = async (e) => {
            const f = e.target.files?.[0];
            if (!f) return;
            const form = new FormData();
            form.append('avatar', f);
            try {
                const res = await fetch(`/api/chat/conversation/${conversationId}/avatar`, {
                    method: 'POST',
                    body: form
                });
                const result = await res.json();
                if (result.success) {
                    const img = document.querySelector(`#chat-${conversationId} .chat-avatar`);
                    if (img) img.src = result.avatarUrl;
                    this.toast('Cập nhật ảnh nhóm thành công!', 'success');
                } else this.toast(result.error || 'Không thể cập nhật ảnh nhóm', 'error');
            } catch {
                this.toast('Lỗi cập nhật ảnh nhóm', 'error');
            }
        };
        input.click();
    }

    autoResize(ta) {
        ta.style.height = 'auto';
        ta.style.height = Math.min(ta.scrollHeight, 100) + 'px';
    }

    escape(s) {
        return String(s || '')
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    toast(msg, type = 'info') {
        const bg = type === 'success' ? '#28a745' : type === 'error' ? '#dc3545' : '#1877f2';
        const el = document.createElement('div');
        el.style.cssText = `position:fixed;top:80px;right:20px;background:${bg};color:#fff;padding:10px 14px;border-radius:8px;z-index:13000;box-shadow:0 8px 24px rgba(0,0,0,.15)`;
        el.textContent = msg;
        document.body.appendChild(el);
        setTimeout(() => el.remove(), 2500);
    }

    handleIncomingMessage(payload) {
        const id = String(payload.conversationId);
        const me = String(getCurrentUserId());
        const mine = String(payload.senderId) === me;
        if (ChatManager.openChats.has(id)) {
            if (mine) {
                this.addMessageToUI(id, payload, 'sent');
            } else {
                this.addMessageToUI(id, payload, 'received', {
                    name: payload.senderName,
                    avatar: payload.senderAvatar
                });
                this.markConversationAsRead(id);
            }
        }
    }

    handleUnreadMessage(data) {
        const checkOpen = () => {
            if (!ChatManager.openChats.has(String(data.conversationId))) {
                updateMessageBadge(data.totalUnread);
                document.querySelectorAll(`.span-conversation-id-${data.conversationId}`).forEach(el => {
                    el.style.display = 'block';
                    el.textContent = data.unreadCount;
                });
            }
        };
        checkOpen();
    }
}

function bindHeaderDropdown() {
    const icon = document.getElementById('messageIcon');
    const dropdown = document.getElementById('messageDropdown');
    const list = document.getElementById('conversationList');
    if (!icon || !dropdown || !list) return;
    let loaded = false;

    const openDropdown = async () => {
        dropdown.classList.add('show');
        const rect = icon.getBoundingClientRect();
        dropdown.style.top = `${rect.bottom + 8 + window.scrollY}px`;
        dropdown.style.right = `${Math.max(16, window.innerWidth - rect.right)}px`;

        if (!loaded) {
            list.innerHTML = `<div class="text-center p-3"><i class="fa-solid fa-spinner fa-spin"></i> Đang tải...</div>`;
            try {
                const r = await fetch('/api/conversations', {headers: {'X-Requested-With': 'XMLHttpRequest'}});
                const data = await r.json();
                if (!Array.isArray(data) || data.length === 0) {
                    list.innerHTML = `<div class="text-center text-muted p-3">Chưa có cuộc trò chuyện</div>`;
                    loaded = true;
                    return;
                }

                list.innerHTML = data.map(c => `
      <div class="conversation-item" data-id="${c.id}" data-type="${c.type}">
        <img src="${c.avatar || (String(c.type).toLowerCase() === 'group' ? '/images/default-group-avatar.jpg' : '/images/default-avatar.jpg')}" alt="">
        <div class="info">
          <div class="conv-name">${c.name || ''}</div>
          <div class="conv-sub">${c.lastMessage ? c.lastMessage : 'Chưa có tin nhắn'}${c.timeAgo ? ' · ' + c.timeAgo : ''}</div>
        </div>
        ${c.hasUnread ? `<span class="badge bg-danger">${c.unreadCount || ''}</span>` : ''}
      </div>`).join('');

                list.querySelectorAll('.conversation-item').forEach(el => {
                    el.addEventListener('click', () => {
                        const id = el.getAttribute('data-id');
                        const type = el.getAttribute('data-type');
                        const name = el.querySelector('.conv-name')?.textContent?.trim() || '';
                        const avatar = el.querySelector('img')?.src || '';
                        dropdown.classList.remove('show');
                        if (chatManager) chatManager.openExistingConversation(id, name, avatar, type);
                    });
                });
                loaded = true;
            } catch (e) {
                console.error(e);
                list.innerHTML = `<div class="text-center text-danger p-3"><i class="fa-solid fa-triangle-exclamation me-1"></i> Lỗi tải danh sách</div>`;
            }
        }
    };

    const closeDropdown = () => dropdown.classList.remove('show');

    icon.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (dropdown.classList.contains('show')) closeDropdown();
        else openDropdown();
    });

    icon.setAttribute('tabindex', '0');
    icon.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            if (dropdown.classList.contains('show')) closeDropdown();
            else openDropdown();
        }
    });

    document.addEventListener('click', (e) => {
        if (!dropdown.classList.contains('show')) return;
        if (!dropdown.contains(e.target) && !icon.contains(e.target)) closeDropdown();
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeDropdown();
    });

    document.querySelectorAll("emoji-picker").forEach(picker => {
        picker.addEventListener("emoji-click", event => {
            const emoji = event.detail.unicode;
            const input = picker.closest(".chat-input").querySelector("textarea");
            input.value += emoji;

            // Tự resize textarea nếu cần
            input.dispatchEvent(new Event("input"));
        });
    });
}

// Nâng cao (để mở rộng sau): tạo nhóm qua modal, tìm user… (đang dùng các API có sẵn)
class EnhancedChatManager extends ChatManager {
}
function connectStompClient() {
    if (stompClient && stompClient.connected) return;
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
        console.log("Global Stomp connected");
        if (isSubscribed) return;
        isSubscribed = true;

        // ✅ Subscribe unread message
        stompClient.subscribe("/user/queue/unread", (message) => {
            if (chatManager) {
                chatManager.handleUnreadMessage(JSON.parse(message.body));
            }
        });

        // ✅ Subscribe call invite
        stompClient.subscribe("/user/queue/call-invite", (message) => {
            console.log("Received call invite:", message.body);
            const invite = JSON.parse(message.body);
            const url = `/video_call/${invite.conversationId}?isIncoming=true&callerId=${invite.callerId}`;
            window.open(
                url,
                `VideoPopup${invite.conversationId}`,
                'width=1070,height=600,resizable=yes,scrollbars=no'
            );
        });

        // ✅ Subscribe auto-open chat notifications
        stompClient.subscribe("/user/queue/auto-open-chat", (message) => {
            const data = JSON.parse(message.body);
            handleAutoOpenChat(data);
        });

        // ✅ Thêm mới: Subscribe @everyone mention
        stompClient.subscribe("/user/queue/everyone-mention", (message) => {
            const data = JSON.parse(message.body);
            handleEveryoneMentionNotification(data);
        });

    }, (error) => {
        console.error("Stomp connection error:", error);
    });
}

function handleAutoOpenChat(data) {
    console.log("Auto-opening chat for mention:", data);

    if (!data.conversationId) {
        console.error("No conversationId provided for auto-open chat");
        return;
    }

    // Show mention notification first
    showMentionNotification({
        sender: {
            username: data.mentionedBy || 'Someone',
            avatarUrl: data.mentionedByAvatar || '/images/default-avatar.jpg'
        },
        referenceId: data.conversationId
    });

    // Auto-open the chat after a short delay
    setTimeout(() => {
        openMentionChat(data.conversationId);
    }, 1000); // 1 second delay to let user see the notification
}


function showMentionNotification(notification) {
    const toastHtml = `
        <div class="toast mention-toast" role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="8000">
            <div class="toast-header">
                <img src="${notification.sender.avatarUrl || '/images/default-avatar.jpg'}" 
                     class="rounded me-2" width="20" height="20" alt="Avatar">
                <strong class="me-auto">${notification.sender.username}</strong>
                <small class="text-muted">vừa xong</small>
                <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                <i class="fas fa-at text-primary"></i> 
                Đã nhắc đến bạn trong nhóm chat
                <div class="mt-2 d-flex gap-2">
                    <button class="btn btn-sm btn-primary flex-fill" onclick="openMentionChat(${notification.referenceId})">
                        <i class="fas fa-comment"></i> Mở chat
                    </button>
                    <button class="btn btn-sm btn-outline-secondary" onclick="dismissMentionNotification(this)">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            </div>
        </div>
    `;

    let toastContainer = document.getElementById('mention-toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'mention-toast-container';
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        toastContainer.style.zIndex = '12000';
        document.body.appendChild(toastContainer);
    }

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = toastContainer.lastElementChild;
    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}

function dismissMentionNotification(button) {
    const toast = button.closest('.toast');
    const bsToast = bootstrap.Toast.getOrCreateInstance(toast);
    bsToast.hide();
}

async function openMentionChat(conversationId) {
    try {
        // Get conversation details first
        const conversationResponse = await fetch(`/api/conversations`);
        const conversations = await conversationResponse.json();

        const conversation = conversations.find(c => c.id == conversationId);

        if (conversation && chatManager) {
            // Auto-open the conversation
            chatManager.openExistingConversation(
                conversationId,
                conversation.name,
                conversation.avatar,
                conversation.type || 'group'
            );

            console.log(`Auto-opened chat: ${conversation.name}`);
        } else {
            console.error(`Conversation ${conversationId} not found`);

            // Fallback: try to get participants to determine conversation name
            const participantsResponse = await fetch(`/api/chat/conversation/${conversationId}/participants`);
            const participants = await participantsResponse.json();

            if (participants && participants.length > 0 && chatManager) {
                const conversationName = participants.length > 2
                    ? `Nhóm ${participants.length} thành viên`
                    : participants[0].fullName;

                chatManager.openExistingConversation(
                    conversationId,
                    conversationName,
                    '/images/default-group-avatar.jpg',
                    participants.length > 2 ? 'group' : 'private'
                );
            }
        }

        // Close mention notifications
        document.querySelectorAll('.mention-toast').forEach(toast => {
            const bsToast = bootstrap.Toast.getOrCreateInstance(toast);
            bsToast.hide();
        });

    } catch (error) {
        console.error('Error auto-opening chat:', error);
    }
}


async function openChat(userId, name, avatar) {
    try {
        const response = await fetch('/api/chat/find-or-create-conversation', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({targetUserId: userId})
        });
        const data = await response.json();

        if (data.success && data.conversation) {
            const convID = data.conversation.id;
            if (chatManager) {
                chatManager.openExistingConversation(convID, name, avatar, 'private');
            }
        } else {
            console.error("Không thể mở chat:", data.error);
        }
    } catch (err) {
        console.error("Lỗi khi mở chat:", err);
    }
}

function handleEveryoneMentionNotification(data) {
    console.log("Received @everyone mention:", data);

    // Show notification
    showEveryoneMentionNotification(data);

    // Auto-open chat if user wants
    setTimeout(() => {
        if (confirm(`${data.senderName} đã nhắc @everyone trong "${data.conversationName}". Mở chat ngay?`)) {
            openEveryoneMentionChat(data.conversationId, data.conversationName);
        }
    }, 1000);
}

function showEveryoneMentionNotification(data) {
    const toastHtml = `
        <div class="toast everyone-mention-toast" role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="10000">
            <div class="toast-header">
                <img src="${data.senderAvatar || '/images/default-avatar.jpg'}" 
                     class="rounded me-2" width="20" height="20" alt="Avatar">
                <strong class="me-auto">${data.senderName}</strong>
                <small class="text-muted">@everyone</small>
                <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                <i class="fas fa-users text-warning"></i> 
                Đã nhắc @everyone trong "${data.conversationName}"
                <div class="mt-2">
                    <small class="text-muted">${data.message}</small>
                </div>
                <div class="mt-2 d-flex gap-2">
                    <button class="btn btn-sm btn-primary flex-fill" onclick="openEveryoneMentionChat(${data.conversationId}, '${data.conversationName}')">
                        <i class="fas fa-comment"></i> Mở chat
                    </button>
                    <button class="btn btn-sm btn-outline-secondary" onclick="dismissEveryoneMentionNotification(this)">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            </div>
        </div>
    `;

    let toastContainer = document.getElementById('everyone-mention-toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'everyone-mention-toast-container';
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        toastContainer.style.zIndex = '12000';
        document.body.appendChild(toastContainer);
    }

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = toastContainer.lastElementChild;
    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}

async function openEveryoneMentionChat(conversationId, conversationName) {
    try {
        if (chatManager) {
            // Get conversation details
            const response = await fetch(`/api/conversations`);
            const conversations = await response.json();
            const conversation = conversations.find(c => c.id == conversationId);

            if (conversation) {
                chatManager.openExistingConversation(
                    conversationId,
                    conversation.name || conversationName,
                    conversation.avatar,
                    conversation.type || 'group'
                );
            } else {
                // Fallback
                chatManager.openExistingConversation(
                    conversationId,
                    conversationName,
                    '/images/default-group-avatar.jpg',
                    'group'
                );
            }
        }

        // Close all @everyone notifications
        document.querySelectorAll('.everyone-mention-toast').forEach(toast => {
            const bsToast = bootstrap.Toast.getOrCreateInstance(toast);
            bsToast.hide();
        });

    } catch (error) {
        console.error('Error opening @everyone chat:', error);
    }
}

function dismissEveryoneMentionNotification(button) {
    const toast = button.closest('.toast');
    const bsToast = bootstrap.Toast.getOrCreateInstance(toast);
    bsToast.hide();
}

document.addEventListener('DOMContentLoaded', () => {
    if (!chatManager) {
        window.chatManager = chatManager = new EnhancedChatManager();
    }
    connectStompClient();
    if (document.getElementById('onlineFriendsList')) chatManager.loadOnlineFriends();
    bindHeaderDropdown();

    // **THÊM MỚI: CSS cho mention toast**
    const mentionToastStyles = `
        <style>
        .mention-toast {
            min-width: 300px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            border-left: 4px solid #1877f2;
        }
        .mention-toast .toast-header {
            background-color: #f8f9fa;
            border-bottom: 1px solid #dee2e6;
        }
        .mention-toast .toast-body {
            background-color: white;
        }
        #mention-toast-container {
            z-index: 12000 !important;
            top: 80px !important; /* Dưới header */
        }
        </style>
    `;


    const mentionNotificationStyles = `
        <style>
       .mention-toast {
            min-width: 320px;
            max-width: 400px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            border-left: 4px solid #1877f2;
            animation: slideInFromRight 0.3s ease-out;
        }

        @keyframes slideInFromRight {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }

        .mention-toast .toast-header {
            background-color: #f8f9fa;
            border-bottom: 1px solid #dee2e6;
        }
        
        .mention-toast .toast-body {
            background-color: white;
        }
        
        #mention-toast-container {
            z-index: 12000 !important;
            top: 80px !important;
        }

        </style>
`;

    // Thêm styles vào head
    if (!document.getElementById('mention-toast-styles')) {
        const styleEl = document.createElement('style');
        styleEl.id = 'mention-toast-styles';
        styleEl.innerHTML = mentionToastStyles.replace(/<\/?style>/g, ''); // Remove style tags
        document.head.appendChild(styleEl);
    }

    if (!document.getElementById('mention-notification-styles')) {
        const styleEl = document.createElement('style');
        styleEl.id = 'mention-notification-styles';
        styleEl.innerHTML = mentionNotificationStyles;
        document.head.appendChild(styleEl);
    }
});

window.openChat = openChat;