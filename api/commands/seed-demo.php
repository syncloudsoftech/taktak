<?php

require_once __DIR__ . '/../vendor/autoload.php';

define('DIR_DEMO', __DIR__ . '/../demo');

$dirs = [
    DIR_AUDIOS,
    DIR_ICONS,
    DIR_PHOTOS,
    DIR_PREVIEWS,
    DIR_SCREENSHOTS,
    DIR_VIDEOS,
];
foreach ($dirs as $dir) {
    cleanup(LOCAL_DIRECTORY . '/' . $dir);
}

$db = database();

echo 'Cleaning up existing data...', PHP_EOL;

$db->exec('SET FOREIGN_KEY_CHECKS = 0');
$db->exec('TRUNCATE articles');
$db->exec('TRUNCATE article_sections');
$db->exec('TRUNCATE comments');
$db->exec('TRUNCATE devices');
$db->exec('TRUNCATE followers');
$db->exec('TRUNCATE likes');
$db->exec('TRUNCATE messages');
$db->exec('TRUNCATE participants');
$db->exec('TRUNCATE threads');
$db->exec('TRUNCATE notifications');
$db->exec('TRUNCATE videos');
$db->exec('TRUNCATE video_sections');
$db->exec('TRUNCATE songs');
$db->exec('TRUNCATE songs');
$db->exec('TRUNCATE song_sections');
$db->exec('TRUNCATE users');
$db->exec('SET FOREIGN_KEY_CHECKS = 1');

echo 'Seeding admin user...', PHP_EOL;

$db->insert('users', [
    'username' => $username = 'admin',
    'password' => hash('sha256', $password = '12345'),
    'role' => 'admin',
    'name' => 'Administrator',
    'email' => 'admin@example.com',
    'verified' => 1,
    'date_created' => date(DATE_MYSQL),
    'date_updated' => date(DATE_MYSQL),
]);

$faker = Faker\Factory::create();

echo 'Seeding demo users...', PHP_EOL;

for ($i = 0; $i <= 25; $i++) {
    if ($faker->boolean) {
        file_upload(
            DIR_DEMO . '/user' . $faker->numberBetween(1, 4) . '.png',
            DIR_PHOTOS,
            $photo2 = random_string(24) . '.png'
        );
    } else {
        $photo2 = null;
    }

    $date = $faker->dateTime->format(DATE_MYSQL);
    $db->insert('users', [
        'username' => $faker->userName,
        'password' => hash('sha256', random_string(10)),
        'role' => 'user',
        'photo' => $photo2,
        'name' => $faker->name,
        'email' => $faker->boolean ? $faker->email : null,
        'bio' => $faker->boolean ? $faker->sentences(2, true) : null,
        'verified' => $faker->boolean(25) ? 1 : 0,
        'date_created' => $date,
        'date_updated' => $date,
    ]);
}

echo 'Seeding article sections...', PHP_EOL;

$sections = [
    'India' => 'CAAqIQgKIhtDQkFTRGdvSUwyMHZNRE55YXpBU0FtVnVLQUFQAQ',
    'Business' => 'CAAqJggKIiBDQkFTRWdvSUwyMHZNRGx6TVdZU0FtVnVHZ0pKVGlnQVAB',
    'Technology' => 'CAAqJggKIiBDQkFTRWdvSUwyMHZNRGRqTVhZU0FtVnVHZ0pKVGlnQVAB',
    'Entertainment' => 'CAAqJggKIiBDQkFTRWdvSUwyMHZNREpxYW5RU0FtVnVHZ0pKVGlnQVAB',
];
foreach ($sections as $name => $topic) {
    $date = $faker->dateTime->format(DATE_MYSQL);
    $db->insert('article_sections', [
        'name' => $name,
        'google_news_topic' => $topic,
        'date_created' => $date,
        'date_updated' => $date,
    ]);
}

$sections = ['Dance', 'Hip-hop', 'Romantic'];
foreach ($sections as $section) {
    $date = $faker->dateTime->format(DATE_MYSQL);
    $db->insert('song_sections', [
        'name' => $section,
        'date_created' => $date,
        'date_updated' => $date,
    ]);
}

