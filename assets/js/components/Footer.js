/*
  @author Sam Heutmaker [samheutmaker@gmail.com]
*/

import React, {Component, PropTypes} from 'react'
import {Link} from 'react-router'
import NPECheck from './../util/NPECheck'
import capitalize from './../util/Capitalize'

export default class Footer extends Component {
  constructor(props) {
    super(props);

    this.state = {};
  }

  renderLogos() {
    let logos = [
      {
        src: '/public/images/puppet/pipelines-applications-bk.svg',
        href: 'https://www.distelli.com/applications'
      },
      {
        src: '/public/images/puppet/pipelines-containers-bk.svg',
        href: 'https://www.distelli.com/containers'
      },
      {
        src: '/public/images/puppet/container-registry-bk.svg',
        href: 'https://www.distelli.com/registry'
      }
    ];

    return (
      <div className="LogoContainer">
        {logos.map((logo, i) => {
          return (
            <a href={logo.href} key={i} target="_blank">
              <img src={logo.src}/>
            </a>
          );
        })}
      </div>
    );
  }

  logoFooter() {
    return (
      <div className="LogoFooter">
        <div className="Spacer"/>
        <h2>Automation for software teams</h2>
        {this.renderLogos()}
      </div>
    );
  }

  footer() {
    let europa = capitalize(PAGE_PROPS.europa);

    return (
      <div className="Footer">
        <div className="FooterInside">
          <div className="Flex1"></div>
          <div className="Flex1"></div>
          <div className="Version"><span>Puppet&copy; Container Registry&nbsp;</span> {europa} -
            Version {PAGE_PROPS.version || 'Unknown'}</div>
        </div>
      </div>
    );
  }

  render() {
    if (window.location.pathname == '/' || typeof this.props.isLoggedIn != 'undefined' && !this.props.isLoggedIn) {
      return this.logoFooter();
    } else {
      return this.footer();
    }
  }
}

Footer.contextTypes = {
  router: PropTypes.object,
  actions: PropTypes.object
};

Footer.childContextTypes = {
  actions: PropTypes.object,
  router: PropTypes.object
};




