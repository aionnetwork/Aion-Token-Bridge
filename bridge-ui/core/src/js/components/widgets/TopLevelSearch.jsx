/* eslint-disable */
import React, {Component} from 'react';
import {hashHistory} from 'react-router';
import {Button, InputGroup, Intent} from "@blueprintjs/core";
import {nc_isStrEmpty, nc_trim} from 'lib/NCUtility';

export default class TopLevelSearch extends Component
{
  constructor(props) {
    super(props);

    this.state = {
      queryStr: ''
    }
  }

  submitQuery = () => {
    if (!nc_isStrEmpty(this.state.queryStr))
    {
      let prevQueryStr = nc_trim(this.state.queryStr);

      this.setState({
        queryStr: ''
      }, () => {
        console.log("query for eth tx hash: "+prevQueryStr);
        hashHistory.push('/track/'+prevQueryStr);
      });
    }
  };

  setQueryStr = (str) => {
    this.setState({
      queryStr: str
    });
  };

  render() {
    return (
      <div className="TopLevelSearch">
        <InputGroup
          className="search-bar"
          placeholder="Track bridge transfer status via Ethereum Transaction Hash"
          value={this.state.queryStr}
          onChange={(e) => this.setQueryStr(e.target.value)}
          onKeyPress={(e) => { if(e.key === 'Enter'){ this.submitQuery() }}}
          leftIcon="search"
          rightElement={
            <Button
              className=""
              minimal={true}
              intent={Intent.PRIMARY}
              rightIcon={"arrow-right"}
              text={"Track"}
              onClick={this.submitQuery}/>
          }/>

      </div>
    );
  };
}
















































