<?php

use Medoo\Medoo;

/** @var Medoo $db */

$db->create('threads', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
]);

$db->create('participants', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'thread_id' => ['INT', 'NOT NULL'],
    'user_id' => ['INT', 'NOT NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
    'CONSTRAINT participants_thread_id FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE',
    'CONSTRAINT participants_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
    'UNIQUE KEY (thread_id, user_id)',
]);

$db->create('messages', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'thread_id' => ['INT', 'NOT NULL'],
    'user_id' => ['INT', 'NOT NULL'],
    'text' => ['VARCHAR(300)', 'NOT NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
    'CONSTRAINT messages_thread_id FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE',
    'CONSTRAINT messages_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE',
]);
