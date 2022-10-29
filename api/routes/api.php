<?php

use Facebook\Facebook;
use Firebase\JWT\JWT;
use Medoo\Medoo;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Psr\Http\Server\RequestHandlerInterface as Handler;
use Rakit\Validation\Validator;
use Slim\Interfaces\RouteCollectorProxyInterface as Routes;
use Slim\Psr7\Factory\ResponseFactory;

/** @var Routes $api */

$api->post('/login/facebook', function (Request $request, Response $response) {
    $data = $request->getParsedBody();
    if (empty($data['access_token'])) {
        return $response->withStatus(401);
    }

    $fb = new Facebook([
        'app_id' => FACEBOOK_APP_ID,
        'app_secret' => FACEBOOK_APP_SECRET,
        'default_graph_version' => 'v2.10',
    ]);
    $result = $fb->get('/me?fields=name,email', $data['access_token']);
    $me = $result->getGraphUser();
    if (empty($me)) {
        return $response->withStatus(401);
    }

    $db = database();
    $user = $db->get('users', '*', ['facebook_id' => $me->getId()]);
    if (empty($user)) {
        $user = [
            'username' => 'user' . rand(10000000, 99999999),
            'password' => hash('sha256', random_string()),
            'role' => 'user',
            'name' => $me->getName(),
            'email' => $me->getEmail(),
            'verified' => 0,
            'facebook_id' => $me->getId(),
            'date_created' => date(DATE_MYSQL),
            'date_updated' => date(DATE_MYSQL),
        ];
        $db->insert('users', $user);
        $user['id'] = $db->id();
    }

    if (empty($user['photo'])) {
        $contents = download("https://graph.facebook.com/{$me->getId()}/picture?type=square");
        if ($contents) {
            $user['photo'] = random_string(24) . '.png';
            file_save($contents, DIR_PHOTOS, $user['photo']);
            $db->update('users', [
                'photo' => $user['photo'],
                'date_updated' => date(DATE_MYSQL),
            ], ['id' => $user['id']]);
        }
    }

    $jwt = JWT::encode(['id' => (int)$user['id']], JWT_SECRET);
    $json = json_encode(compact('jwt'));
    $response->getBody()->write($json);
    return $response->withHeader('Content-Type', 'application/json');
});

$api->post('/login/google', function (Request $request, Response $response) {
    $data = $request->getParsedBody();
    if (empty($data['id_token'])) {
        return $response->withStatus(401);
    }

    $client = new Google_Client(['client_id' => GOOGLE_CLIENT_ID]);
    $payload = $client->verifyIdToken($data['id_token']);
    if (empty($payload)) {
        return $response->withStatus(401);
    }

    $db = database();
    $user = $db->get('users', '*', ['google_id' => $payload['sub']]);
    if (empty($user)) {
        $user = [
            'username' => 'user' . rand(10000000, 99999999),
            'password' => hash('sha256', random_string()),
            'role' => 'user',
            'name' => $payload['name'],
            'email' => $payload['email'],
            'verified' => 0,
            'google_id' => $payload['sub'],
            'date_created' => date(DATE_MYSQL),
            'date_updated' => date(DATE_MYSQL),
        ];
        $db->insert('users', $user);
        $user['id'] = $db->id();
    }

    if (empty($user['photo']) && isset($payload['picture'])) {
        $contents = download($payload['picture']);
        if ($contents) {
            $user['photo'] = random_string(24) . '.png';
            file_save($contents, DIR_PHOTOS, $user['photo']);
            $db->update('users', [
                'photo' => $user['photo'],
                'date_updated' => date(DATE_MYSQL),
            ], ['id' => $user['id']]);
        }
    }

    $jwt = JWT::encode(['id' => (int)$user['id']], JWT_SECRET);
    $json = json_encode(compact('jwt'));
    $response->getBody()->write($json);
    return $response->withHeader('Content-Type', 'application/json');
});

$api->post('/devices', function (Request $request, Response $response) {
    $data = $request->getParsedBody();
    if (empty($data['token'])) {
        return $response->withStatus(401);
    }

    $user = $request->getAttribute('user');
    $db = database();
    $device = $db->get('devices', '*', ['token' => $data['token']]);
    if (empty($device)) {
        $db->insert('devices', [
            'user_id' => $user ? $user['id'] : null,
            'token' => $data['token'],
            'date_created' => date(DATE_MYSQL),
            'date_updated' => date(DATE_MYSQL),
        ]);
    } else if (empty($device['user_id']) && $user) {
        $db->update('devices', [
            'user_id' => $user['id'],
            'date_updated' => date(DATE_MYSQL),
        ], ['token' => $data['token']]);
    }

    return $response->withStatus(200);
});

