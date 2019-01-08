/* eslint-disable */
import React, {Component} from 'react';
import {StageState} from 'components/common/Types';
import {Intent, Spinner} from "@blueprintjs/core";
import {nc_isPositiveInteger} from "lib/NCUtility";

export default class StageIcon extends Component {
  render () {
    const { status, count=null } = this.props;

    switch(status) 
    {
      case StageState.SUCCESS: {
        return (
          <span className="statemap-icon">
            <i className="fa fa-check-circle smi st-success"/>
          </span>);
      }
      case StageState.DANGER: {
        return (
          <span className="statemap-icon">
            <i className="fa fa-times-circle smi st-danger"/>
          </span>);
      }
      case StageState.WARNING: {
        return (
          <span className="statemap-icon">
            <i className="fa fa-exclamation-circle smi st-warning"/>
          </span>);
      }
      case StageState.LOADING: {
        // noinspection JSUnresolvedFunction
        return (
          <span className="statemap-icon">
            <Spinner intent={Intent.PRIMARY} className="smi-loading"/>
            {
              (count != null && nc_isPositiveInteger(count) && count <= 999) ?
                <span className="smi-group">
                  <span className="smi-count">{count.toString(10)}</span>
                  <i className="fa fa-circle-thin smi st-primary"/>
                </span> :
                <i className="fa fa-clock-o smi st-primary"/>
            }
          </span>);
      }
      case StageState.COUNT: {
        return (
          <span className="statemap-icon">
          {
            (count != null && nc_isPositiveInteger(count) && count <= 999) ?
              <span className="smi-group">
                <span className="smi-count">{count.toString(10)}</span>
                <i className="fa fa-circle-thin smi st-primary"/>
              </span> :
              <i className="fa fa-clock-o smi st-primary"/>
          }
          </span>);
      }
      default: {
        return (
          <span className="statemap-icon">
            <i className="fa fa-clock-o smi st-pending"/>
          </span>);
      }
    }
  }
}