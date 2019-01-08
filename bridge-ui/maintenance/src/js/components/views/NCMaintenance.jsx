/* eslint-disable */
import React, {Component} from 'react';

export default class NCMaintenance extends Component
{
  render() {
    return (
      <div 
        className="NCMaintenance"
        style={{
          paddingTop: 70,
          paddingBottom: 40,
        }}>
        <div className="pt-non-ideal-state" style={{
          maxWidth: `none`
        }}>
          <div className="pt-non-ideal-state-visual pt-non-ideal-state-icon" style={{
            marginBottom: `40px`
          }}>
            <img src="/img/robot.svg" style={{
              height: 200
            }}/>
          </div>
          <h4 className="pt-non-ideal-state-title" style={{
            fontFamily: `'Roboto', sans-serif`,
            fontWeight: `400`,
            fontSize: `20px`
          }}>Bridge Under Maintenance</h4>
          <div className="pt-non-ideal-state-description" style={{
            fontWeight: `300`
          }}>
            <p>Bridge transfers are momentarily disabled due to planned maintenance.</p>
            <p><b>If you were tracking an active transfer, rest assured that your tokens will be bridged</b></p>
            <p>(as long as you can retrieve the bridge transfer transaction through Etherscan).</p>
          </div>
        </div>
      </div>
    );
  }
}