$api->get('/users', function (Request $request, Response $response) {
    $query = $request->getQueryParams();
    $page = intval($query['page'] ?? 0);
    if ($page <= 0) {
        $page = 1;
    }

    $offset = ($page - 1) * 10;
    $q = $query['q'] ?? '';
    if ($q) {
        $where = [
            'OR' => [
                'username[~]' => $q,
                'name[~]' => $q,
            ],
        ];
    }

    $where['ORDER'] = [
        'date_created' => 'DESC',
    ];
    $where['LIMIT'] = [$offset, 10];
    $where['role[!]'] = 'admin';
    $db = database();
    $data = $db->select(
        'users',
        ['id', 'photo', 'username', 'name', 'verified', 'date_created'],
        $where
    );
    if (empty($data)) {
        $data = [];
    }

    foreach ($data as &$row) {
        if ($row['photo']) {
            $row['photo'] = file_url(DIR_PHOTOS, $row['photo']);
        }
    }

    unset($where['LIMIT']);
    $total = $db->count('users', $where);
    $pages = ceil($total / 10);
    $json = compact('data', 'page', 'pages', 'total');
    $response->getBody()->write(json_encode($json));
    return $response->withHeader('Content-Type', 'application/json');
});

$api->get('/users/{id}', function (Request $request, Response $response, array $args) {
    $db = database();
    $user = $db->get(
        'users',
        ['id', 'name', 'username', 'photo', 'bio', 'verified'],
        ['id' => $args['id']]
    );
    if (empty($user)) {
        return $response->withStatus(404);
    }

    if ($user['photo']) {
        $user['photo'] = file_url(DIR_PHOTOS, $user['photo']);
    }

    $self = $request->getAttribute('user');
    $user['self'] = $self && $self['id'] == $user['id'] ? 1 : 0;
    if (empty($self) || $self['id'] == $user['id']) {
        $user['following'] = 0;
    } else {
        $following = $db->has('followers', [
            'follower_id' => $self['id'],
            'following_id' => $user['id'],
        ]);
        $user['following'] = $following ? 1 : 0;
    }

    $user['videos'] = $db->count('videos', ['user_id' => $user['id']]);
    $user['followers'] = $db->count('followers', ['following_id' => $user['id']]);
    $user['followings'] = $db->count('followers', ['follower_id' => $user['id']]);
    if ($self && $self['id'] != $user['id']) {
        $threads = $db->get('participants', 'thread_id', ['user_id' => $self['id']]);
        if ($threads) {
            $thread = $db->get('participants', 'thread_id', [
                'thread_id' => $threads,
                'user_id' => $user['id'],
            ]);
            if ($thread) {
                $user['thread'] = $thread;
            }
        }
    }

    $response->getBody()->write(json_encode($user));
    return $response->withHeader('Content-Type', 'application/json');
});

$api->get('/users/{id}/followers', function (Request $request, Response $response, array $args) {
    $query = $request->getQueryParams();
    $page = intval($query['page'] ?? 0);
    $count = intval($query['count'] ?? 10);
    if ($page <= 0) {
        $page = 1;
    }

    $db = database();
    $offset = ($page - 1) * $count;
    $where['ORDER'] = [
        'users.date_created' => 'DESC',
    ];
    $where['LIMIT'] = [$offset, $count];
    $where['following_id'] = $args['id'];
    $data = $db->select(
        'followers',
        ['[>]users' => ['follower_id' => 'id']],
        ['users.id', 'users.name', 'users.username', 'users.photo', 'users.verified'],
        $where
    );
    if (empty($data)) {
        $data = [];
    }

    unset($where['ORDER'], $where['LIMIT']);
    $total = $db->count('followers', $where);
    $pages = ceil($total / $count);
    foreach ($data as &$row) {
        if ($row['photo']) {
            $row['photo'] = file_url(DIR_PHOTOS, $row['photo']);
        }
    }

    $json = compact('data', 'page', 'pages');
    $response->getBody()->write(json_encode($json));
    return $response->withHeader('Content-Type', 'application/json');
});

