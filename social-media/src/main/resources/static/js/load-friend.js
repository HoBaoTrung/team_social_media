let friendshipStatus = 'ACCEPTED'
let page = 0;
const size = 10;
let isLoading = false;
let hasMoreData = true;
const targetUserId = document.getElementById('targetUserId').value;
const currentFilter = document.getElementById('filterValue').value;
const currentPage = window.location.pathname;
const isAddBtnActionFriend = currentPage.includes('friends') ? true : false

$(document).ready(function () {
    console.log('Initial filter from server:', currentFilter);

    // Đảm bảo nav-link tương ứng có lớp active
    $('.sidebar .nav-link').removeClass('active');
    $(`.sidebar .nav-link[data-filter="${currentFilter}"]`).addClass('active');

    // Xử lý click vào sidebar
    $('.sidebar .nav-link').click(function (e) {
        $('.sidebar .nav-link').removeClass('active');
        $(this).addClass('active');

        page = 0;
        hasMoreData = true;
        $('#friends-list').empty(); // reset danh sách khi đổi filter
        console.log('Filter changed to:', currentFilter);
        loadMoreFriends();
    });

    // Infinite scroll
    $(window).scroll(function () {
        if (hasMoreData && !isLoading &&
            $(window).scrollTop() + $(window).height() >= $(document).height() - 100) {
            loadMoreFriends();
        }
    });

    // 🔹 Gọi loadMoreFriends() ngay khi DOM đã tải
    loadMoreFriends();
});


let isSender = isReceiver = false

function loadMoreFriends() {
    if (!hasMoreData) return;

    isLoading = true;
    $('#loading').show();

    let url;
    switch (currentFilter) {
        case 'all':
            url = `/api/friends?targetUserId=${targetUserId}&page=${page}&size=${size}`;
            friendshipStatus = 'ACCEPTED'
            break;
        case 'mutual':
            url = `/api/friends/mutual?targetUserId=${targetUserId}&page=${page}&size=${size}`;
            friendshipStatus = 'ACCEPTED'
            break;
        case 'non-friends':
            url = `/api/friends/non-friends?page=${page}&size=${size}`;
            friendshipStatus = 'NONE'
            break;
        case 'sent-requests':
            url = `/api/friends/sent-requests?page=${page}&size=${size}`;
            friendshipStatus = 'PENDING';
            isSender = true;
            break;
        case 'received-requests':
            url = `/api/friends/received-requests?page=${page}&size=${size}`;
            friendshipStatus = 'PENDING';
            isReceiver = true
            break;
        default:
            url = `/api/friends?targetUserId=${targetUserId}&page=${page}&size=${size}`;
            friendshipStatus = 'ACCEPTED'
    }

    $.ajax({
        url: url,
        method: 'GET',
        success: function (data) {
            if (data.totalElements == 0){
                const el = document.getElementById("no-friend");
                el.style.display = 'block';
                return;
            }

            if (!data.content || data.content.length === 0 || page >= data.totalPages) {
                hasMoreData = false;
                $('#loading').hide();
                isLoading = false;
                return;
            }

            data.content.forEach(function (friend) {
                // Tạo HTML cho friendCard và friend-button-group
                const friendCardHtml = `
                    <div class="col-6 col-md-3">
                        <div class="friend-action">
                            <a href="/profile/${friend.username}" class="friend-card mt-2 mb-2 justify-content-center d-flex">
                                <img class="mb-3" src="${friend.avatarUrl || '/images/default-avatar.jpg'}" alt="Avatar" />
                                <div class="friend-info">
                                    <h5 class="name-list-friend" title="${friend.fullName}">${friend.fullName}</h5>
                                    <h7>${friend.mutualFriends} bạn chung</h7>
                                </div>
                            </a>
                            <div class="friend-button-group" id="btn-group-${friend.username}"></div>
                            <div class="chat-button-group" id="chat-btn-${friend.username}"></div>
                        </div>
                    </div>
                `;
                $('#friends-list').append(friendCardHtml);

                if (isAddBtnActionFriend) {
                    // Gắn nút friend action vào friendCard
                    $.ajax({
                        url: '/friend/button',
                        method: 'GET',
                        data: {
                            username: friend.username,
                            friendshipStatus: friendshipStatus,
                            isSender: isSender,
                            isReceiver: isReceiver,
                            allowFriendRequests: friend.allowFriendRequests,
                            isVisible: true
                        },
                        success: function (fragmentHtml) {
                            $(`#btn-group-${friend.username}`).html(fragmentHtml);
                        },
                        error: function (xhr) {
                            console.error(`Lỗi khi tải nút cho ${friend.username}:`, xhr.status, xhr.statusText);
                        }
                    });

                }
            });

            page++;
            isLoading = false;
            $('#loading').hide();
        },
        error: function (xhr) {
            isLoading = false;
            $('#loading').hide();
            hasMoreData = false;
            console.error('Lỗi khi tải danh sách:', xhr.status, xhr.statusText);
            alert(`Lỗi khi tải danh sách: ${xhr.status} - ${xhr.statusText}`);
        }
    });
}