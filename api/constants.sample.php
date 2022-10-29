<?php

define('BASE_URL', 'http://localhost:8000');

define('DATE_TIMEZONE', 'Asia/Kolkata');
define('DATE_MYSQL', 'Y-m-d H:i:s');

define('DIR_AUDIOS', 'audios');
define('DIR_ICONS', 'icons');
define('DIR_PHOTOS', 'photos');
define('DIR_PREVIEWS', 'previews');
define('DIR_SCREENSHOTS', 'screenshots');
define('DIR_VIDEOS', 'videos');

define('FACEBOOK_APP_ID', null);
define('FACEBOOK_APP_SECRET', null);

define('FILESYSTEM_DRIVER', 'local'); // local, s3, gcs

define('FIREBASE_CREDENTIALS', null);

define('GCS_CREDENTIALS', null);
define('GCS_BUCKET', null);
define('GCS_URL', null);

define('GOOGLE_CLIENT_ID', null);

define('LOCAL_DIRECTORY', __DIR__.'/public/storage');
define('LOCAL_URL', BASE_URL.'/storage');

define('JWT_SECRET', 'something_really_secret');

define('MYSQL_DATABASE', 'taktak');
define('MYSQL_HOST', '127.0.0.1');
define('MYSQL_USER', 'taktak');
define('MYSQL_PASSWORD', 'taktak');

define('NOTIFICATION_TOPIC', 'default');

define('S3_ACCESS_KEY_ID', null);
define('S3_ACCESS_KEY_SECRET', null);
define('S3_REGION', 'eu-west-1');
define('S3_BUCKET', null);
