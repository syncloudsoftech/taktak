# api

This folder holds source code for TakTak's REST APIs powered by [Slim](https://www.slimframework.com/), a popular
[PHP](https://www.php.net/) framework.

## Pre-requisites

1. Create a project (if not already) in [Firebase](https://console.firebase.google.com/) console, download the `google-services.json` file.
2. Create an app (if not already) in [Meta for Developers](https://developers.facebook.com/) console, make note of the **App ID** and **App Secret** when setting up your app.

## Development

Before getting started, [Docker](https://www.docker.com/), [PHP](https://www.php.net/) and
[composer](https://getcomposer.org/) must be installed on your workstation. Once you have everything, run below commands
in `api` sub-folder:

```shell
# start the services
$ docker-compose up -d

# install dependencies
$ composer install

# create sample config file
$ php -r "file_exists('constants.php') || copy('constants.sample.php', 'constants.php');"

# create database tables
$ php commands/migrate.php

# insert demo data (optional)
$ php commands/seed-demo.php

# fetch latest news (optional)
$ php commands/fetch-news.php

# start the web server
$ php -S 0.0.0.0:8000 -t public public/index_dev.php
```

For [Facebook](https://www.facebook.com/) or [Google](https://www.google.com/) login to work in app, you need to update `constants.php` with your own app credentials:

```php
<?php

// This is the base URL where you have hosted the api sub-project.
// Use http://10.0.2.2:8000/ if connecting from emulator to api running on host.
define('BASE_URL', 'http://127.0.0.1:8000');
```

For [Facebook](https://www.facebook.com/) or [Google](https://www.google.com/) login to work in app, you need to update `constants.php` with your own app credentials:

```php
<?php

// You will find these values when setting up Login with Facebook for your app in the Facebook developer console.
define('FACEBOOK_APP_ID', null);
define('FACEBOOK_APP_SECRET', null);

// This is usually the client ID (with "client_type": 3) from your google-services.json file.
define('GOOGLE_CLIENT_ID', null);
```

## Deployment

If running in production, you must update some more values in `constants.php` shown below:

```php
<?php

// This must be set to something really, really random
define('JWT_SECRET', 'something_really_secret');
```