$api->get('/users/{id}/followings', function (Request $request, Response $response, array $args) {
    $query = $request->getQueryParams();
    $page = intval($query['page'] ?? 0);
    $count = intval($query['count'] ?? 10);
    if ($page <= 0) {
        $page = 1;
    }

    $db = database();
    $offset = ($page - 1) * $count;
    $where['ORDER'] = [
        'users.date_created' => 'DESC',
    ];
    $where['LIMIT'] = [$offset, $count];
    $where['follower_id'] = $args['id'];
    $data = $db->select(
        'followers',
        ['[>]users' => ['following_id' => 'id']],
        ['users.id', 'users.name', 'users.username', 'users.photo', 'users.verified'],
        $where
    );
    if (empty($data)) {
        $data = [];
    }

    unset($where['ORDER'], $where['LIMIT']);
    $total = $db->count('followers', $where);
    $pages = ceil($total / $count);
    foreach ($data as &$row) {
        if ($row['photo']) {
            $row['photo'] = file_url(DIR_PHOTOS, $row['photo']);
        }
    }

    $json = compact('data', 'page', 'pages');
    $response->getBody()->write(json_encode($json));
    return $response->withHeader('Content-Type', 'application/json');
});

$api->get('/videos', function (Request $request, Response $response) {
    $query = $request->getQueryParams();
    $page = intval($query['page'] ?? 0);
    $count = intval($query['count'] ?? 10);
    if ($page <= 0) {
        $page = 1;
    }

    $offset = ($page - 1) * $count;
    $q = $query['q'] ?? '';
    if ($q) {
        $where['description[~]'] = $q;
    }

    $section = $query['section'] ?? '';
    if ($section) {
        $where['videos.section_id'] = $section;
    }

    $self = $request->getAttribute('user');
    $user = $query['user'] ?? '';
    if ($user) {
        $where['videos.user_id'] = $user;
        if (empty($self) || $user != $self['id']) {
            $where['private'] = 0;
        }
    } else {
        $where['private'] = 0;
    }

    $db = database();
    $liked = $query['liked'] ?? '';
    if ($liked && $self) {
        $where['videos.id'] = $db->select('likes', 'video_id', ['user_id' => $self['id']]);
    }

    $following = $query['following'] ?? '';
    if ($following && $self) {
        $where['videos.user_id'] = $db->select('followers', 'following_id', ['follower_id' => $self['id']]);
    }

    $where['ORDER'] = [
        'videos.date_created' => 'DESC',
    ];
    $where['LIMIT'] = [$offset, $count];
    $data = $db->select(
        'videos',
        [
            '[>]users' => ['user_id' => 'id'],
            '[>]songs' => ['song_id' => 'id'],
            '[>]video_sections' => ['section_id' => 'id'],
        ],
        [
            'videos.id (id)',
            'description',
            'video',
            'preview',
            'screenshot',
            'private',
            'comments',
            'song_id',
            'songs.name (song_name)',
            'videos.user_id',
            'users.username (user_username)',
            'users.photo (user_photo)',
            'users.verified (user_verified)',
            'videos.date_created',
        ],
        $where
    );
    if (empty($data)) {
        $data = [];
    }

    unset($where['LIMIT']);
    $total = $db->count('videos', $where);
    $pages = ceil($total / $count);
    foreach ($data as &$row) {
        $row['preview'] = DIR_PREVIEWS. '/' . $row['preview'];
        $row['screenshot'] = file_url(DIR_SCREENSHOTS, $row['screenshot']);
        $row['video'] = file_url(DIR_VIDEOS, $row['video']);
        if ($row['user_photo']) {
            $row['user_photo'] = file_url(DIR_PHOTOS, $row['user_photo']);
        }

        if ($self) {
            $liked = $db->has('likes', ['user_id' => $self['id'], 'video_id' => $row['id']]) ? 1 : 0;
        } else {
            $liked = 0;
        }

        $row['is_liked'] = $liked;
        $row['likes_count'] = $db->count('likes', ['video_id' => $row['id']]);
        $row['comments_count'] = $db->count('comments', ['video_id' => $row['id']]);
    }

    $json = compact('data', 'page', 'pages');
    $response->getBody()->write(json_encode($json));
    return $response->withHeader('Content-Type', 'application/json');
});

