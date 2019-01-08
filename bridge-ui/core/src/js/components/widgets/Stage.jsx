/* eslint-disable */
import React, {Component} from 'react';
import StageIcon from 'components/widgets/StageIcon';
import {Position, Tooltip} from "@blueprintjs/core";

export default class Stage extends Component {
  render () {
    const { status, count=null, isLast=false, title=[], tooltip, linkable=null } = this.props;

    return (
      <div className={"stage-container " + (linkable != null ? "linkable" : "")} onClick={()=>{ }}>
        <div className="stage">
          <Tooltip
            content={tooltip}
            inline={false}
            className="statemap-icon-container"
            portalClassName={"stage-tooltip"}
            position={Position.BOTTOM}
            hoverOpenDelay={50}
            modifiers={{
              offset: {
                enabled: true,
                offset: '0, 40'
              }, 
              preventOverflow: { enabled: true, boundariesElement: "window" }
            }}>
            <StageIcon status={status} count={count}/>
          </Tooltip>
          <span className="title-container">
          {
            title.map((v, i) => {
              return(<span className={"title "+((i==title.length-1) ? "last" : "")} key={i}>{v}</span>);
            })
          }
          </span>
        </div>
        {
          !isLast &&
          <div className="stage-divider">
            {/*<i className="fa fa-chevron-right sdi"></i>*/}
            <span className="pt-icon-large pt-icon-chevron-right sdi"/>
          </div>
        }
      </div>
    );
  }
}