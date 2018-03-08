import React, {Component, PropTypes} from 'react'
import CenteredConfirm from './../components/CenteredConfirm'
import Loader from './../components/Loader'
import NPECheck from './../util/NPECheck'
import Msg from './../components/Msg'
import CopyToClipboard from './../util/CopyToClipboard'
import RepoSelector from "../components/RepoSelector";

export default class CreateRepoMirror extends Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  componentWillUnmount() {
    this.context.actions.resetAddRepoState();
  }

  createRepoMirror() {
    this.context.actions.createRepoMirror()
    .then((res) => {
      this.context.router.push(`/repositories/${res.name}`);
    })
    .catch(() => {
    });
  }

  keyPress(e) {
    if (e.keyCode == 13) {
      this.createRepoMirror();
    }
  }

  repoFilter(repo) {
    return (repo.provider !== "EUROPA");
  }

  repoSelect(repo) {
    let sourceRepoId = NPECheck(repo, 'target/value/id', null);
    if (sourceRepoId === null) {
      return;
    }
    this.context.actions.updateNewMirrorSource(sourceRepoId);
  }

  renderRepoNameInput() {
    return (
      <div className="FlexColumn">
        <label style={{marginBottom: '5px'}}>Repository Name</label>
        <input className="BlueBorder FullWidth White"
               onChange={(e) => this.context.actions.updateNewRepoMirrorName(e)}
               placeholder="Enter repository name.."
               ref="name"/>
        {this.renderButton()}
        {this.renderError()}
      </div>
    );
  }

  renderButton() {
    if (NPECheck(this.props, 'addRepo/createMirrorXHR', false)) {
      return (
        <Loader/>
      );
    }

    return (
      <CenteredConfirm confirmButtonText="Create"
                       noMessage={true}
                       confirmButtonStyle={{}}
                       onConfirm={() => this.createRepoMirror()}
                       onCancel={() => this.context.actions.toggleCreateNewRepoMirror()}
                       containerStyle={{paddingBottom: '0px'}}/>
    );
  }

  renderError() {
    let error = NPECheck(this.props, 'addRepo/createMirrorError', false)
    if (error) {
      return (
        <Msg text={error}
             close={() => this.context.actions.clearCreateRepoMirrorErrors()}
             style={{marginTop: '1rem'}}/>
      );
    }
  }

  renderCommands() {
    return (
      <div className="FlexColumn NewRepoCommands">
        <div className="HelperText">or</div>
        <div className="HelperText">Push a Docker image to a local repository</div>
        <div className="HelperText FlexRow">
          <div className="Code White">
            <span>$&nbsp;<span
              id="copyCommands">docker push&nbsp;{`${this.props.dnsName}/${(this.props.isLoggedIn && this.props.isEnterprise) ? NPECheck(this.props, 'ctx/username', '') + '/' : ''}REPO_NAME[:IMAGE_TAG]`}</span></span>
            <i className="icon icon-dis-copy"
               onClick={() => CopyToClipboard(document.getElementById('copyCommands'))}
               data-tip="Click To Copy"
               data-for="ToolTipTop"/>
          </div>
        </div>
      </div>
    );
  }

  render() {
    return (
      <div className="CR_BodyContent" onKeyDown={(e) => this.keyPress(e)}>
        <RepoSelector onSelectFn={(repo) => this.context.actions.updateNewMirrorSource(repo)}
                      repoList={this.props.repos}
                      valueFn={() => NPECheck(this.props, 'addRepo/createMirrorSourceName', '')}
                      filterFn={this.repoFilter.bind(this)}/>
        {this.renderRepoNameInput()}
        {this.renderCommands()}
      </div>
    );
  }
}

CreateRepoMirror.childContextTypes = {
  actions: PropTypes.object,
  router: PropTypes.object
};

CreateRepoMirror.contextTypes = {
  actions: PropTypes.object,
  router: PropTypes.object
};
