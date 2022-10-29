# admin

This folder holds source code for TakTak's [React.js](https://reactjs.org/) powered administration area.

## Development

Before getting started on building the admin area, you need to make sure you have [Node.js](https://nodejs.org/en/) and
[Yarn](https://yarnpkg.com/) installed on your machine. Once done, run below commands in `admin` sub-folder:

```shell
# install dependencies
$ yarn install

# start the web server
$ npm start
```

The admin panel should not be accessible at [http://localhost:3000](http://localhost:3000) page.
If you are using the default demo setup, you can access the admin area using `admin` as username and `12345` as password.

## Deployment

To deploy the admin panel, you need to build the static files first:

```shell
# set production API url
$ export REACT_APP_BASE_URL=https://api.example.com

# build for production
$ npm run build
```

The static files will be generated in the `build` folder.
You can then upload the contents of the `build` folder to your web server.
