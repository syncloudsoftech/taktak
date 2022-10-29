<?php

use Medoo\Medoo;

/** @var Medoo $db */

$db->create('users', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'username' => ['VARCHAR(20)', 'NOT NULL'],
    'password' => ['CHAR(64)', 'NOT NULL'],
    'role' => ['VARCHAR(150)', 'NOT NULL'],
    'photo' => ['VARCHAR(150)', 'NULL'],
    'name' => ['VARCHAR(150)', 'NOT NULL'],
    'email' => ['VARCHAR(150)', 'NULL'],
    'bio' => ['VARCHAR(300)', 'NULL'],
    'verified' => ['TINYINT', 'NOT NULL'],
    'facebook_id' => ['VARCHAR(150)', 'NULL'],
    'google_id' => ['VARCHAR(150)', 'NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
    'UNIQUE KEY (username)',
]);

$db->create('song_sections', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'name' => ['VARCHAR(150)', 'NOT NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
]);

$db->create('songs', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'section_id' => ['INT', 'NOT NULL'],
    'name' => ['VARCHAR(150)', 'NOT NULL'],
    'audio' => ['VARCHAR(150)', 'NOT NULL'],
    'icon' => ['VARCHAR(150)', 'NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
    'CONSTRAINT songs_section_id FOREIGN KEY (section_id) REFERENCES song_sections(id) ON DELETE CASCADE',
]);

$db->create('video_sections', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'name' => ['VARCHAR(150)', 'NOT NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
]);

$db->create('videos', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'user_id' => ['INT', 'NOT NULL'],
    'song_id' => ['INT', 'NULL'],
    'section_id' => ['INT', 'NULL'],
    'description' => ['VARCHAR(300)', 'NULL'],
    'video' => ['VARCHAR(150)', 'NOT NULL'],
    'screenshot' => ['VARCHAR(150)', 'NOT NULL'],
    'preview' => ['VARCHAR(150)', 'NOT NULL'],
    'private' => ['TINYINT', 'NOT NULL'],
    'comments' => ['TINYINT', 'NOT NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
    'CONSTRAINT videos_section_id FOREIGN KEY (section_id) REFERENCES video_sections(id) ON DELETE SET NULL',
    'CONSTRAINT videos_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
    'CONSTRAINT videos_song_id FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE SET NULL',
]);

$db->create('likes', [
    'user_id' => ['INT', 'NOT NULL'],
    'video_id' => ['INT', 'NOT NULL'],
    'CONSTRAINT likes_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
    'CONSTRAINT likes_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE',
    'CONSTRAINT likes_user_id_video_id_unique UNIQUE KEY (user_id, video_id)',
]);

$db->create('comments', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'user_id' => ['INT', 'NOT NULL'],
    'video_id' => ['INT', 'NOT NULL'],
    'text' => ['VARCHAR(300)', 'NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
    'CONSTRAINT comments_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
    'CONSTRAINT comments_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE',
]);

$db->create('devices', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'user_id' => ['INT', 'NULL'],
    'token' => ['VARCHAR(4096)', 'NOT NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
    'CONSTRAINT devices_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
]);

$db->create('followers', [
    'follower_id' => ['INT', 'NOT NULL'],
    'following_id' => ['INT', 'NOT NULL'],
    'CONSTRAINT followers_follower_id FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE',
    'CONSTRAINT followers_following_id FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE',
    'CONSTRAINT followers_follower_id_following_id_unique UNIQUE KEY (follower_id, following_id)',
]);

$db->create('notifications', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'source_id' => ['INT', 'NOT NULL'],
    'target_id' => ['INT', 'NOT NULL'],
    'video_id' => ['INT', 'NULL'],
    'content' => ['VARCHAR(150)', 'NOT NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
    'CONSTRAINT notifications_source_id FOREIGN KEY (source_id) REFERENCES users(id) ON DELETE CASCADE',
    'CONSTRAINT notifications_target_id FOREIGN KEY (target_id) REFERENCES users(id) ON DELETE CASCADE',
    'CONSTRAINT notifications_video_id FOREIGN KEY (video_id) REFERENCES videos(id) ON DELETE CASCADE',
]);
