import React, {Component, PropTypes} from "react"

export default class MirrorRepoConfigItem extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isEditing: false,
      localRepoName: null,
    };
  }

  handleKeyPress(e) {
    if (e.key === 'Enter') {
      this.saveLocalRepoName();
    }
    // Escape
    if (e.key === 'Escape') {
      this.cancelLocalRepoName();
    }
  }

  toggleEditingLocalRepoElement() {
    this.setState((prevState, props) => {
      return {
        isEditing: !prevState.isEditing,
      };
    });
  }

  getLocalRepoName() {
    return (this.state.localRepoName === null) ? this.props.localRepoName : this.state.localRepoName;
  }

  getConflictingRepoNameClassNames() {
    let classNames = [];
    let remoteRepoName = this.props.remoteRepoName.split("/").pop();
    if (this.props.checkConflictFn(remoteRepoName)) {
      classNames.push("remote-conflicting");
    }
    if (this.props.checkConflictFn(this.getLocalRepoName())) {
      classNames.push("conflicting");
    } else {
      classNames.push("not-conflicting");
    }
    return classNames.join(" ");
  }

  updateLocalRepoNameEdit(e) {
    let value = e.target.value;
    this.setState({
      localRepoName: value,
    });
  }

  saveLocalRepoName() {
    let localRepoName = this.getLocalRepoName();
    this.setState({
      isEditing: false,
      localRepoName: null,
    }, () => this.props.localRepoNameSaveFn(localRepoName));
  }

  cancelLocalRepoName() {
    this.setState({
      isEditing: false,
      localRepoName: null,
    }, this.props.localRepoNameCancelFn);
  }

  renderEditingLocalRepoElement() {
    let inputClassName = `LocalRepoNameEdit ${this.getConflictingRepoNameClassNames()}`;
    return (
      <div className="LocalRepo LocalRepoElement SimpleTableCell">
        <div className="LocalRepoElementEdit">
          <div className="LocalRepoName">
            <input className={inputClassName}
                   onChange={this.updateLocalRepoNameEdit.bind(this)}
                   onKeyDown={this.handleKeyPress.bind(this)}
                   autoFocus={true}
                   onFocus={(e) => {
                     // Place the cursor at the end of the input
                     let val = e.target.value;
                     e.target.value = '';
                     e.target.value = val;
                   }}
                   value={this.getLocalRepoName()}/>
          </div>
          <div className="LocalRepoButtons">
            <span className="LocalRepoSaveButton Clickable"
                  onClick={this.saveLocalRepoName.bind(this)}>
              Save
            </span>
            <span className="LocalRepoCancelButton Clickable"
                  onClick={this.cancelLocalRepoName.bind(this)}>
              Cancel
            </span>
          </div>
        </div>
      </div>
    );
  }

  renderRemoteRepoName() {
    let repoNameElements = this.props.remoteRepoName.split("/");
    let lastElement = repoNameElements.pop();
    let repoName = (this.props.checkConflictFn(lastElement))
      ? <span className="RepoName conflicting">{lastElement}</span>
      : <span className="RepoName">{lastElement}</span>;
    let repoNamePrefix = <span className="RepoNamePrefix">{repoNameElements.map(e => `${e}/`).join("")}</span>;
    let iconClasses = (this.props.isSelected) ? 'icon icon-dis-box-check Clickable' : 'icon icon-dis-box-uncheck Clickable';
    return (
      <div className="RemoteRepo RemoteRepoName SimpleTableCell">
        <i className={iconClasses}
           onClick={this.props.toggleSelectedFn}/>
        <span className="RemoteRepoName">{repoNamePrefix}{repoName}</span>
      </div>
    )
  }

  formatLocalRepoName(repoName) {
    let className = `LocalRepoName ${this.getConflictingRepoNameClassNames()}`;
    return (
      <span className={className}>{repoName}</span>
    );
  }

  renderLocalRepoButtons() {
    if (!this.props.isSelected) {
      return;
    }
    let deleteButton = (this.props.checkConflictFn(this.props.localRepoName))
      ? (
        <div className="LocalRepoDeleteButton Clickable"
             onClick={this.props.localRepoDeleteFn}>
          <i className="icon icon-dis-trash"/>
        </div>
      )
      : null;
    return (
      <div className="LocalRepoButtons">
        <div className="LocalRepoEditButton Clickable"
             onClick={this.toggleEditingLocalRepoElement.bind(this)}>
          <i className="icon icon-dis-edit"/>
        </div>
        {deleteButton}
      </div>
    )
  }

  renderLocalRepoElement() {
    if (this.state.isEditing) {
      return this.renderEditingLocalRepoElement();
    }
    return (
      <div className="LocalRepo LocalRepoElement SimpleTableCell">
        <div className="LocalRepoElementView">
          <div className="LocalRepoName"
               onClick={this.toggleEditingLocalRepoElement.bind(this)}>
            {this.formatLocalRepoName(this.getLocalRepoName())}
          </div>
          {this.renderLocalRepoButtons()}
        </div>
      </div>
    );
  }

  render() {
    let selectedClass = (this.props.isSelected) ? "MirrorRepoSelected" : "MirrorRepoNotSelected";
    return (
      <div className={`MirrorRepoItem MirrorRepoRow SimpleTableRow ${selectedClass}`}>
        {this.renderRemoteRepoName()}
        {this.renderLocalRepoElement()}
      </div>
    )
  }
}

MirrorRepoConfigItem.propTypes = {
  remoteRepoName: PropTypes.string.isRequired,
  isSelected: PropTypes.bool.isRequired,
  toggleSelectedFn: PropTypes.func.isRequired,
  checkConflictFn: PropTypes.func.isRequired,
  localRepoName: PropTypes.string,
  localRepoNameSaveFn: PropTypes.func.isRequired,
  localRepoDeleteFn: PropTypes.func,
};

MirrorRepoConfigItem.defaultProps = {
  remoteRepoNameRenderFn: (repoName) => repoName,
};

MirrorRepoConfigItem.childContextTypes = {
  actions: PropTypes.object,
  router: PropTypes.object
};

MirrorRepoConfigItem.contextTypes = {
  actions: PropTypes.object,
  router: PropTypes.object
};
