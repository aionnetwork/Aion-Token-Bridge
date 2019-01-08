/* eslint-disable */
import React, {Component} from 'react';

export default class NCLayout extends Component {

  constructor(props) {
    super(props);

    this.buildHash = null;
    try {
      const file = String(document.currentScript.src);
      const fileName = file.substring(file.lastIndexOf("/") + 1);
      const parts = fileName.split(".");
      if (Array.isArray(parts) && parts[0] != null && parts[1] != null) {
        if (parts[0] == "main") {
          this.buildHash = parts[1];
        }
      }
    } catch (e) {
      console.log("Build hash retrieve failed.");
      console.log(e);
    }
  }

  render() {
    let version = "v1.0.4";
    if (this.buildHash != null) {
      version += "."+this.buildHash;
    }

    return (
      <div className="NCPage">
        <div className="NCHeader pt-navbar">
          <div className="row">
            <div className="pt-navbar-group navbar-group-left">
              <span className="logo">
                <img className="logo-img" src="/img/aion-icon.svg" alt="logo"/>
                <span className="title">
                  <span>Token Bridge</span>
                </span>
              </span>
            </div>
          </div>
        </div>

        <div className="NCPageContent">
          <div className="container">
            { this.props.children }
          </div>
        </div>
        <div className="NCFooter">
          <div className="footer-container">
            <div className="link-container">
              <a className="link" href="https://aion.network" target="_blank">
                <span className="text">Powered By</span>
                <img className="logo" src="/img/aion-icon.svg" alt="logo"/>
              </a>
            </div>
            <div className="version">
              { version }
            </div>
          </div>
        </div>
      </div>
    );
  }
}

































