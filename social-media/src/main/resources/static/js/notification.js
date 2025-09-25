import {formatTimeAgo} from '/js/timeUtils.js';

// ---------- Config ----------
const API_BASE = '/api/notifications';
const PAGE_SIZE = 20;
const DROPDOWN_LIST_ID = 'notificationList';
const BADGE_ID = 'notificationCount';
const DROPDOWN_TOGGLE_ID = 'notificationDropdown';
const WS_ENDPOINT = '/ws';
const STOMP_QUEUE = '/user/queue/notifications';

// ---------- State ----------
let stompClient = null;
let socket = null;
let currentPage = 0;
let loadingPage = false;
let noMore = false;
let loadedOnce = false;
let unreadCount = 0;

// ---------- Init on DOM ready ----------
document.addEventListener('DOMContentLoaded', () => {
    connectWebSocket();

    const dropdownToggle = document.getElementById(DROPDOWN_TOGGLE_ID);
    if (dropdownToggle) {
        // Load initial notifications when dropdown shown (lazy load)
        dropdownToggle.addEventListener('show.bs.dropdown', async () => {
            if (!loadedOnce) {
                await loadInitialNotifications();
                loadedOnce = true;
            }
        });
    }

    // click handler delegation: mark single notification read and follow link
    const list = document.getElementById(DROPDOWN_LIST_ID);
    if (list) {
        list.addEventListener('click', onNotificationClick);
        // infinite scroll: load more when near bottom
        list.addEventListener('scroll', onDropdownScroll);
    }

    // load unread count on start (best-effort, doesn't block)
    refreshUnreadCount().catch(() => {
    });
});


function setBadge(n) {

    const el = document.getElementById(BADGE_ID);

    if (!el) return;
    if (!n || n <= 0) {
        el.style.display = 'none';
        el.innerText = '0';
    } else {
        el.style.display = '';
        el.innerText = n > 99 ? '99+' : String(n);
    }
}

function increaseBadge(by = 1) {

    unreadCount = Math.max(0, (unreadCount || 0) + by);
    setBadge(unreadCount);
}

function decreaseBadge(by = 1) {
    unreadCount = Math.max(0, (unreadCount || 0) - by);
    setBadge(unreadCount);
}

function buildLink(n) {
    if (!n) return '#';

    switch ((n.referenceType || '').toUpperCase()) {
        case 'POST':
            // Kiểm tra nếu là mention notification thì có thể là chat mention
            if (n.notificationType === 'MENTION_COMMENT') {
                // Trả về link để mở chat (JavaScript sẽ xử lý)
                return `javascript:openMentionChat(${n.referenceId})`;
            }
            // Post bình thường
            let profileUrl = '/news-feed' + '?postID=' + n.referenceId;
            return profileUrl;
        case 'COMMENT':
            return `/news-feed?commentID=${n.referenceId}`;
        case 'FRIENDSHIP':
            return `/friends?filter=received-requests`;
        default:
            return '#';
    }
}

function buildNotificationText(n) {
    const username = n.sender?.fullName || 'Người dùng';
    switch ((n.notificationType || '').toUpperCase()) {
        case 'LIKE_POST':
            return `${username} đã thích bài viết của bạn`;
        case 'LIKE_COMMENT':
            return `${username} đã thích bình luận của bạn`;
        case 'COMMENT_POST':
            return `${username} đã bình luận bài viết của bạn`;
        case 'REPLY_COMMENT':
            return `${username} đã trả lời bình luận của bạn`;
        case 'FRIEND_REQUEST':
            return `${username} đã gửi lời mời kết bạn`;
        case 'MENTION_COMMENT':
                // Mention trong comment (logic cũ)
                return `${username} đã nhắc đến bạn trong một bình luận`;
        default:
            return n.message || "Bạn có một thông báo mới";
    }
}

function buildNotificationElement(n) {
    const a = document.createElement('a');
    a.className = 'dropdown-item d-flex align-items-start'; // đổi align-items-start để dot nằm trên
    a.setAttribute('href', buildLink(n));
    if (n.id) a.dataset.id = String(n.id);

    const avatarUrl = n.sender?.avatarUrl || '/images/default-avatar.png';

    a.innerHTML = `
        <img src="${escapeHtml(avatarUrl)}" 
             class="rounded-circle me-2" 
             width="40" height="40" 
             onerror="this.src='/images/default-avatar.png'">
        <div class="flex-grow-1">
            <div class="small text-dark">${escapeHtml(buildNotificationText(n))}</div>
            <div class="text-muted small">${formatTimeAgo(n.createdAt)}</div>
        </div>
        ${!n.isRead ? '<span class="unread-dot ms-2"></span>' : ''}
    `;

    const li = document.createElement('li');
    li.appendChild(a);
    return li;
}


