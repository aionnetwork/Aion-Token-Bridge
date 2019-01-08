/* eslint-disable */
import React from 'react';
import ReactDOM from 'react-dom';
import { FocusStyleManager } from "@blueprintjs/core";
import { Router, Route, IndexRedirect, hashHistory } from 'react-router'

import '../node_modules/normalize.css/normalize.css';
import '../node_modules/@blueprintjs/core/lib/css/blueprint.css';
import '../node_modules/@blueprintjs/icons/lib/css/blueprint-icons.css';
import '../node_modules/font-awesome/css/font-awesome.min.css';

import './css/app.css';

import Layout from "components/layout/NCLayout";
import Maintenance from 'components/views/NCMaintenance';

// disable (annoying) focus border accessibility feature (blueprint js)
FocusStyleManager.onlyShowFocusOnTabs();

ReactDOM.render((
	<Router onUpdate={() => window.scrollTo(0, 0)} history={ hashHistory }>
	  <Route path="/" component={ Layout }>
	    <IndexRedirect to={"transfer"}/>
	    <Route path="*" component={ Maintenance }/>
	  </Route>
	</Router>
  ), document.getElementById('root')
);
