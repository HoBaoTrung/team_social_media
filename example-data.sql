use social_media;

INSERT INTO users (
    username, email, password_hash, first_name, last_name,
    profile_picture, bio, date_of_birth, gender, phone,
    login_method, is_active, is_verified, account_status,
	 created_at
)
VALUES
('admin', 'admin@example.com', '$2a$10$oNtne4qiFdVyKsFC.S.xx.Pl9Zxhl65krYilyBrvzk4HeHLE1ExkK', 'I am', 'Admin',
 null, 'Hello, I am Admin.', '1990-05-15', 'MALE', '0121537589',
 'EMAIL', true, true, 'ACTIVE',   '2025-08-01'),
('john_doe', 'john@example.com', '$2a$10$oNtne4qiFdVyKsFC.S.xx.Pl9Zxhl65krYilyBrvzk4HeHLE1ExkK', 'John', 'Doe',
 null, 'Hello, I am John.', '1990-05-15', 'MALE', '0123456789',
 'EMAIL', true, true, 'ACTIVE',   '2025-08-01'),

('jane_smith', 'jane@example.com', '$2a$10$oNtne4qiFdVyKsFC.S.xx.Pl9Zxhl65krYilyBrvzk4HeHLE1ExkK', 'Jane', 'Smith',
 null, 'Photographer & blogger.', '1992-08-25', 'FEMALE', '0987654321',
 'GOOGLE', true, true, 'ACTIVE',  '2025-08-01'),

('alex_taylor', 'alex@example.com', '$2a$10$oNtne4qiFdVyKsFC.S.xx.Pl9Zxhl65krYilyBrvzk4HeHLE1ExkK', 'Alex', 'Taylor',
 NULL, 'Love coding and coffee.', '1995-12-03', 'OTHER', NULL,
 'FACEBOOK', true, true, 'ACTIVE', '2025-08-01');
 
 
 INSERT INTO users (
    username, email, password_hash, first_name, last_name,
    gender, login_method, is_active, is_verified, account_status,
    created_at
)
VALUES
   ('mike_jones', 'mike.jones@example.com', '$2a$10$DEF789', 'Mike', 'Jones', 'MALE', 'FACEBOOK', true, false, 'ACTIVE', '2023-03-05 14:45:00'),
    ('sarah_wilson', 'sarah.wilson@example.com', '$2a$10$GHI012', 'Sarah', 'Wilson', 'FEMALE', 'email', true, true, 'ACTIVE', '2023-04-10 09:20:00'),
    ('david_brown', 'david.brown@example.com', '$2a$10$JKL345', 'David', 'Brown', 'MALE', 'EMAIL', false, true, 'ACTIVE', '2023-05-15 16:30:00'),
    ('robert_garcia', 'robert.garcia@example.com', '$2a$10$PQR901', 'Robert', 'Garcia', 'MALE', 'EMAIL', true, false, 'ACTIVE', '2023-07-25 13:10:00'),
    ('emily_martin', 'emily.martin@example.com', '$2a$10$STU234', 'Emily', 'Martin', 'FEMALE', 'FACEBOOK', true, true, 'ACTIVE', '2023-08-30 15:25:00'),
    ('thomas_clark', 'thomas.clark@example.com', '$2a$10$VWX567', 'Thomas', 'Clark', 'MALE', 'EMAIL', false, false, 'ACTIVE', '2023-09-05 18:40:00'),
    ('amanda_lee', 'amanda.lee@example.com', '$2a$10$YZA890', 'Amanda', 'Lee', 'FEMALE', 'GOOGLE', true, true, 'ACTIVE', '2023-10-10 20:55:00');
 
 INSERT INTO users (
    username, email, password_hash, first_name, last_name, 
    gender, login_method, is_active, is_verified, account_status, 
    created_at
) VALUES
('user1', 'user1@example.com', '$2a$10$abc123hashedpassword1', 'Nguyen', 'Van A', 'MALE', 'EMAIL', 1, 1, 'ACTIVE', '2025-08-07 19:00:00'),
('user2', 'user2@example.com', '$2a$10$def456hashedpassword2', 'Tran', 'Thi B', 'FEMALE', 'GOOGLE', 1, 0, 'ACTIVE', '2025-08-07 19:01:00'),
('user3', 'user3@example.com', '$2a$10$ghi789hashedpassword3', 'Le', 'Van C', 'MALE', 'EMAIL', 1, 1, 'ACTIVE', '2025-08-07 19:02:00'),
('user4', 'user4@example.com', '$2a$10$jkl012hashedpassword4', 'Pham', 'Thi D', 'FEMALE', 'EMAIL', 0, 0, 'ACTIVE', '2025-08-07 19:03:00'),
('user5', 'user5@example.com', '$2a$10$mno345hashedpassword5', 'Ho', 'Van E', 'MALE', 'EMAIL', 1, 1, 'ACTIVE', '2025-08-07 19:04:00'),
('user6', 'user6@example.com', '$2a$10$pqr678hashedpassword6', 'Vo', 'Thi F', 'FEMALE', 'GOOGLE', 1, 1, 'ACTIVE', '2025-08-07 19:05:00'),
('user7', 'user7@example.com', '$2a$10$stu901hashedpassword7', 'Bui', 'Van G', 'MALE', 'EMAIL', 0, 0, 'ACTIVE', '2025-08-07 19:06:00'),
('user8', 'user8@example.com', '$2a$10$vwx234hashedpassword8', 'Dang', 'Thi H', 'FEMALE', 'facebook', 1, 0, 'ACTIVE', '2025-08-07 19:07:00'),
('user9', 'user9@example.com', '$2a$10$yza567hashedpassword9', 'Ngo', 'Van I', 'MALE', 'EMAIL', 1, 1, 'ACTIVE', '2025-08-07 19:08:00'),
('user10', 'user10@example.com', '$2a$10$bcd890hashedpassword10', 'Do', 'Thi K', 'FEMALE', 'GOOGLE', 1, 1, 'ACTIVE', '2025-08-07 19:09:00');
 
  INSERT INTO users_roles(user_id,role_id)values
  (1,1),(2,2),(21,2),
 (3,2),(4,2),(5,2),(6,2),(7,2),(8,2),(9,2),(10,2),(11,2),(12,2),(13,2),(14,2),(15,2),(16,2),(17,2),(18,2),(19,2),(20,2);
 
 
 UPDATE users 
