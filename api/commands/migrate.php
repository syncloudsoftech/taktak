<?php

require_once __DIR__ . '/../vendor/autoload.php';

$db = database();
$db->create('migrations', [
    'version' => ['CHAR(14)', 'NOT NULL'],
]);

$files = scandir(__DIR__ . '/../migrations');
$migrations = [];
foreach ($files as $file) {
    if (!in_array($file, ['.', '..'])) {
        $migrations[] = substr($file, 0, 14);
    }
}

$migrated = $db->select('migrations', 'version');
$pending = array_diff($migrations, $migrated);

if (empty($pending)) {
    die('Nothing to migrate!');
}

array_walk($pending, function ($version) use ($db) {
    echo 'Migrating ', $version, '...', PHP_EOL;
    /** @noinspection PhpIncludeInspection */
    require __DIR__ . "/../migrations/$version.php";
    $db->insert('migrations', compact('version'));
});