$api->get('/videos/{id}', function (Request $request, Response $response, array $args) {
    $user = $request->getAttribute('user');
    if ($user) {
        $where['OR'] = [
            'videos.user_id' => $user['id'],
            'private' => 0,
        ];
    } else {
        $where['private'] = 0;
    }

    $where['videos.id'] = $args['id'];
    $db = database();
    $video = $db->get(
        'videos',
        [
            '[>]users' => ['user_id' => 'id'],
            '[>]songs' => ['song_id' => 'id'],
            '[>]video_sections' => ['section_id' => 'id'],
        ],
        [
            'videos.id (id)',
            'description',
            'video',
            'preview',
            'screenshot',
            'private',
            'comments',
            'song_id',
            'songs.name (song_name)',
            'videos.user_id',
            'users.username (user_username)',
            'users.photo (user_photo)',
            'users.verified (user_verified)',
            'videos.date_created',
        ],
        $where
    );
    if (empty($video)) {
        return $response->withStatus(404);
    }

    $video['preview'] = DIR_PREVIEWS. '/' . $video['preview'];
    $video['screenshot'] = file_url(DIR_SCREENSHOTS, $video['screenshot']);
    $video['video'] = file_url(DIR_VIDEOS, $video['video']);
    if ($video['user_photo']) {
        $video['user_photo'] = file_url(DIR_PHOTOS, $video['user_photo']);
    }

    if ($user) {
        $liked = $db->has('likes', ['user_id' => $user['id'], 'video_id' => $video['id']]) ? 1 : 0;
    } else {
        $liked = 0;
    }

    $video['is_liked'] = $liked;
    $video['likes_count'] = $db->count('likes', ['video_id' => $video['id']]);
    $video['comments_count'] = $db->count('comments', ['video_id' => $video['id']]);
    $response->getBody()->write(json_encode($video));
    return $response->withHeader('Content-Type', 'application/json');
});

$api->get('/videos/{id}/comments', function (Request $request, Response $response, array $args) {
    $query = $request->getQueryParams();
    $page = intval($query['page'] ?? 0);
    $count = intval($query['count'] ?? 10);
    if ($page <= 0) {
        $page = 1;
    }

    $offset = ($page - 1) * $count;
    $where['video_id'] = $args['id'];
    $where['ORDER'] = [
        'comments.date_created' => 'DESC',
    ];
    $where['LIMIT'] = [$offset, $count];
    $db = database();
    $data = $db->select(
        'comments',
        [
            '[>]users' => ['user_id' => 'id'],
            '[>]videos' => ['video_id' => 'id'],
        ],
        [
            'comments.id (id)',
            'text',
            'comments.user_id',
            'users.username (user_username)',
            'users.photo (user_photo)',
            'users.verified (user_verified)',
            'comments.date_created',
        ],
        $where
    );
    if (empty($data)) {
        $data = [];
    }

    $total = $db->count('comments', $where);
    $pages = ceil($total / $count);
    foreach ($data as &$row) {
        if ($row['user_photo']) {
            $row['user_photo'] = file_url(DIR_PHOTOS, $row['user_photo']);
        }
    }

    $json = compact('data', 'page', 'pages');
    $response->getBody()->write(json_encode($json));
    return $response->withHeader('Content-Type', 'application/json');
});

$api->get('/articles', function (Request $request, Response $response) {
    $query = $request->getQueryParams();
    $page = intval($query['page'] ?? 0);
    if ($page <= 0) {
        $page = 1;
    }

    $offset = ($page - 1) * 10;
    $q = $query['q'] ?? '';
    if ($q) {
        $where = [
            'OR' => [
                'articles.title[~]' => $q,
                'articles.snippet[~]' => $q,
            ],
        ];
    }

    $sections = $query['sections'] ?? '';
    if ($sections) {
        $where['articles.section_id'] = explode(',', $sections);
    }

    $where['ORDER'] = [
        'articles.date_reported' => 'DESC',
    ];
    $where['LIMIT'] = [$offset, 10];
    $db = database();
    $data = $db->select(
        'articles',
        [
            '[>]article_sections' => ['section_id' => 'id'],
        ],
        [
            'articles.id (id)',
            'title',
            'snippet',
            'image',
            'url',
            'publisher',
            'date_reported',
            'section_id',
            'article_sections.name (section_name)',
            'articles.date_created',
        ],
        $where
    );
    if (empty($data)) {
        $data = [];
    }

    unset($where['LIMIT']);
    $total = $db->count('articles', $where);
    $pages = ceil($total / 10);
    $json = compact('data', 'page', 'pages', 'total');
    $response->getBody()->write(json_encode($json));
    return $response->withHeader('Content-Type', 'application/json');
});