SET profile_picture = 'https://res.cloudinary.com/dryyvmkwo/image/upload/v1748588721/samples/landscapes/nature-mountains.jpg'
WHERE id BETWEEN 1 AND 20;
 
 INSERT INTO friendships (`status`, addressee_id, requester_id) VALUES
 ('ACCEPTED',1,2),
 ('ACCEPTED',1,3),
 ('ACCEPTED',3,2);
 
 INSERT INTO user_privacy_settings (
    user_id,
    show_profile,
    show_friend_list,
    show_full_name,
    show_address,
    show_phone,
    show_email,
    show_avatar,
    show_bio,
    show_dob,
    allow_send_message,
    allow_search_by_email,
    allow_search_by_phone,
    can_be_found,
    allow_friend_requests
) VALUES 
(
    1, -- User 1
    'PUBLIC',
    'PUBLIC',
    'PUBLIC',
    'PRIVATE',
    'PRIVATE',
    'PRIVATE',
    'PUBLIC',
    'PUBLIC',
    'PRIVATE',
    'FRIENDS',
    true,
    true,
    true,
    true
),
(
    2, -- User 2
    'FRIENDS',
    'FRIENDS',
    'PUBLIC',
    'PRIVATE',
    'PRIVATE',
    'FRIENDS',
    'PUBLIC',
    'FRIENDS',
    'PRIVATE',
    'FRIENDS',
    false,
    true,
    true,
    false
),
(
    3, -- User 3
    'FRIENDS',
    'FRIENDS',
    'PUBLIC',
    'PRIVATE',
    'PRIVATE',
    'FRIENDS',
    'PUBLIC',
    'FRIENDS',
    'PRIVATE',
    'FRIENDS',
    false,
    true,
    true,
    false
);