echo 'Seeding song sections...', PHP_EOL;

$sections = $db->select('song_sections', 'id');
for ($i = 0; $i <= 25; $i++) {
    file_upload(
        DIR_DEMO . '/song' . $faker->numberBetween(1, 4) . '.aac',
        DIR_AUDIOS,
        $audio2 = random_string(24) . '.aac'
    );
    if ($faker->boolean) {
        file_upload(
            DIR_DEMO . '/song' . $faker->numberBetween(1, 2) . '.png',
            DIR_ICONS,
            $icon2 = random_string(24) . '.png'
        );
    } else {
        $icon2 = null;
    }

    $date = $faker->dateTime->format(DATE_MYSQL);
    $db->insert('songs', [
        'section_id' => $faker->randomElement($sections),
        'name' => $faker->words(3, true),
        'audio' => $audio2,
        'icon' => $icon2,
        'date_created' => $date,
        'date_updated' => $date,
    ]);
}

echo 'Seeding video sections...', PHP_EOL;

$sections = ['Trending', 'Featured', "Editor's Choice"];
foreach ($sections as $section) {
    $date = $faker->dateTime->format(DATE_MYSQL);
    $db->insert('video_sections', [
        'name' => $section,
        'date_created' => $date,
        'date_updated' => $date,
    ]);
}

echo 'Seeding demo videos...', PHP_EOL;

$songs = $db->select('songs', 'id');
$sections = $db->select('video_sections', 'id');
$users = $db->select('users', 'id');
for ($i = 0; $i <= 25; $i++) {
    $song = $faker->boolean ? $faker->randomElement($songs) : null;
    $section = $faker->boolean ? $faker->randomElement($sections) : null;
    $j = $faker->numberBetween(1, 2);
    file_upload(
        DIR_DEMO . '/video' . $j . '.mp4',
        DIR_VIDEOS,
        $video2 = random_string(24) . '.mp4'
    );
    file_upload(
        DIR_DEMO . '/video' . $j . '.png',
        DIR_SCREENSHOTS,
        $screenshot2 = random_string(24) . '.png'
    );
    file_upload(
        DIR_DEMO . '/video' . $j . '.gif',
        DIR_PREVIEWS,
        $preview2 = random_string(24) . '.gif'
    );
    $date = $faker->dateTime->format(DATE_MYSQL);
    $db->insert('videos', [
        'user_id' => $faker->randomElement($users),
        'song_id' => $song,
        'section_id' => $section,
        'description' => $faker->boolean ? $faker->paragraph : null,
        'video' => $video2,
        'screenshot' => $screenshot2,
        'preview' => $preview2,
        'private' => $faker->boolean(10) ? 1 : 0,
        'comments' => $faker->boolean(75) ? 1 : 0,
        'date_created' => $date,
        'date_updated' => $date,
    ]);
}

echo 'Seeding demo comments...', PHP_EOL;

$videos = $db->select('videos', 'id');
foreach ($videos as $video) {
    foreach ($users as $user) {
        if ($faker->boolean) {
            $date = $faker->dateTime->format(DATE_MYSQL);
            $db->insert('comments', [
                'user_id' => $user,
                'video_id' => $video,
                'text' => $faker->paragraph,
                'date_created' => $date,
                'date_updated' => $date,
            ]);
        }

        if ($faker->boolean(75)) {
            $db->insert('likes', [
                'user_id' => $user,
                'video_id' => $video,
            ]);
        }
    }
}

echo 'Seeding demo followers...', PHP_EOL;

foreach ($users as $user1) {
    foreach ($users as $user2) {
        if ($faker->boolean(75)) {
            $db->insert('followers', [
                'follower_id' => $user1,
                'following_id' => $user2,
            ]);
        }
    }
}

echo 'Login as "', $username, '" using "', $password, '" as password.', PHP_EOL;
echo 'Finished!', PHP_EOL;
