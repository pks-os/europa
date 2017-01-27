/*
  @author Sam Heutmaker [samheutmaker@gmail.com]
*/

import React, { Component, PropTypes } from 'react'
import {Link} from 'react-router'

export default class Footer extends Component {
	constructor(props) {
		super(props);

		this.state = {};
	}
	renderLogos(){

		let logos = [
			{
				src: '/public/images/vmdashboard-logo.svg'
			},
			{
				src: '/public/images/k8sdashboard-logo.svg'
			},
			{
				src: '/public/images/europa-logo.svg'
			},
			{
				src: '/public/images/passly-logo.svg'
			}
		];

		return (
			<div className="LogoContainer">
				{logos.map((logo, i) => <img key={i} src={logo.src} /> )}
			</div>
		);
	}
	render(){
		return (
			<div className="Footer">
				<h2>We simplify and expedite devlopment operations.</h2>
				{this.renderLogos()}
			</div>
		);
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



