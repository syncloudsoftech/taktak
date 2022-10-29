<?php

require_once __DIR__ . '/../vendor/autoload.php';

use Cake\Chronos\Chronos;
use Embed\Embed;
use SimplePie\SimplePie;

$db = database();
$sections = $db->select('article_sections', ['id', 'google_news_topic'], ['google_news_topic[!]' => null]);
if (empty($sections)) {
    die('No news sections were added.');
}

foreach ($sections as $section) {
    $feed = new SimplePie();
    $feed->enable_cache(false);
    $feed->set_feed_url("https://news.google.com/rss/topics/{$section['google_news_topic']}?hl=en");
    $feed->init();
    foreach ($feed->get_items() as $item) {
        $guid = $item->get_id(true);
        if ($db->has('articles', compact('guid'))) continue;

        $link = $item->get_permalink();
        if (empty($link)) continue;

        $embed = Embed::create($link);
        if (empty($embed->imagesUrls)) continue;

        $snippet = $embed->description;
        if (mb_strlen($snippet) > 300) {
            $snippet = mb_substr($snippet, 0, 300);
        }

        $dateReported = Chronos::parse($item->get_date())
            ->timezone(DATE_TIMEZONE)
            ->format(DATE_MYSQL);
        $source = $item->get_item_tags(SimplePie::NAMESPACE_RSS_20, 'source');
        $row = [
            'section_id' => $section['id'],
            'guid' => $guid,
            'image' => $embed->imagesUrls[0],
            'publisher' => $source[0]['data'] ?? null,
            'snippet' => $snippet,
            'title' => $embed->title,
            'url' => $embed->url,
            'date_reported' => $dateReported,
        ];
        $row['date_created'] = $row['date_updated'] = date(DATE_MYSQL);
        database()->insert('articles', $row);
    }
}
