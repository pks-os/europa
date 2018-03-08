import React, {Component, PropTypes} from 'react'
import Dropdown from '../components/Dropdown';
import RegistryProviderIcons from './../util/RegistryProviderIcons'
import NPECheck from "../util/NPECheck";

export default class RepoSelector extends Component {
  constructor(props) {
    super(props);
    this.state = {
      dropdownOpen: false,
    };
  }

  toggleDropdownState() {
    this.setState((prevState, props) => {
      return {
        dropdownOpen: !prevState.dropdownOpen,
      };
    });
  }

  renderItem(repo, index) {
    return (
      <div key={index}
           className="ListItem FlexRow"
           onClick={() => this.props.onSelectFn(repo)}>
        <img src={RegistryProviderIcons(repo.provider)}/>
        {repo.name}
      </div>
    );
  }

  render() {
    return (
      <div className="RepoSelector">
        <label className="small FlexColumn">
          Docker Image Repository
        </label>
        <Dropdown isOpen={this.state.dropdownOpen}
                  toggleOpen={this.toggleDropdownState.bind(this)}
                  listItems={this.props.repoList.filter(this.props.filterFn)}
                  renderItem={this.renderItem.bind(this)}
                  inputPlaceholder="Docker Image Repository"
                  inputClassName="BlueBorder FullWidth White"
                  inputValue={this.props.valueFn()}
                  className={"Flex1"}/>
      </div>
    )
  }
}

RepoSelector.propTypes = {
  onSelectFn: PropTypes.func.isRequired,
  repoList: PropTypes.arrayOf(PropTypes.object).isRequired,
  valueFn: PropTypes.func.isRequired,
  filterFn: PropTypes.func,
};

RepoSelector.defaultProps = {
  filterFn: (() => true),
};

RepoSelector.childContextTypes = {
  actions: PropTypes.object,
  router: PropTypes.object
};

RepoSelector.contextTypes = {
  actions: PropTypes.object,
  router: PropTypes.object
};
