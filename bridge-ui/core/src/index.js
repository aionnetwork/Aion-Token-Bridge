/* eslint-disable */

import React from 'react';
import ReactDOM from 'react-dom';
import { FocusStyleManager } from "@blueprintjs/core";
import { Router, Route, IndexRedirect, hashHistory } from 'react-router'
import { Provider } from 'react-redux'

import { store } from 'stores/NCReduxStore';

import '../node_modules/normalize.css/normalize.css';
import '../node_modules/@blueprintjs/core/lib/css/blueprint.css';
import '../node_modules/@blueprintjs/icons/lib/css/blueprint-icons.css';

import '../node_modules/font-awesome/css/font-awesome.min.css';

import './css/app.css';

import * as nc from 'lib/NCConstants';
import NoMatch from 'components/common/NCNoMatch';
import Layout from "components/layout/NCLayout";

import Transfer from 'components/views/NCTransfer';
import Track from 'components/views/NCTrack';

// disable (annoying) focus border accessibility feature (blueprint js)
FocusStyleManager.onlyShowFocusOnTabs();

ReactDOM.render((
  <Provider store={ store }>
    <Router onUpdate={() => window.scrollTo(0, 0)} history={ hashHistory }>
      <Route path="/" component={ Layout }>
        <IndexRedirect to={nc.PATH_TRANSFER}/>
        <Route path={nc.PATH_TRANSFER} component={ Transfer }/>
        <Route path={nc.PATH_TRACK +"/:ethTxHash"} component={ Track }/>
        <Route path="*" component={ NoMatch }/>
      </Route>
    </Router>
  </Provider>), document.getElementById('root')
);
