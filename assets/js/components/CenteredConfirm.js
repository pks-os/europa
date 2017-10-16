/*
  @author Sam Heutmaker [samheutmaker@gmail.com]
*/

import React, { Component } from 'react'
import PropTypes from 'prop-types'

export default  class CenteredConfirm extends Component {
  constructor(props) {
    super(props);
    this.state = {};
  }
  renderMsg() {
    if (this.props.message && !this.props.noMessage) {
      return (
        <div>
          <span style={this.props.messageStyle || {}}>
            {this.props.message}
          </span>
        </div>
      );
    }
  }
  render() {
    return (
      <div className="CenteredDelete" style={this.props.containerStyle || {}}>
        {this.renderMsg()}
        <div>
          <div className="ButtonBlue"
               style={this.props.confirmButtonStyle}
               onClick={this.props.onConfirm}>
            {this.props.confirmButtonText}
          </div>
          <div className="ButtonPink"
               onClick={this.props.onCancel}>
            Cancel
          </div>
        </div>
      </div>
    );
  }
}

CenteredConfirm.propTypes = {
  message: PropTypes.string,
  noMessage: PropTypes.bool,
  containerStyle: PropTypes.object,
  confirmButtonText:  PropTypes.string,
  confirmButtonStyle: PropTypes.object,
  onConfirm: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
};

CenteredConfirm.defaultProps = {
  noMessage: false,
  message: "Are you sure?",
  confirmButtonStyle: "Continue"
};
