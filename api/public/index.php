<?php

require __DIR__ . '/../vendor/autoload.php';

use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;
use Psr\Http\Server\RequestHandlerInterface as Handler;
use Slim\Factory\AppFactory;
use Slim\Interfaces\RouteCollectorProxyInterface as Routes;
use Slim\Middleware\MethodOverrideMiddleware as MethodOverride;
use Tuupola\Middleware\CorsMiddleware;
use Tuupola\Middleware\JwtAuthentication;

date_default_timezone_set(DATE_TIMEZONE);

$app = AppFactory::create();

$app->get('/', function (Request $request, Response $response) {
    $response->getBody()->write(json_encode(['api' => true]));
    return $response->withHeader('Content-Type', 'application/json');
});

$api = $app->group('/api', function (Routes $api) {

    require __DIR__ . '/../routes/api.php';

    $api->group('/admin', function (Routes $api) {

        require __DIR__ . '/../routes/admin.php';
    });
});

$api->add(function (Request $request, Handler $handler) {
    $token = $request->getAttribute('token');
    if ($token) {
        $user = database()->get('users', '*', ['id' => $token['id']]);
        if ($user) {
            return $handler->handle($request->withAttribute('user', $user));
        }
    }

    return $handler->handle($request);
});

$api->add(new JwtAuthentication([
    'rules' => [
        function (Request $request) {
            return $request->hasHeader('Authorization');
        },
    ],
    'secret' => JWT_SECRET,
    "relaxed" => ['localhost', '127.0.0.1', '10.0.2.2'],
]));

$app->addMiddleware(new MethodOverride());

$app->addErrorMiddleware(defined('DEBUG_ENABLED'), true, true);

$app->addMiddleware(new CorsMiddleware([
    'headers.allow' => ['Authorization', 'Content-Type'],
]));

$app->run();