foreach (['video', 'song', 'article'] as $_) {
    $api->get("/$_-sections", function (Request $request, Response $response) use ($_) {
        $query = $request->getQueryParams();
        $page = intval($query['page'] ?? 0);
        if ($page <= 0) {
            $page = 1;
        }

        $count = intval($query['count'] ?? 10);
        $offset = ($page - 1) * $count;
        $where['ORDER'] = [
            'name' => 'ASC',
        ];
        $where['LIMIT'] = [$offset, $count];
        $db = database();
        $data = $db->select($_ . '_sections', ['id', 'name', 'date_created'], $where);
        if (empty($data)) {
            $data = [];
        }

        unset($where['LIMIT']);
        $total = $db->count($_ . '_sections', $where);
        $pages = ceil($total / $count);
        $json = compact('data', 'page', 'pages', 'total');
        $response->getBody()->write(json_encode($json));
        return $response->withHeader('Content-Type', 'application/json');
    });
}

$secured = $api->group('', function (Routes $api) {

    $api->get('/self', function (Request $request, Response $response) {
        $db = database();
        $user = $request->getAttribute('user');
        $user = $db->get(
            'users',
            ['id', 'name', 'username', 'email', 'photo', 'bio', 'verified'],
            ['id' => $user['id']]
        );
        if ($user['photo']) {
            $user['photo'] = file_url(DIR_PHOTOS, $user['photo']);
        }

        $user['self'] = 1;
        $user['following'] = 0;
        $user['videos'] = $db->count('videos', ['user_id' => $user['id']]);
        $user['followers'] = $db->count('followers', ['following_id' => $user['id']]);
        $user['followings'] = $db->count('followers', ['follower_id' => $user['id']]);
        $response->getBody()->write(json_encode($user));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->post('/self', function (Request $request, Response $response) {
        $validator = new Validator();
        $validation = $validator->validate($data = $_POST + $_FILES, [
            'photo' => 'uploaded_file:0,1024K,png',
            'username' => 'required|min:3|max:20',
            'password' => 'min:5|max:25',
            'name' => 'required|min:2|max:150',
            'email' => 'email|max:150',
            'bio' => 'max:300',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $db = database();
        $user = $request->getAttribute('user');
        $existing = $db->has('users', [
            'id[!]' => $user['id'],
            'username' => $data['username'],
        ]);
        if ($existing) {
            $errors = json_encode(['username' => ['exists' => 'This Username is taken.']]);
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $updates = [
            'username' => $data['username'],
            'name' => $data['name'],
            'email' => $data['email'],
            'bio' => $data['bio'],
            'date_updated' => date(DATE_MYSQL),
        ];
        if ($data['photo'] ?? null) {
            $updates['photo'] = random_string(24) . '.png';
            file_upload($data['photo']['tmp_name'], DIR_PHOTOS, $updates['photo']);
        } else if ($data['remove'] ?? null) {
            $updates['photo'] = null;
        }

        if ($data['password'] ?? null) {
            $updates['password'] = hash('sha256', $data['password']);
        }

        $updated = $db->update('users', $updates, ['id' => $user['id']]);
        if ($updated && array_key_exists('photo', $updates)) {
            file_delete(DIR_PHOTOS, $user['photo']);
        }

        return $response->withStatus($updated ? 200 : 500);
    });

    $api->post('/users/{id}/follow', function (Request $request, Response $response, array $args) {
        $db = database();
        $user = $request->getAttribute('user');
        $following = $db->has('followers', [
            'follower_id' => $user['id'],
            'following_id' => $args['id'],
        ]);
        if (!$following) {
            $db->insert('followers', [
                'follower_id' => $user['id'],
                'following_id' => $args['id'],
            ]);
            notify('followed_you', $user['id'], $args['id'], null);
        }

        return $response;
    });

    $api->delete('/users/{id}/follow', function (Request $request, Response $response, array $args) {
        $user = $request->getAttribute('user');
        database()->delete('followers', [
            'follower_id' => $user['id'],
            'following_id' => $args['id'],
        ]);
        return $response;
    });

    $api->post('/videos', function (Request $request, Response $response) {
        $validator = new Validator();
        $validation = $validator->validate($data = $_POST + $_FILES, [
            'song_id' => 'integer',
            'description' => 'max:300',
            'video' => 'required|uploaded_file:0,15360K,mp4',
            'screenshot' => 'required|uploaded_file:0,1024K,png',
            'preview' => 'required|uploaded_file:0,5120K,gif',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $user = $request->getAttribute('user');
        $video = [
            'user_id' => $user['id'],
            'song_id' => $data['song_id'] ?? null,
            'description' => $data['description'] ?? null,
            'private' => empty($data['private']) ? 0 : 1,
            'comments' => empty($data['comments']) ? 0 : 1,
            'date_created' => date(DATE_MYSQL),
            'date_updated' => date(DATE_MYSQL),
        ];
        $video['preview'] = random_string(24) . '.gif';
        file_upload($data['preview']['tmp_name'], DIR_PREVIEWS, $video['preview']);
        $video['screenshot'] = random_string(24) . '.png';
        file_upload($data['screenshot']['tmp_name'], DIR_SCREENSHOTS, $video['screenshot']);
        $video['video'] = random_string(24) . '.mp4';
        file_upload($data['video']['tmp_name'], DIR_VIDEOS, $video['video']);
        $db = database();
        $created = $db->insert('videos', $video);
        if ($created) {
            $video['id'] = $db->id();
            $video['preview'] = file_url(DIR_PREVIEWS, $video['preview']);
            $video['screenshot'] = file_url(DIR_SCREENSHOTS, $video['screenshot']);
            $video['video'] = file_url(DIR_VIDEOS, $video['video']);
            $response->getBody()->write(json_encode($video));
            return $response->withHeader('Content-Type', 'application/json');
        }

        return $response->withStatus(500);
    });

    $api->post('/videos/{id}/like', function (Request $request, Response $response, array $args) {
        $db = database();
        $video = $db->get('videos', '*', ['id' => $args['id']]);
        if (empty($video)) {
            return $response->withStatus(404);
        }

        $user = $request->getAttribute('user');
        $liked = $db->has('likes', [
            'user_id' => $user['id'],
            'video_id' => $args['id'],
        ]);
        if (!$liked) {
            $db->insert('likes', [
                'user_id' => $user['id'],
                'video_id' => $args['id'],
            ]);
            notify('liked_video', $user['id'], $video['user_id'], $video['id']);
        }

        return $response;
    });

    $api->delete('/videos/{id}/like', function (Request $request, Response $response, array $args) {
        $user = $request->getAttribute('user');
        database()->delete('likes', [
            'user_id' => $user['id'],
            'video_id' => $args['id'],
        ]);
        return $response;
    });

    $api->post('/videos/{id}/comments', function (Request $request, Response $response, array $args) {
        $data = $request->getParsedBody();
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'text' => 'required|max:300',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $db = database();
        $video = $db->get('videos', '*', [
            'id' => $args['id'],
            'comments' => 1,
        ]);
        if (empty($video)) {
            return $response->withStatus(404);
        }

        $user = $request->getAttribute('user');
        $comment = [
            'user_id' => $user['id'],
            'video_id' => $video['id'],
            'text' => $data['text'],
            'date_created' => date(DATE_MYSQL),
            'date_updated' => date(DATE_MYSQL),
        ];
        $created = $db->insert('comments', $comment);
        if ($created) {
            notify('commented_on_video', $user['id'], $video['user_id'], $video['id']);
            $comment['id'] = $db->id();
            $response->getBody()->write(json_encode($comment));
            return $response->withHeader('Content-Type', 'application/json');
        }

        return $response->withStatus(500);
    });

    $api->get('/threads', function (Request $request, Response $response) {
        $query = $request->getQueryParams();
        $page = intval($query['page'] ?? 0);
        $count = intval($query['count'] ?? 10);
        if ($page <= 0) {
            $page = 1;
        }

        $offset = ($page - 1) * $count;
        $self = $request->getAttribute('user');
        $db = database();
        $where['threads.id'] = $db->select('participants', 'thread_id', ['user_id' => $self['id']]);
        $where['ORDER'] = [
            'threads.date_updated' => 'DESC',
        ];
        $where['LIMIT'] = [$offset, $count];
        $data = $db->select(
            'threads',
            [
                'threads.id (id)',
                'threads.date_created',
            ],
            $where
        );
        if (empty($data)) {
            $data = [];
        }

        unset($where['LIMIT']);
        $total = $db->count('threads', $where);
        $pages = ceil($total / $count);
        foreach ($data as &$row) {
            $participant = $db->get('participants', 'user_id', [
                'thread_id' => $row['id'],
                'user_id[!]' => $self['id'],
            ]);
            $user = $db->get('users', ['id', 'username', 'photo', 'verified'], ['id' => $participant]);
            $row['user_id'] = $user['id'];
            $row['user_username'] = $user['username'];
            $row['user_photo'] = $user['photo'];
            $row['user_verified'] = $user['verified'];
            $message = $db->get(
                'messages',
                ['text', 'date_created'],
                [
                    'thread_id' => $row['id'],
                    'ORDER' => [
                        'date_created' => 'DESC',
                    ],
                ]
            );
            if ($message) {
                $row['last_message_text'] = $message['text'];
                $row['last_message_date_created'] = $message['date_created'];
            }

            if ($row['user_photo']) {
                $row['user_photo'] = file_url(DIR_PHOTOS, $row['user_photo']);
            }
        }

        $json = compact('data', 'page', 'pages');
        $response->getBody()->write(json_encode($json));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->post('/threads', function (Request $request, Response $response) {
        $validator = new Validator();
        $validation = $validator->validate($data = $_POST + $_FILES, [
            'user_id' => 'required|integer',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $db = database();
        $user = $db->get('users', '*', ['id' => $data['user_id']]);
        if (empty($user)) {
            $errors = json_encode([
                'user_id' => ['Exists' => 'This user does not exist.']
            ]);
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $self = $request->getAttribute('user');
        $threads = $db->select('participants', 'thread_id', ['user_id' => $user['id']]);
        if ($threads) {
            $thread = $db->get('participants', 'thread_id', [
                'thread_id' => $threads,
                'user_id' => $self['id'],
            ]);
            if ($thread) {
                $thread = $db->get('threads', '*', ['id' => $thread]);
            }
        }

        if (empty($thread)) {
            $thread = [];
            $thread['date_created'] = $thread['date_updated'] = date(DATE_MYSQL);
            $db->insert('threads', $thread);
            $thread['id'] = $db->id();
            $db->insert('participants', [
                'thread_id' => $thread['id'],
                'user_id' => $self['id'],
                'date_created' => date(DATE_MYSQL),
                'date_updated' => date(DATE_MYSQL),
            ]);
            $db->insert('participants', [
                'thread_id' => $thread['id'],
                'user_id' => $user['id'],
                'date_created' => date(DATE_MYSQL),
                'date_updated' => date(DATE_MYSQL),
            ]);
        }

        $thread['user_id'] = $user['id'];
        $thread['user_username'] = $user['username'];
        $thread['user_photo'] = $user['photo'];
        $thread['user_verified'] = $user['verified'];
        if ($thread['user_photo']) {
            $thread['user_photo'] = file_url(DIR_PHOTOS, $thread['user_photo']);
        }

        $response->getBody()->write(json_encode($thread));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->get('/threads/{thread}/messages', function (Request $request, Response $response, array $args) {
        $db = database();
        $thread = $db->get('threads', '*', ['id' => $args['thread']]);
        if (empty($thread)) {
            return $response->withStatus(404);
        }

        $self = $request->getAttribute('user');
        $accessible = $db->has('participants', [
            'thread_id' => $thread['id'],
            'user_id' => $self['id'],
        ]);
        if (!$accessible) {
            return $response->withStatus(403);
        }

        $query = $request->getQueryParams();
        $page = intval($query['page'] ?? 0);
        if ($page <= 0) {
            $page = 1;
        }

        $offset = ($page - 1) * 10;
        $where['messages.thread_id'] = $thread['id'];
        $where['ORDER'] = [
            'messages.date_created' => 'DESC',
        ];
        $where['LIMIT'] = [$offset, 10];
        $data = $db->select(
            'messages',
            [
                '[>]users' => ['user_id' => 'id'],
            ],
            [
                'messages.id (id)',
                'user_id',
                'users.username (user_username)',
                'users.photo (user_photo)',
                'users.verified (user_verified)',
                'text',
                'inbox' => Medoo::raw('CASE WHEN user_id = :id THEN 0 ELSE 1 END', [':id' => $self['id']]),
                'messages.date_created',
            ],
            $where
        );
        if (empty($data)) {
            $data = [];
        }

        unset($where['LIMIT']);
        $total = $db->count('messages', $where);
        $pages = ceil($total / 10);
        foreach ($data as &$row) {
            if ($row['user_photo']) {
                $row['user_photo'] = file_url(DIR_PHOTOS, $row['user_photo']);
            }
        }

        $json = compact('data', 'page', 'pages');
        $response->getBody()->write(json_encode($json));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->post('/threads/{thread}/messages', function (Request $request, Response $response, array $args) {
        $data = $request->getParsedBody();
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'text' => 'required|max:300',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $db = database();
        $thread = $db->get('threads', '*', ['id' => $args['thread']]);
        if (empty($thread)) {
            return $response->withStatus(404);
        }

        $self = $request->getAttribute('user');
        $accessible = $db->has('participants', [
            'thread_id' => $thread['id'],
            'user_id' => $self['id'],
        ]);
        if (!$accessible) {
            return $response->withStatus(403);
        }

        $created = $db->insert('messages', [
            'thread_id' => $thread['id'],
            'user_id' => $self['id'],
            'text' => $data['text'],
            'date_created' => date(DATE_MYSQL),
            'date_updated' => date(DATE_MYSQL),
        ]);
        if ($created) {
            $db->update(
                'threads',
                ['date_updated' => date(DATE_MYSQL)],
                ['id' => $thread['id']]
            );
            $participant = $db->get('participants', 'user_id', [
                'thread_id' => $thread['id'],
                'user_id[!]' => $self['id'],
            ]);
            notify('sent_message', $self['id'], $participant, null, [
                'content' => 'sent_message',
                'thread_id' => (string)$thread['id'],
            ]);
        }

        return $response->withStatus($created ? 200 : 500);
    });

    $api->get('/songs', function (Request $request, Response $response) {
        $query = $request->getQueryParams();
        $page = intval($query['page'] ?? 0);
        if ($page <= 0) {
            $page = 1;
        }

        $offset = ($page - 1) * 10;
        $section = $query['section'] ?? '';
        if ($section) {
            $where['songs.section_id'] = $section;
        }

        $where['ORDER'] = [
            'songs.date_created' => 'DESC',
        ];
        $where['LIMIT'] = [$offset, 10];
        $db = database();
        $data = $db->select(
            'songs',
            [
                '[>]song_sections' => ['section_id' => 'id'],
            ],
            [
                'songs.id (id)',
                'songs.name',
                'audio',
                'icon',
                'section_id',
                'song_sections.name (section_name)',
                'songs.date_created',
            ],
            $where
        );
        if (empty($data)) {
            $data = [];
        }

        unset($where['LIMIT']);
        $total = $db->count('songs', $where);
        $pages = ceil($total / 10);
        foreach ($data as &$row) {
            $row['audio'] = file_url(DIR_AUDIOS, $row['audio']);
            if ($row['icon']) {
                $row['icon'] = file_url(DIR_ICONS, $row['icon']);
            }
        }

        $json = compact('data', 'page', 'pages');
        $response->getBody()->write(json_encode($json));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->get('/notifications', function (Request $request, Response $response) {
        $query = $request->getQueryParams();
        $page = intval($query['page'] ?? 0);
        if ($page <= 0) {
            $page = 1;
        }

        $offset = ($page - 1) * 10;
        $user = $request->getAttribute('user');
        $where['target_id'] = $user['id'];
        $where['ORDER'] = [
            'notifications.date_created' => 'DESC',
        ];
        $where['LIMIT'] = [$offset, 10];
        $db = database();
        $data = $db->select(
            'notifications',
            [
                '[>]users (sources)' => ['source_id' => 'id'],
                '[>]videos' => ['video_id' => 'id'],
            ],
            [
                'notifications.id (id)',
                'content',
                'source_id',
                'sources.username (source_username)',
                'sources.photo (source_photo)',
                'video_id',
                'videos.screenshot (video_screenshot)',
                'notifications.date_created',
            ],
            $where
        );
        if (empty($data)) {
            $data = [];
        }

        unset($where['LIMIT']);
        $total = $db->count('notifications', $where);
        $pages = ceil($total / 10);
        foreach ($data as &$row) {
            $row['source_photo'] = file_url(DIR_PHOTOS, $row['source_photo']);
            if ($row['video_screenshot']) {
                $row['video_screenshot'] = file_url(DIR_SCREENSHOTS, $row['video_screenshot']);
            }
        }

        $json = compact('data', 'page', 'pages');
        $response->getBody()->write(json_encode($json));
        return $response->withHeader('Content-Type', 'application/json');
    });
});

$secured->add(function (Request $request, Handler $handler) {
    $user = $request->getAttribute('user');
    if (empty($user)) {
        return (new ResponseFactory())->createResponse(401);
    }

    return $handler->handle($request);
});
