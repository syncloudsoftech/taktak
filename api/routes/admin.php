<?php

use Firebase\JWT\JWT;
use Kreait\Firebase\Messaging\CloudMessage;
use Kreait\Firebase\Messaging\Notification;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Psr\Http\Server\RequestHandlerInterface as Handler;
use Rakit\Validation\Validator;
use Slim\Interfaces\RouteCollectorProxyInterface as Routes;
use Slim\Psr7\Factory\ResponseFactory;

/** @var Routes $api */

$api->post('/login', function (Request $request, Response $response) {
    $data = json_decode($request->getBody(), true);
    $validator = new Validator();
    $validation = $validator->validate((array)$data, [
        'username' => 'required|min:3|max:20',
        'password' => 'required|min:5|max:25',
    ]);
    if ($validation->fails()) {
        $errors = json_encode($validation->errors()->toArray());
        $response->getBody()->write($errors);
        return $response
            ->withHeader('Content-Type', 'application/json')
            ->withStatus(422);
    }

    $db = database();
    $user = $db->get('users', '*', ['username' => $data['username']]);
    if (empty($user) || !hash_equals($user['password'], hash('sha256', $data['password']))) {
        $json = json_encode(['username' => ['invalid' => 'Username or Password is incorrect.']]);
        $response->getBody()->write($json);
        return $response
            ->withHeader('Content-Type', 'application/json')
            ->withStatus(422);
    }

    $jwt = JWT::encode(['id' => (int)$user['id']], JWT_SECRET);
    $json = json_encode(compact('jwt'));
    $response->getBody()->write($json);
    return $response->withHeader('Content-Type', 'application/json');
});

