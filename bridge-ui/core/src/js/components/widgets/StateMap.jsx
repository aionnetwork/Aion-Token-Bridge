/* eslint-disable */
import React, {Component} from 'react';
import Stage from 'components/widgets/Stage';

export default class StateMap extends Component {
  render () {
    const { statemap } = this.props;

    return (
      <div className="StateMap">
      { 
        statemap.map((v, i) => {
          return(
            <Stage
              key={i} 
              status={v.status}
              count={v.count} 
              isLast={v.isLast}
              title={v.title}
              subtitle={v.subtitle}
              tooltip={v.tooltip}
              linkable={v.linkable}/>
          )
        })
      }
      </div>
    );
  }
}