INSERT INTO user_privacy_settings (
    user_id,show_profile,
    show_friend_list,show_full_name,
    show_address,show_phone,show_email,show_avatar,show_bio,
    show_dob, allow_send_message,
    allow_search_by_email,allow_search_by_phone,
    can_be_found,allow_friend_requests
) VALUES 
(4, 'PUBLIC', 'FRIENDS', 'PUBLIC', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'FRIENDS', true, false, true, true),
(5, 'FRIENDS', 'FRIENDS', 'FRIENDS', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'FRIENDS', 'FRIENDS', 'PRIVATE', 'PUBLIC', false, false, true, true),
(12, 'PUBLIC', 'FRIENDS', 'PUBLIC', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'FRIENDS', true, false, true, true),
(13, 'FRIENDS', 'FRIENDS', 'FRIENDS', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'FRIENDS', 'FRIENDS', 'PRIVATE', 'PUBLIC', false, false, true, true),
(6, 'PUBLIC', 'FRIENDS', 'PUBLIC', 'PRIVATE', 'FRIENDS', 'PRIVATE', 'PUBLIC', 'FRIENDS', 'FRIENDS', 'FRIENDS', true, true, true, true),
(7, 'FRIENDS', 'PRIVATE', 'FRIENDS', 'PRIVATE', 'PRIVATE', 'FRIENDS', 'PUBLIC', 'PUBLIC', 'PRIVATE', 'PUBLIC', false, true, true, false),
(8, 'PUBLIC', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'PRIVATE', 'PRIVATE', 'FRIENDS', 'FRIENDS', 'FRIENDS', 'FRIENDS', true, false, true, true),
(9, 'PRIVATE', 'FRIENDS', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', false, false, false, false),
(10, 'PUBLIC', 'FRIENDS', 'FRIENDS', 'PRIVATE', 'FRIENDS', 'FRIENDS', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'PUBLIC', true, true, true, true),
(11, 'FRIENDS', 'PUBLIC', 'PUBLIC', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'FRIENDS', 'FRIENDS', 'PRIVATE', 'FRIENDS', false, true, true, false),
  (14, 'PUBLIC', 'PRIVATE', 'PUBLIC', 'PRIVATE', 'PRIVATE', 'FRIENDS', 'PUBLIC', 'PUBLIC', 'PRIVATE', 'PUBLIC', true, false, true, true),
    (15, 'PRIVATE', 'PRIVATE', 'FRIENDS', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'FRIENDS', 'PRIVATE', 'PRIVATE', 'FRIENDS', false, false, false, false),
    (16, 'PUBLIC', 'FRIENDS', 'PUBLIC', 'FRIENDS', 'PRIVATE', 'FRIENDS', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'FRIENDS', true, true, true, true),
    (17, 'FRIENDS', 'FRIENDS', 'FRIENDS', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'FRIENDS', 'FRIENDS', 'PRIVATE', 'FRIENDS', false, true, true, true),
    (18, 'PUBLIC', 'PUBLIC', 'PUBLIC', 'PRIVATE', 'FRIENDS', 'PUBLIC', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'PUBLIC', true, true, true, true),
    (19, 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PRIVATE', 'PUBLIC', false, false, false, false),
    (20, 'PUBLIC', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'FRIENDS', 'FRIENDS', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'PUBLIC', true, true, true, true),
 (21, 'PUBLIC', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'FRIENDS', 'FRIENDS', 'PUBLIC', 'PUBLIC', 'FRIENDS', 'PUBLIC', true, true, true, true);


INSERT INTO posts (user_id, content, image_urls, privacy_level,`privacy_comment_level`, created_at,is_deleted) VALUES
(1, 'Chào ngày mới! Mọi người có kế hoạch gì cho hôm nay không?', NULL, 'PUBLIC', 'PUBLIC',  '2023-10-01 08:15:00',false),
(1, 'Beautiful pictures', '["https://res.cloudinary.com/dryyvmkwo/image/upload/v1748588729/cld-sample-4.jpg", "https://res.cloudinary.com/dryyvmkwo/image/upload/v1748588729/cld-sample-2.jpg"]', 'FRIENDS', 'PUBLIC', '2023-10-01 10:30:00',false),
(1, 'Check out my new video!', NULL,  'PUBLIC','PUBLIC',  '2023-10-02 15:45:00',false),
(1, 'Cảm ơn mọi người đã chúc mừng sinh nhật tôi!', '["https://res.cloudinary.com/dryyvmkwo/image/upload/v1748588728/samples/dessert-on-a-plate.jpg"]', 'FRIENDS',  'PUBLIC', '2023-10-03 09:20:00',false);


-- INSERT INTO post_comments (post_id, user_id, content, is_deleted, created_at, updated_at)
-- VALUES (2, 1, 'Great post! Really enjoyed reading this.', false, '2023-05-10 09:15:22', '2023-05-10 09:15:22'),
-- (2, 2, 'Could you elaborate more on the second point?', false, '2023-05-10 11:30:45', '2023-05-10 11:30:45'),
-- (2, 3, 'Good', false, '2023-05-11 14:20:10', '2023-05-11 14:25:33'),
-- (2, 1, 'Thanks for sharing this information!', false, '2023-05-12 08:45:12', '2023-05-12 08:45:12'),
-- (2, 4, 'I disagree with some points but appreciate the effort.', false, '2023-05-12 16:30:00', '2023-05-12 16:30:00');



-- 1. Cuộc trò chuyện 1-1 (PRIVATE)
INSERT INTO conversations (conversation_name, conversation_type, created_by, group_avatar, created_at, updated_at, is_active, last_message_at)
VALUES (NULL, 'PRIVATE', 1, NULL, NOW(), NOW(), TRUE, NOW());

-- Giả sử ID sinh ra tự động = 1
INSERT INTO conversation_participants (conversation_id, user_id, joined_at, role, is_muted, is_active, nickname)
VALUES 
    (1, 1, NOW(), 'MEMBER', FALSE, TRUE, NULL), 
    (1, 2, NOW(), 'MEMBER', FALSE, TRUE, NULL);


-- 2. Cuộc trò chuyện nhóm (GROUP)
INSERT INTO conversations (conversation_name, conversation_type, created_by, group_avatar, created_at, updated_at, is_active, last_message_at)
VALUES ('Nhóm Java Spring', 'GROUP', 1, 'https://res.cloudinary.com/dryyvmkwo/image/upload/v1748588726/samples/balloons.jpg', NOW(), NOW(), TRUE, NOW());

-- Giả sử ID sinh ra tự động = 2
INSERT INTO conversation_participants (conversation_id, user_id, joined_at, role, is_muted, is_active, nickname)
VALUES 
    (2, 1, NOW(), 'ADMIN', FALSE, TRUE, 'Trung'), 
    (2, 2, NOW(), 'MEMBER', FALSE, TRUE, 'Hà'), 
    (2, 3, NOW(), 'MEMBER', TRUE, TRUE, 'Minh');

---------------------------------------------------

-- 3. Cuộc trò chuyện nhóm khác
INSERT INTO conversations (conversation_name, conversation_type, created_by, group_avatar, created_at, updated_at, is_active, last_message_at)
VALUES ('Dự án Web Chat', 'GROUP', 2, 'https://res.cloudinary.com/dryyvmkwo/image/upload/v1748588721/samples/ecommerce/accessories-bag.jpg', NOW(), NOW(), TRUE, NOW());

-- Giả sử ID sinh ra tự động = 3
INSERT INTO conversation_participants (conversation_id, user_id, joined_at, role, is_muted, is_active, nickname)
VALUES 
    (3, 2, NOW(), 'ADMIN', FALSE, TRUE, 'Hà'), 
    (3, 3, NOW(), 'MEMBER', FALSE, TRUE, 'Minh'), 
    (3, 4, NOW(), 'MEMBER', FALSE, TRUE, 'An'), 
    (3, 5, NOW(), 'MEMBER', TRUE, TRUE, 'Lan');

