<?php

if (php_sapi_name() !== 'cli-server') {
    die('No trespassing.');
}

define('DEBUG_ENABLED', true);

$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);

if ($path === '/') {
    $path .= 'index.php';
}

if (preg_match('/\.php$/', $path)) {
    require __DIR__ . $path;
    return;
}

if (file_exists(__DIR__ . $path)) {
    return false;
}

require_once __DIR__ . '/index.php';
