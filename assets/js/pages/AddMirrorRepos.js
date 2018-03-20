import React, {Component, PropTypes} from "react"
import AddRegistry from "../components/AddRegistry";
import Btn from "../components/Btn";
import Loader from "../components/Loader";
import Msg from "../components/Msg";
import SelectReposToMirror from "../components/SelectReposToMirror";
import NPECheck from "../util/NPECheck";

export default class AddMirrorRepos extends Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  componentWillMount() {
    this.context.actions.listRegistries();
    this.context.actions.listRepos();
  }

  componentWillUnmount() {
    this.context.actions.resetAddMirrorsState();
  }

  createRepos() {
    if (this.props.addRepo.newRepoCredsType === 'NEW') {
      this.context.actions.addRegistryRequest()
        .then((credId) => this.context.actions.selectCredsForNewRepo(null, credId))
        .then(() => this.context.actions.addMirrors(this.toReposList.bind(this)))
        .catch(() => console.error('Add Registry Errors -- Skipping add mirrors'))
    } else {
      this.context.actions.addMirrors(this.toReposList.bind(this));
    }
  }
  toReposList() {
    this.context.router.push("/repositories");
  }

  keyPress(e) {
    if (e.keyCode === 13) {
      this.createRepos();
    }
  }

  renderSave() {
    if (this.props.addMirrors.addMirrorsXHR) {
      return <Loader/>;
    }
    if (this.props.addMirrors.addMirrorsError) {
      return (
        <Msg text={this.props.addMirrors.addMirrorsError}
             close={() => this.context.actions.clearAddMirrorsError()}/>
      );
    }
    if (this.context.actions.canAddMirrors()) {
      return (
        <div className="FlexRow JustifyCenter RowPadding">
          <Btn className="LargeBlueButton"
               onClick={this.createRepos.bind(this)}
               text="Mirror Repositories"
          />
        </div>
      );
    }
  }

  render() {
    return (
      <div className="ContentContainer">
        <div className="PageHeader">
          <h2>
            Mirror one or multiple repositories
          </h2>
        </div>
        <div>
          <AddRegistry
            {...this.props}
            standaloneMode={false}
            isEdit={false} />
          <SelectReposToMirror
            {...this.props}
            isLoading={NPECheck(this.props, 'addRepo/reposInRegistryXHR', false)}
            remoteRepos={NPECheck(this.props, 'addRepo/reposInRegistry', [])} />
          {this.renderSave()}
        </div>
      </div>
    )
  }
}

AddMirrorRepos.childContextTypes = {
  actions: PropTypes.object,
  router : PropTypes.object,
};

AddMirrorRepos.contextTypes = {
  actions: PropTypes.object,
  router : PropTypes.object,
};
