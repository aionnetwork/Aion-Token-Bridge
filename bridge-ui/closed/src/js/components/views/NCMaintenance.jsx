/* eslint-disable */
import React, {Component} from 'react';
import {Button, Intent} from "@blueprintjs/core";

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
            <img src="/img/vulcan-hello.svg" style={{
              height: 140
            }}/>
          </div>
          <h4 className="pt-non-ideal-state-title" style={{
              fontFamily: `'Roboto', sans-serif`,
              fontWeight: `500`,
              fontSize: `24px`,
              marginBottom: `45px`
          }}>Aion Token Swap Completed!</h4>
          <div className="pt-non-ideal-state-description" style={{
            fontWeight: `400`
          }}>
            <p>As of November 30th, 23:59:59 UTC, the Swap-period officially concluded.</p>
            <p><b>We saw overwhelming adoption with over 95M AION crossing the Token Transfer bridge!</b></p>
            <p style={{
                maxWidth: `730px`
            }}>By putting our technology to the test and bringing together an engaged ecosystem of global partners and community members, we have completed the swap period with over 94% of ERC-20 tokens converted. This amazing participation in such a condensed period of time marks the Aion Token Swap as the most engaged token swap to date.</p>
          </div>
            <Button
                intent={Intent.PRIMARY}
                icon="help"
                minimal={false}
                onClick={() => {
                    window.open("https://blog.aion.network", "_blank");
                }}
                text="I still hodl Aion ERC-20 ..."
                style={{boxShadow: `none`}}/>
          <div>

          </div>
        </div>
      </div>
    );
  }
}
