<?php

use Aws\S3\S3Client;
use Gaufrette\Adapter\AwsS3 as AwsS3Adapter;
use Gaufrette\Adapter\GoogleCloudStorage as GcsAdapter;
use Gaufrette\Adapter\Local as LocalAdapter;
use Gaufrette\Extras\Resolvable\ResolvableFilesystem;
use Gaufrette\Extras\Resolvable\Resolver\AwsS3PublicUrlResolver;
use Gaufrette\Extras\Resolvable\Resolver\StaticUrlResolver;
use Gaufrette\Filesystem;
use Kreait\Firebase\Factory;
use Kreait\Firebase\Messaging\AndroidConfig;
use Kreait\Firebase\Messaging\CloudMessage;
use Kreait\Firebase\Messaging\Notification;
use Medoo\Medoo;

function cleanup(string $dir): bool
{
    if (is_dir($dir)) {
        $rdi = new RecursiveDirectoryIterator($dir, FilesystemIterator::SKIP_DOTS);
        $rii = new RecursiveIteratorIterator($rdi, RecursiveIteratorIterator::CHILD_FIRST);
        foreach ($rii as $file) {
            $file->isDir() ? rmdir($file) : unlink($file);
        }

        return true;
    }

    return false;
}

function database(): Medoo
{
    static $db;
    if (empty($db)) {
        $db = new Medoo([
            'database_type' => 'mysql',
            'database_name' => MYSQL_DATABASE,
            'server' => MYSQL_HOST,
            'username' => MYSQL_USER,
            'password' => MYSQL_PASSWORD,
            'charset' => 'utf8mb4',
            'collation' => 'utf8mb4_general_ci',
        ]);
    }

    return $db;
}

function download(string $url)
{
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_HEADER, false);
    curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 15);
    $data = curl_exec($ch);
    curl_close($ch);
    return $data;
}

function filesystem(): ?ResolvableFilesystem
{
    static $fs = null;
    if (empty($fs)) {
        switch (FILESYSTEM_DRIVER) {
            case 'local':
                $adapter = new LocalAdapter(LOCAL_DIRECTORY);
                return new ResolvableFilesystem(
                    new Filesystem($adapter),
                    new StaticUrlResolver(LOCAL_URL)
                );
            case 's3':
                $client = new S3Client([
                    'credentials' => [
                        'key' => S3_ACCESS_KEY_ID,
                        'secret' => S3_ACCESS_KEY_SECRET,
                    ],
                    'version' => 'latest',
                    'region' => S3_REGION,
                ]);
                $adapter = new AwsS3Adapter($client,S3_BUCKET);
                return new ResolvableFilesystem(
                    new Filesystem($adapter),
                    new AwsS3PublicUrlResolver($client, S3_BUCKET)
                );
            case 'gcs':
                $client = new \Google_Client();
                $client->setAuthConfig(GCS_CREDENTIALS);
                $client->addScope(Google_Service_Storage::DEVSTORAGE_READ_WRITE);
                $service = new \Google_Service_Storage($client);
                $adapter = new GcsAdapter($service, GCS_BUCKET, ['acl' => 'public'], true);
                return new ResolvableFilesystem(
                    new Filesystem($adapter),
                    new StaticUrlResolver(GCS_URL)
                );
        }
    }

    return null;
}

function file_delete(string $folder, string $name)
{
    filesystem()->delete($folder . '/' . $name);
}

function file_save($contents, string $folder, ?string $name = null)
{
    if (empty($name)) {
        $name = random_string(10);
    }

    filesystem()->write($folder . '/' . $name, $contents, true);
}

function file_upload(string $file, string $folder, ?string $name = null)
{
    if (empty($name)) {
        $name = random_string(10);
    }

    filesystem()->write($folder . '/' . $name, file_get_contents($file), true);
}

function file_url(string $folder, string $name): ?string
{
    return filesystem()->resolve($folder . '/' . $name);
}

function firebase(): Factory
{
    return (new Factory)->withServiceAccount(FIREBASE_CREDENTIALS);
}

function notification(int $source, int $target, string $content, ?int $video = null): array
{
    $db = database();
    $username = $db->get('users', 'username', ['id' => $source]);
    switch ($content) {
        case 'commented_on_video':
            $title = sprintf('@%s commented on your video.', $username);
            $body = 'Click here to open app and reply to their comment.';
            break;
        case 'followed_you':
            $title = sprintf('@%s started following you.', $username);
            $body = 'Click here to open app and see their profile.';
            break;
        case 'liked_video':
            $title = sprintf('@%s liked your video.', $username);
            $body = 'Click here to open app and create more videos.';
            break;
        case 'sent_message':
            $title = sprintf('@%s sent you a message.', $username);
            $body = 'Click here to open app and reply.';
            break;
        default:
            $title = $body = false;
            break;
    }

    $image = null;
    if ($video) {
        $image = $db->get('videos', 'screenshot', ['id' => $video]);
    }

    return [$title, $body, $image];
}

function notify(string $content, int $source, int $target, ?int $video = null, ?array $data = null)
{
    $db = database();
    if ($content !== 'sent_message') {
        $row = [
            'source_id' => $source,
            'target_id' => $target,
            'video_id' => $video,
            'content' => $content,
        ];
        $row['date_created'] = $row['date_updated'] = date(DATE_MYSQL);
        $db->insert('notifications', $row);
    }

    if (FIREBASE_CREDENTIALS !== null) {
        list ($title, $body, $image) = notification($source, $target, $content, $video);
        if ($title && $body) {
            $messaging = firebase()->createMessaging();
            $message = CloudMessage::new()
                ->withAndroidConfig(AndroidConfig::fromArray(['notification' => ['sound' => 'default']]))
                ->withNotification(Notification::create($title, $body, $image));
            if ($data) {
                $message = $message->withData($data);
            }

            $tokens = $db->select('devices', 'token', ['user_id' => $target]);
            try {
                $messaging->sendMulticast($message, $tokens);
            } catch (Exception $e) {
                if (defined('DEBUG_ENABLED')) {
                    throw $e;
                }
            }
        }
    }
}

function random_string(int $length = 10): string
{
    static $charset = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    $random = '';
    for ($i = 0; $i < $length; $i++) {
        $random .= $charset[rand(0, strlen($charset) - 1)];
    }

    return $random;
}
