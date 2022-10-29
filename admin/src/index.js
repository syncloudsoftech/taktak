import './index.scss';
import React from 'react';
import ReactDOM from 'react-dom';
import { TakTak } from './components/main';
import * as serviceWorker from './serviceWorker';

ReactDOM.render(
    <React.StrictMode>
        <TakTak />
    </React.StrictMode>,
    document.getElementById('root')
);

serviceWorker.unregister();