$secured = $api->group('', function (Routes $api) {

    $api->get('/dashboard', function (Request $request, Response $response) {
        $db = database();
        $data = [
            'users' => $db->count('users'),
            'videos' => $db->count('videos'),
            'likes' => $db->count('likes'),
            'comments' => $db->count('comments'),
        ];
        $response->getBody()->write(json_encode($data));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->get('/self', function (Request $request, Response $response) {
        $user = $request->getAttribute('user');
        unset($user['password']);
        $response->getBody()->write(json_encode($user));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->put('/self', function (Request $request, Response $response) {
        $data = json_decode($request->getBody(), true);
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'username' => 'required|min:3|max:20',
            'password' => 'min:5|max:25',
            'name' => 'required|min:2|max:150',
            'email' => 'email|max:150',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $user = $request->getAttribute('user');
        $db = database();
        $updates = [
            'username' => $data['username'],
            'name' => $data['name'],
            'email' => $data['email'],
            'date_updated' => date(DATE_MYSQL),
        ];
        if ($data['password'] ?? null) {
            $updates['password'] = hash('sha256', $data['password']);
        }

        $updated = $db->update('users', $updates, ['id' => $user['id']]);
        if ($updated) {
            $user = array_replace($user, $updates);
            unset($user['password']);
            $response->getBody()->write(json_encode($user));
            return $response->withHeader('Content-Type', 'application/json');
        }

        return $response->withStatus(500);
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
                    'email[~]' => $q,
                ],
            ];
        }

        $where['ORDER'] = [
            'date_created' => 'DESC',
        ];
        $where['LIMIT'] = [$offset, 10];
        $db = database();
        $data = $db->select(
            'users',
            ['id', 'photo', 'username', 'name', 'email', 'role', 'verified', 'date_created'],
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
        $user = database()->get('users', '*', ['id' => $args['id']]);
        if (empty($user)) {
            return $response->withStatus(404);
        }

        unset($user['password']);
        $response->getBody()->write(json_encode($user));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->put('/users/{id}', function (Request $request, Response $response, array $args) {
        $data = json_decode($request->getBody(), true);
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'username' => 'required|min:3|max:20',
            'password' => 'min:5|max:25',
            'name' => 'required|min:2|max:150',
            'email' => 'email|max:150',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $self = $request->getAttribute('user');
        if ($args['id'] === $self['id']) {
            return $response->withStatus(403);
        }

        $updates = [
            'username' => $data['username'],
            'name' => $data['name'],
            'email' => $data['email'],
            'verified' => $data['verified'] ?? false,
            'date_updated' => date(DATE_MYSQL),
        ];
        if ($data['password'] ?? null) {
            $updates['password'] = hash('sha256', $data['password']);
        }

        $updated = database()->update('users', $updates, ['id' => $args['id']]);
        return $response->withStatus($updated ? 200 : 500);
    });

    $api->delete('/users/{id}', function (Request $request, Response $response, array $args) {
        $self = $request->getAttribute('user');
        if ($args['id'] === $self['id']) {
            return $response->withStatus(403);
        }

        $deleted = database()->delete('users', ['id' => $args['id']]);
        return $response->withStatus($deleted ? 200 : 500);
    });

    $api->get('/videos', function (Request $request, Response $response) {
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
                    'description[~]' => $q,
                ],
            ];
        }

        $where['ORDER'] = [
            'videos.date_created' => 'DESC',
        ];
        $where['LIMIT'] = [$offset, 10];
        $db = database();
        $data = $db->select(
            'videos',
            [
                '[>]users' => ['user_id' => 'id'],
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
                'user_id',
                'users.username (user_username)',
                'section_id',
                'video_sections.name (section_name)',
                'videos.date_created',
            ],
            $where
        );
        if (empty($data)) {
            $data = [];
        }

        unset($where['LIMIT']);
        $total = $db->count('videos', $where);
        $pages = ceil($total / 10);
        foreach ($data as &$row) {
            $row['preview'] = DIR_PREVIEWS. '/' . $row['preview'];
            $row['screenshot'] = file_url(DIR_SCREENSHOTS, $row['screenshot']);
            $row['video'] = file_url(DIR_VIDEOS, $row['video']);
        }

        $json = compact('data', 'page', 'pages', 'total');
        $response->getBody()->write(json_encode($json));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->get('/videos/{id}', function (Request $request, Response $response, array $args) {
        $db = database();
        $video = $db->get(
            'videos',
            [
                '[>]users' => ['user_id' => 'id'],
                '[>]video_sections' => ['section_id' => 'id'],
            ],
            [
                'videos.id (id)',
                'description',
                'video',
                'screenshot',
                'preview',
                'private',
                'comments',
                'user_id',
                'users.username (user_username)',
                'section_id',
                'video_sections.name (section_name)',
                'videos.date_created',
            ],
            ['videos.id' => $args['id']]
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

        $video['likes_count'] = $db->count('likes', ['video_id' => $video['id']]);
        $video['comments_count'] = $db->count('comments', ['video_id' => $video['id']]);
        $response->getBody()->write(json_encode($video));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->put('/videos/{id}', function (Request $request, Response $response, array $args) {
        $data = json_decode($request->getBody(), true);
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'section_id' => 'integer',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        if (empty($data['section_id'])) {
            $data['section_id'] = null;
        }

        $updates = [
            'section_id' => $data['section_id'],
            'date_updated' => date(DATE_MYSQL),
        ];
        $updated = database()->update('videos', $updates, ['id' => $args['id']]);
        return $response->withStatus($updated ? 200 : 500);
    });

    $api->delete('/videos/{id}', function (Request $request, Response $response, array $args) {
        $db = database();
        $video = $db->get('videos', '*', ['id' => $args['id']]);
        if (empty($video)) {
            return $response->withStatus(404);
        }

        $deleted = $db->delete('videos', ['id' => $video['id']]);
        if ($deleted) {
            file_delete(DIR_PREVIEWS, $video['preview']);
            file_delete(DIR_SCREENSHOTS, $video['screenshot']);
            file_delete(DIR_VIDEOS, $video['video']);
        }

        return $response->withStatus($deleted ? 200 : 500);
    });

    $api->get('/songs', function (Request $request, Response $response) {
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
                    'songs.name[~]' => $q,
                ],
            ];
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

            $row['videos'] = $db->count('videos', ['song_id' => $row['id']]);
        }

        $json = compact('data', 'page', 'pages', 'total');
        $response->getBody()->write(json_encode($json));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->get('/songs/{id}', function (Request $request, Response $response, array $args) {
        $db = database();
        $song = $db->get(
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
            ['songs.id' => $args['id']]
        );
        if (empty($song)) {
            return $response->withStatus(404);
        }

        $song['audio'] = file_url(DIR_AUDIOS, $song['audio']);
        if ($song['icon']) {
            $song['icon'] = file_url(DIR_ICONS, $song['icon']);
        }

        $song['videos'] = $db->count('videos', ['song_id' => $song['id']]);
        $response->getBody()->write(json_encode($song));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->post('/songs', function (Request $request, Response $response) {
        $validator = new Validator();
        $validation = $validator->validate($data = $_POST + $_FILES, [
            'section_id' => 'required|integer',
            'name' => 'required|max:150',
            'audio' => 'required|uploaded_file:0,1024K',
            'icon' => 'uploaded_file:0,500K,png',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $song = [
            'section_id' => $data['section_id'],
            'name' => $data['name'],
            'date_created' => date(DATE_MYSQL),
            'date_updated' => date(DATE_MYSQL),
        ];
        $song['audio'] = random_string(24) . '.aac';
        file_upload($data['audio']['tmp_name'], DIR_AUDIOS, $song['audio']);
        if (!empty($data['icon'])) {
            $song['icon'] = random_string(24) . '.png';
            file_upload($data['icon']['tmp_name'], DIR_ICONS, $song['icon']);
        }

        $created = database()->insert('songs', $song);
        return $response->withStatus($created ? 200 : 500);
    });

    $api->put('/songs/{id}', function (Request $request, Response $response, array $args) {
        $validator = new Validator();
        $validation = $validator->validate($data = $_POST + $_FILES, [
            'section_id' => 'required|integer',
            'name' => 'required|max:150',
            'audio' => 'uploaded_file:0,1024K',
            'icon' => 'uploaded_file:0,500K,png',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $db = database();
        $song = $db->get('songs', '*', ['id' => $args['id']]);
        if (empty($song)) {
            return $response->withStatus(404);
        }

        $updates = [
            'section_id' => $data['section_id'],
            'name' => $data['name'],
            'date_updated' => date(DATE_MYSQL),
        ];
        if (!empty($data['audio'])) {
            $updates['audio'] = random_string(24) . '.aac';
            file_upload($data['audio']['tmp_name'], DIR_AUDIOS, $updates['audio']);
        }

        if (!empty($data['icon'])) {
            $updates['icon'] = random_string(24) . '.png';
            file_upload($data['icon']['tmp_name'], DIR_ICONS, $updates['icon']);
        }

        $updated = $db->update('songs', $updates, ['id' => $song['id']]);
        if ($updated) {
            if ($updates['audio'] ?? null) {
                file_delete(DIR_AUDIOS, $song['audio']);
            }

            if ($updates['icon']) {
                file_delete(DIR_ICONS, $song['icon']);
            }
        }

        return $response->withStatus($updated ? 200 : 500);
    });

    $api->delete('/songs/{id}', function (Request $request, Response $response, array $args) {
        $db = database();
        $song = $db->get('songs', ['audio', 'icon'], ['id' => $args['id']]);
        if (empty($song)) {
            return $response->withStatus(404);
        }

        $deleted = $db->delete('songs', ['id' => $args['id']]);
        if ($deleted) {
            file_delete(DIR_AUDIOS, $song['audio']);
            if ($song['icon']) {
                file_delete(DIR_ICONS, $song['icon']);
            }
        }

        return $response->withStatus($deleted ? 200 : 500);
    });

    foreach (['video', 'song'] as $_) {
        $api->get("/$_-sections", function (Request $request, Response $response) use ($_) {
            $query = $request->getQueryParams();
            $page = intval($query['page'] ?? 0);
            if ($page <= 0) {
                $page = 1;
            }

            $count = intval($query['count'] ?? 10);
            $offset = ($page - 1) * $count;
            $q = $query['q'] ?? '';
            if ($q) {
                $where = [
                    'OR' => [
                        'name[~]' => $q,
                    ],
                ];
            }

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
            foreach ($data as &$row) {
                $row[$_ . 's'] = $db->count($_ . 's', ['section_id' => $row['id']]);
            }

            $json = compact('data', 'page', 'pages', 'total');
            $response->getBody()->write(json_encode($json));
            return $response->withHeader('Content-Type', 'application/json');
        });

        $api->post("/$_-sections", function (Request $request, Response $response, array $args) use ($_) {
            $data = json_decode($request->getBody(), true);
            $validator = new Validator();
            $validation = $validator->validate((array)$data, [
                'name' => 'required|max:150',
            ]);
            if ($validation->fails()) {
                $errors = json_encode($validation->errors()->toArray());
                $response->getBody()->write($errors);
                return $response
                    ->withHeader('Content-Type', 'application/json')
                    ->withStatus(422);
            }

            $data['date_created'] = $data['date_updated'] = date(DATE_MYSQL);
            $created = database()->insert($_ . '_sections', $data);
            return $response->withStatus($created ? 200 : 500);
        });

        $api->get("/$_-sections/{id}", function (Request $request, Response $response, array $args) use ($_) {
            $db = database();
            $section = $db->get($_ . '_sections', '*', ['id' => $args['id']]);
            if (empty($section)) {
                return $response->withStatus(404);
            }

            $section[$_ . 's'] = $db->count($_ . 's', ['section_id' => $args['id']]);
            $response->getBody()->write(json_encode($section));
            return $response->withHeader('Content-Type', 'application/json');
        });

        $api->put("/$_-sections/{id}", function (Request $request, Response $response, array $args) use ($_) {
            $data = json_decode($request->getBody(), true);
            $validator = new Validator();
            $validation = $validator->validate((array)$data, [
                'name' => 'required|max:150',
            ]);
            if ($validation->fails()) {
                $errors = json_encode($validation->errors()->toArray());
                $response->getBody()->write($errors);
                return $response
                    ->withHeader('Content-Type', 'application/json')
                    ->withStatus(422);
            }

            $db = database();
            $section = $db->get($_ . '_sections', '*', ['id' => $args['id']]);
            if (empty($section)) {
                return $response->withStatus(404);
            }

            $updates = [
                'name' => $data['name'],
                'date_updated' => date(DATE_MYSQL),
            ];
            $updated = $db->update($_ . '_sections', $updates, ['id' => $args['id']]);
            return $response->withStatus($updated ? 200 : 500);
        });

        $api->delete("/$_-sections/{id}", function (Request $request, Response $response, array $args) use ($_) {
            $deleted = database()->delete($_ . '_sections', ['id' => $args['id']]);
            return $response->withStatus($deleted ? 200 : 500);
        });
    }

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

    $api->post('/articles', function (Request $request, Response $response) {
        $data = json_decode($request->getBody(), true);
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'section_id' => 'required|integer',
            'title' => 'required|max:150',
            'snippet' => 'required|max:300',
            'image' => 'required|max:150|url:http,https',
            'url' => 'required|max:150|url:http,https',
            'publisher' => 'max:150',
            'date_reported' => 'required|date:Y-m-d H:i:s',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $data['date_created'] = $data['date_updated'] = date(DATE_MYSQL);
        $created = database()->insert('articles', $data);
        return $response->withStatus($created ? 200 : 500);
    });

    $api->get('/articles/{id}', function (Request $request, Response $response, array $args) {
        $db = database();
        $article = $db->get(
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
            ['articles.id' => $args['id']]
        );
        if (empty($article)) {
            return $response->withStatus(404);
        }

        $response->getBody()->write(json_encode($article));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->put('/articles/{id}', function (Request $request, Response $response, array $args) {
        $data = json_decode($request->getBody(), true);
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'section_id' => 'required|integer',
            'title' => 'required|max:150',
            'snippet' => 'required|max:300',
            'image' => 'required|max:150|url:http,https',
            'url' => 'required|max:150|url:http,https',
            'publisher' => 'max:150',
            'date_reported' => 'required|date:Y-m-d H:i:s',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $db = database();
        $article = $db->get('articles', '*', ['id' => $args['id']]);
        if (empty($article)) {
            return $response->withStatus(404);
        }

        $updates = $data;
        $updates['date_updated'] = date(DATE_MYSQL);
        $updated = $db->update('articles', $updates, ['id' => $article['id']]);
        return $response->withStatus($updated ? 200 : 500);
    });

    $api->delete('/articles/{id}', function (Request $request, Response $response, array $args) {
        $deleted = database()->delete('articles', ['id' => $args['id']]);
        return $response->withStatus($deleted ? 200 : 500);
    });

    $api->get('/article-sections', function (Request $request, Response $response) {
        $query = $request->getQueryParams();
        $page = intval($query['page'] ?? 0);
        if ($page <= 0) {
            $page = 1;
        }

        $count = intval($query['count'] ?? 10);
        $offset = ($page - 1) * $count;
        $q = $query['q'] ?? '';
        if ($q) {
            $where = [
                'OR' => [
                    'name[~]' => $q,
                ],
            ];
        }

        $where['ORDER'] = [
            'name' => 'ASC',
        ];
        $where['LIMIT'] = [$offset, $count];
        $db = database();
        $data = $db->select('article_sections', ['id', 'name', 'date_created'], $where);
        if (empty($data)) {
            $data = [];
        }

        unset($where['LIMIT']);
        $total = $db->count('article_sections', $where);
        $pages = ceil($total / $count);
        foreach ($data as &$row) {
            $row['articles'] = $db->count('articles', ['section_id' => $row['id']]);
        }

        $json = compact('data', 'page', 'pages', 'total');
        $response->getBody()->write(json_encode($json));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->post('/article-sections', function (Request $request, Response $response, array $args) {
        $data = json_decode($request->getBody(), true);
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'name' => 'required|max:150',
            'google_news_section' => 'max:200',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $data['date_created'] = $data['date_updated'] = date(DATE_MYSQL);
        $created = database()->insert('article_sections', $data);
        return $response->withStatus($created ? 200 : 500);
    });

    $api->get('/article-sections/{id}', function (Request $request, Response $response, array $args) {
        $db = database();
        $section = $db->get('article_sections', '*', ['id' => $args['id']]);
        if (empty($section)) {
            return $response->withStatus(404);
        }

        $section['articles'] = $db->count('articles', ['section_id' => $args['id']]);
        $response->getBody()->write(json_encode($section));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->put('/article-sections/{id}', function (Request $request, Response $response, array $args) {
        $data = json_decode($request->getBody(), true);
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'name' => 'required|max:150',
            'google_news_section' => 'max:200',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $db = database();
        $section = $db->get('article_sections', '*', ['id' => $args['id']]);
        if (empty($section)) {
            return $response->withStatus(404);
        }

        $updates = [
            'name' => $data['name'],
            'google_news_section' => $data['google_news_section'],
            'date_updated' => date(DATE_MYSQL),
        ];
        $updated = $db->update('article_sections', $updates, ['id' => $args['id']]);
        return $response->withStatus($updated ? 200 : 500);
    });

    $api->delete('/article-sections/{id}', function (Request $request, Response $response, array $args) {
        $deleted = database()->delete('article_sections', ['id' => $args['id']]);
        return $response->withStatus($deleted ? 200 : 500);
    });

    $api->get('/comments', function (Request $request, Response $response) {
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
                    'text[~]' => $q,
                ],
            ];
        }

        $where['ORDER'] = [
            'comments.date_created' => 'DESC',
        ];
        $where['LIMIT'] = [$offset, 10];
        $db = database();
        $data = $db->select(
            'comments',
            [
                '[>]users' => ['user_id' => 'id'],
                '[>]videos' => ['video_id' => 'id'],
            ],
            [
                'comments.id',
                'comments.user_id',
                'username (user_username)',
                'name (user_name)',
                'video_id',
                'video (video_video)',
                'screenshot (video_screenshot)',
                'text',
                'comments.date_created',
            ],
            $where
        );
        if (empty($data)) {
            $data = [];
        }

        unset($where['LIMIT']);
        $total = $db->count('comments', $where);
        $pages = ceil($total / 10);
        foreach ($data as &$row) {
            $row['video_screenshot'] = file_url(DIR_SCREENSHOTS, $row['video_screenshot']);
            $row['video_video'] = file_url(DIR_VIDEOS, $row['video_video']);
        }

        $json = compact('data', 'page', 'pages', 'total');
        $response->getBody()->write(json_encode($json));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->get('/comments/{id}', function (Request $request, Response $response, array $args) {
        $comment = database()->get(
            'comments',
            [
                '[>]users' => ['user_id' => 'id'],
                '[>]videos' => ['video_id' => 'id'],
            ],
            [
                'comments.id',
                'comments.user_id',
                'username (user_username)',
                'name (user_name)',
                'video_id',
                'video (video_video)',
                'screenshot (video_screenshot)',
                'text',
                'comments.date_created',
            ],
            ['comments.id' => $args['id']]
        );
        if (empty($comment)) {
            return $response->withStatus(404);
        }

        $comment['video_screenshot'] = file_url(DIR_SCREENSHOTS, $comment['video_screenshot']);
        $comment['video_video'] = file_url(DIR_VIDEOS, $comment['video_video']);
        $response->getBody()->write(json_encode($comment));
        return $response->withHeader('Content-Type', 'application/json');
    });

    $api->delete('/comments/{id}', function (Request $request, Response $response, array $args) {
        $deleted = database()->delete('comments', ['id' => $args['id']]);
        return $response->withStatus($deleted ? 200 : 500);
    });

    $api->post('/notifications', function (Request $request, Response $response, array $args) {
        $data = json_decode($request->getBody(), true);
        $validator = new Validator();
        $validation = $validator->validate((array)$data, [
            'title' => 'required|max:100',
            'body' => 'required|max:1024',
        ]);
        if ($validation->fails()) {
            $errors = json_encode($validation->errors()->toArray());
            $response->getBody()->write($errors);
            return $response
                ->withHeader('Content-Type', 'application/json')
                ->withStatus(422);
        }

        $notification = Notification::create($data['title'], $data['body']);
        $message = CloudMessage::withTarget('topic', NOTIFICATION_TOPIC)
            ->withNotification($notification);
        $messaging = firebase()->createMessaging();
        $messaging->send($message);
        return $response;
    });
});

$secured->add(function (Request $request, Handler $handler) {
    $user = $request->getAttribute('user');
    if (empty($user) || $user['role'] !== 'admin') {
        return (new ResponseFactory())->createResponse(401);
    }

    return $handler->handle($request);
});
