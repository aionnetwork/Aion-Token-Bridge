/* eslint-disable */
import React, {Component} from 'react';

import {NCEntity} from 'lib/NCEnums';
import {nc_GetEntityIcon, nc_trim} from 'lib/NCUtility';

export function parseClientTransaction(_entityId, _entitySCName, _entityUserName)
{
  let entityId = _entityId;
  let entityType = null;
  let entityName = null;

  if (_entitySCName != null && _entityUserName == null)
  {
    entityType = NCEntity.SC;
    entityName = _entitySCName;
  }
  else if (_entitySCName == null && _entityUserName != null)
  {
    entityType = NCEntity.USER;
    entityName = _entityUserName;
  }
  else
  {
    entityType = NCEntity.ACCOUNT;
  }

  return ({
    entityType: entityType,
    entityId: entityId,
    entityName: entityName,
  });
}

export default class NCEntityLabel extends Component
{
  constructor(props) 
  {
    super(props);

    // entity type does not change over lifecyle in this app
    this.iconName = nc_GetEntityIcon(this.props.entityType);

    // bind the functions
    this.linkToEntity = this.linkToEntity.bind(this);
  }

  linkToEntity(e, linkActive, link) 
  {
    e.preventDefault();

    if (!linkActive)
      return;    

    window.open(link)
  }

  render() {

    let { entityType, entityName, entityId, linkActive=true, className="", link } = this.props;

    let displayName = "Undefined";

    if (entityName != null && entityName != "") 
    {
      displayName = entityName;
    }
    else if (entityId != null && nc_trim(entityId) != "")
    {
      displayName = entityId;
    }

    return( 
        <span 
          className={"NCEntityLabel " + className + (linkActive ? " active" : "")}
          onClick={ (e) => {this.linkToEntity(e, linkActive, link)}}>
          <span className={"icon pt-icon-standard " + this.iconName}/>
          <span className="text pt-text-overflow-ellipsis ">{ displayName }</span>
        </span>
    );
  }
}

























