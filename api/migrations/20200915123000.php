<?php

use Medoo\Medoo;

/** @var Medoo $db */

$db->create('article_sections', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'name' => ['VARCHAR(150)', 'NOT NULL'],
    'google_news_topic' => ['VARCHAR(200)', 'NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
]);

$db->create('articles', [
    'id' => ['INT', 'NOT NULL', 'AUTO_INCREMENT', 'PRIMARY KEY'],
    'section_id' => ['INT', 'NOT NULL'],
    'title' => ['VARCHAR(150)', 'NOT NULL'],
    'snippet' => ['VARCHAR(300)', 'NOT NULL'],
    'image' => ['VARCHAR(150)', 'NOT NULL'],
    'url' => ['VARCHAR(150)', 'NOT NULL'],
    'publisher' => ['VARCHAR(150)', 'NULL'],
    'date_reported' => ['DATETIME', 'NULL'],
    'guid' => ['VARCHAR(150)', 'NULL'],
    'date_created' => ['DATETIME', 'NULL'],
    'date_updated' => ['DATETIME', 'NULL'],
    'CONSTRAINT articles_section_id FOREIGN KEY (section_id) REFERENCES article_sections(id) ON DELETE CASCADE',
    'INDEX articles_guid_index (guid)',
]);
