/* eslint-disable */
import {combineReducers, createStore} from 'redux';
import {reducer_track} from 'stores/StoreTrack';

// store -----------------------------------------------------------------
let redux_extension = undefined;
if (!process.env.NODE_ENV || process.env.NODE_ENV === 'development') {
  // noinspection JSUnresolvedVariable
  redux_extension = window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__();
} 

export const store = createStore (
  combineReducers({
    track: reducer_track
  }), redux_extension
);
