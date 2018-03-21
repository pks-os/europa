import React from 'react'
import ContentRow from "../components/ContentRow";
import Loader from "../components/Loader";
import MirrorRepoConfigItem from "../components/MirrorRepoConfigItem";
import Msg from "../components/Msg";
import Selector from "../components/Selector";

export default class SelectReposToMirror extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      selectorOpen: false,
      searchFilter: '',
      showReadyToMirror: true,
      showIgnored: true,
      showConflicts: true,
    };
  }

  getSelectorOptions(choice = null) {
    return this.context.actions.getAddMirrorsSelectorOptions(choice);
  }

  getRepos(props = this.props) {
    return props.remoteRepos;
  }

  toggleSelected(repoName) {
    this.context.actions.toggleAddMirrorsRepoStateField(repoName, "selected", true);
  }

  toggleSelectorOpen(callback = null) {
    this.setState((prevState, props) => {
      return {
        selectorOpen: !prevState.selectorOpen,
      };
    }, callback);
  }

  activateSelector(option) {
    this.toggleSelectorOpen(() => this.context.actions.activateAddMirrorsSelector(option));
  }

  getSelectorStatus() {
    return this.context.actions.getAddMirrorsSelectorStatus();
  }

  checkRepoNameConflict(repoName) {
    return this.props.reposNameMap.hasOwnProperty(repoName);
  }

  updateLocalRepoName(remoteRepoName, localRepoName) {
    this.context.actions.updateAddMirrorsRepoStateField(remoteRepoName, "localRepoName", localRepoName);
  }

  updateSearchFilter(filterString) {
    this.setState({
      searchFilter: filterString.target.value,
    });
  }

  renderRepoItem(repoName, key) {
    let isSelected = this.context.actions.getAddMirrorsIsRepoSelected(repoName);
    let localRepoName = this.context.actions.getAddMirrorsLocalRepoName(repoName);

    return (
      <MirrorRepoConfigItem
        {...this.props}
        key={key}
        remoteRepoName={repoName}
        isSelected={isSelected}
        toggleSelectedFn={() => this.toggleSelected(repoName)}
        checkConflictFn={this.checkRepoNameConflict.bind(this)}
        localRepoName={localRepoName}
        localRepoNameSaveFn={(newLocalRepoName) => this.updateLocalRepoName(repoName, newLocalRepoName)}
      />
    )
  }

  repoMatchesFilter(repo) {
    let elements = repo.split("/");
    let localRepoName = this.context.actions.getAddMirrorsLocalRepoName(repo);
    let isIgnored = !this.context.actions.getAddMirrorsIsRepoSelected(repo);
    let isConflicting = !isIgnored && this.checkRepoNameConflict(localRepoName);
    let isReadyToMirror = (!isIgnored && !isConflicting);

    let matchesElement = (elements.findIndex(e => e.startsWith(this.state.searchFilter)) !== -1);
    let matchesLocalRepoName = localRepoName.startsWith(this.state.searchFilter);

    let matchesReadyToMirror = (!isReadyToMirror || this.state.showReadyToMirror);
    let matchesIgnored = (!isIgnored || this.state.showIgnored);
    let matchesConflicting = (!isConflicting || this.state.showConflicts);

    let matchesSearch = (matchesElement || matchesLocalRepoName);
    let matchesShowFilter = (matchesReadyToMirror && matchesIgnored && matchesConflicting);

    return matchesSearch && matchesShowFilter;
  }

  renderRepoList() {
    let repos = this.getRepos().filter(this.repoMatchesFilter.bind(this));
    return (
      <div className="MirrorRepoList SimpleTable">
        {repos.map(this.renderRepoItem.bind(this))}
      </div>
    )
  }

  renderRepoListHeaderOperations() {
    return (
      <div className="LocalRepoHeaderOperations">
        {this.renderRepoListHeaderSelector()}
        {this.renderRepoListHeaderFilter()}
      </div>
    );
  }

  renderRepoListHeaderSelector() {
    return (
      <div className="RepoListHeaderSelector">
        <Selector isOpen={this.state.selectorOpen}
                  toggleOpen={() => this.toggleSelectorOpen()}
                  listItems={this.getSelectorOptions()}
                  onClick={this.activateSelector.bind(this)}
                  currentValue={this.getSelectorStatus()}
                  renderItem={(item) => item.name}
        />
      </div>
    )
  }

  toggleReadyToMirrorFilter() {
    this.setState((prevState, props) => {
      return {
        showReadyToMirror: !prevState.showReadyToMirror,
      };
    });
  }

  toggleIgnoredFilter() {
    this.setState((prevState, props) => {
      return {
        showIgnored: !prevState.showIgnored,
      };
    });
  }

  toggleConflictsFilter() {
    this.setState((prevState, props) => {
      return {
        showConflicts: !prevState.showConflicts,
      };
    });
  }

  renderRepoListHeaderFilter() {
    let selectedRepos = this.context.actions.getAddMirrorsSelectedRepoData();
    let conflictingRepos = selectedRepos.filter(repoData => this.props.reposNameMap.hasOwnProperty(repoData.destinationRepoName));

    let conflictsCount = conflictingRepos.length;
    let readyToMirrorCount = selectedRepos.length - conflictsCount;
    let ignoredCount = this.props.remoteRepos.length - selectedRepos.length;
    let conflictsLabel = (conflictsCount === 1) ? "Conflict" : "Conflicts";

    let readyToMirrorIconClassName = (this.state.showReadyToMirror) ? 'icon icon-dis-box-check' : 'icon icon-dis-box-uncheck';
    let ignoredIconClassName = (this.state.showIgnored) ? 'icon icon-dis-box-check' : 'icon icon-dis-box-uncheck';
    let conflictsIconClassName = (this.state.showConflicts) ? 'icon icon-dis-box-check' : 'icon icon-dis-box-uncheck';
    let conflictsWrapperClassName = (conflictsCount === 0)
      ? 'RepoListHeaderConflictsFilter cursor-on-hover no-conflicts'
      : 'RepoListHeaderConflictsFilter cursor-on-hover conflicts';

    return (
      <div className="RepoListHeaderFilterContainer">
        <div className="RepoListHeaderFilterLabel">
          <label>Show:</label>
        </div>
        <div className="RepoListHeaderFilters">
          <div className="RepoListHeaderReadyToMirrorFilter cursor-on-hover"
               onClick={this.toggleReadyToMirrorFilter.bind(this)}>
            <i className={readyToMirrorIconClassName}/>
            <label className="cursor-on-hover">{readyToMirrorCount} Ready to mirror</label>
          </div>
          <div className="RepoListHeaderIgnoredFilter cursor-on-hover"
               onClick={this.toggleIgnoredFilter.bind(this)}>
            <i className={ignoredIconClassName}/>
            <label className="cursor-on-hover">{ignoredCount} Ignored</label>
          </div>
          <div className={conflictsWrapperClassName}
               onClick={this.toggleConflictsFilter.bind(this)}>
            <i className={conflictsIconClassName}/>
            <label className="cursor-on-hover">{conflictsCount} {conflictsLabel}</label>
          </div>
        </div>
      </div>
    )
  }

  renderRepoListHeader() {
    return (
      <div className="MirrorRepoHeader MirrorRepoRow SimpleTableHeaderRow">
        <div className="RemoteRepo RemoteRepoHeader SimpleTableHeader">
          Remote repository
        </div>
        <div className="LocalRepo LocalRepoHeader SimpleTableHeader">
          <div className="LocalRepoHeaderText">
            Local repository
          </div>
          {this.renderRepoListHeaderOperations()}
        </div>
      </div>
    );
  }

  renderSearchRepos() {
    return (
      <div className="MirrorRepoConfigSearch">
        <input className="BlueBorder Search Tiny"
               onChange={this.updateSearchFilter.bind(this)}
               placeholder="Search repositories…"
               value={this.state.searchFilter}
        />
      </div>
    );
  }

  renderConfig() {
    if (this.props.isLoading) {
      return (
        <Loader/>
      );
    }
    if (this.getRepos().length === 0) {
      return (
        <div className="MirrorRepoConfig">
          <div className="MirrorRepoConfigEmpty">
            <Msg text="Select existing registry credentials or add new credentials to mirror repositories" />
          </div>
        </div>
      )
    }
    return (
      <div className="MirrorRepoConfig">
        {this.renderSearchRepos()}
        <div className="SimpleTable">
          {this.renderRepoListHeader()}
          {this.renderRepoList()}
        </div>
      </div>
    );
  }

  renderBody() {
    return (
      <div className="SelectReposToMirror">
        <div className="FlexColumn">
          <label>Select Repositories – <span className="hint">When mirroring repositories, make sure to <strong>rename</strong> or <strong>remove</strong> any conflicting repositories.</span></label>
          {this.renderConfig()}
        </div>
      </div>
    );
  }

  render() {
    return (
      <div className="ContentContainer">
        <ContentRow
          row={{
            columns: [{
              icon: 'icon icon-dis-repo',
              renderBody: this.renderBody.bind(this),
            }]
          }}
        />
      </div>
    );
  }
}

SelectReposToMirror.propTypes = {
  isLoading: React.PropTypes.bool.isRequired,
  remoteRepos: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
};

SelectReposToMirror.childContextTypes = {
  actions: React.PropTypes.object,
  router: React.PropTypes.object,
};

SelectReposToMirror.contextTypes = {
  actions: React.PropTypes.object,
  router: React.PropTypes.object,
};