function escapeHtml(unsafe) {
    if (unsafe == null) return '';
    return String(unsafe)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

// ---------- Fetch / API ----------
async function fetchJson(url, opts = {}) {
    const res = await fetch(url, {
        credentials: 'same-origin',
        headers: {'Accept': 'application/json'}, ...opts
    });
    if (!res.ok) {
        const text = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status}: ${text}`);
    }
    return res.json();
}

async function refreshUnreadCount() {
    try {
        const data = await fetchJson(`${API_BASE}/unread-count`);
        unreadCount = Number(data.count || 0);
        setBadge(unreadCount);
        return unreadCount;
    } catch (err) {
        // silent fail
        console.error('refreshUnreadCount error', err);
        return 0;
    }
}

async function loadInitialNotifications() {
    currentPage = 0;
    noMore = false;
    const container = document.getElementById(DROPDOWN_LIST_ID);
    if (!container) return;
    container.innerHTML = '<li class="text-center small text-muted py-2">Đang tải...</li>';
    try {
        const page = await fetchJson(`${API_BASE}?page=${currentPage}&size=${PAGE_SIZE}`);
        container.innerHTML = '';
        if (page.totalElements > 0)  page.content.forEach(n => container.appendChild(buildNotificationElement(n)));
        else {
            container.innerHTML = `<li class="text-center small text-muted py-2">Không có thông báo</li>`;
            return;
        }
        // track paging
        if (!page.pageable || page.totalPages === undefined) {
            // fallback: if items < page size, mark no more
            noMore = page.length < PAGE_SIZE;
        } else {
            noMore = page.number + 1 >= page.totalPages;
        }
        // sync badge with server value
        await refreshUnreadCount();
    } catch (err) {
        console.error('loadInitialNotifications error', err);
        container.innerHTML = `<li class="text-center small text-muted py-2">Không thể tải thông báo</li>`;
    }
}

async function loadNextPage() {
    if (loadingPage || noMore) return;
    loadingPage = true;
    currentPage += 1;
    const container = document.getElementById(DROPDOWN_LIST_ID);
    if (!container) {
        loadingPage = false;
        return;
    }

    // show loader item
    const loader = document.createElement('li');
    loader.className = 'text-center small text-muted py-2';
    loader.innerText = 'Đang tải...';
    container.appendChild(loader);
    try {
        const page = await fetchJson(`${API_BASE}?page=${currentPage}&size=${PAGE_SIZE}`);
        // remove loader
        loader.remove();
        page.content.forEach(n => container.appendChild(buildNotificationElement(n)));
        if (!page.pageable || page.totalPages === undefined) {
            noMore = page.length < PAGE_SIZE;
        } else {
            noMore = page.number + 1 >= page.totalPages;
        }
    } catch (err) {
        console.error('loadNextPage error', err);
        loader.innerText = 'Tải thất bại';
        // keep loader briefly then remove
        setTimeout(() => loader.remove(), 1500);
        currentPage -= 1;
    } finally {
        loadingPage = false;
    }
}

// ---------- Event handlers ----------
async function onNotificationClick(evt) {
    const a = evt.target.closest('a[data-id]');
    if (!a) return;
    const id = a.dataset.id;
    // mark as read (fire-and-forget)
    fetch(`${API_BASE}/${id}/read`, {method: 'PATCH', credentials: 'same-origin'})
        .then(resp => {
            if (resp.ok) decreaseBadge(1);
        })
        .catch(e => console.warn('mark read failed', e));
    // allow anchor default navigation to proceed
}

// infinite scroll detector
function onDropdownScroll(evt) {
    const el = evt.target;
    if (!el) return;
    const threshold = 120; // px from bottom
    if (el.scrollHeight - el.scrollTop - el.clientHeight < threshold) {
        loadNextPage();
    }
}

// ---------- WebSocket (STOMP) ----------
function connectWebSocket() {
    try {
        socket = new SockJS(WS_ENDPOINT);
        stompClient = Stomp.over(socket);
        stompClient.reconnect_delay = 5000; // auto reconnect setting in stompjs (ms)
        stompClient.debug = function (msg) { /* disable verbose debug in prod */
        };

        stompClient.connect({}, frame => {
            console.log('STOMP connected', frame);
            try {
                stompClient.subscribe(STOMP_QUEUE, message => {
                    console.log("kiem tra ")
                    console.log(message)
                    if (!message.body) return;
                    try {
                        const noti = JSON.parse(message.body);

                        handleIncomingNotification(noti);
                    } catch (err) {
                        console.error('Invalid notification payload', err, message.body);
                    }
                });
            } catch (err) {
                console.error('subscribe error', err);
            }
        }, onStompError);
    } catch (err) {
        console.error('connectWebSocket error', err);
    }
}

function onStompError(err) {
    console.warn('STOMP error', err);
    // stomp client will try to reconnect automatically if configured
}

// Handle incoming realtime notification
function handleIncomingNotification(noti) {

    // Update badge
    increaseBadge(1);

    // Prepend to list DOM
    const list = document.getElementById(DROPDOWN_LIST_ID);
    if (!list) return;
    const li = buildNotificationElement(noti);
    // ensure newest at top
    const first = list.firstChild;
    if (first) list.insertBefore(li, first);
    else list.appendChild(li);
}

// ---------- Extra helpers ----------
async function markAllRead() {
    try {
        const res = await fetch(`${API_BASE}/read-all`, {method: 'PATCH', credentials: 'same-origin'});
        if (res.ok) {
            unreadCount = 0;
            setBadge(0);
            // update items UI if you want (e.g., remove unread highlight) — left to CSS impl
        } else {
            console.warn('markAllRead failed', res.status);
        }
    } catch (err) {
        console.error('markAllRead error', err);
    }
}