import React from "react"

export default class Selector extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};

    this.setWrapperRef = this.setWrapperRef.bind(this);
    this.handleClickOutside = this.handleClickOutside.bind(this);
  }

  componentDidMount() {
    document.addEventListener('click', this.handleClickOutside);
  }

  componentWillUnmount() {
    document.removeEventListener('click', this.handleClickOutside);
  }

  setWrapperRef(node) {
    this.wrapperRef = node;
  }

  handleClickOutside(e) {
    if (this.wrapperRef && !this.wrapperRef.contains(e.target)) {
      this.props.toggleOpen();
    }
  }

  renderToggleItem(option, key) {
    let className = (this.props.currentValue === option) ? 'SelectorToggle active' : 'SelectorToggle inactive';
    let value = (option)
      ? <div className="SelectorToggleValue">{this.props.renderItem(option)}</div>
      : null;
    let iconClass = (this.props.isOpen) ? "icon icon-dis-collapse" : "icon icon-dis-expand";
    return (
      <div className={className}
           key={key}
           onClick={this.props.toggleOpen}>
        <div className="SelectorToggleLabel">
          <label>{this.props.labelText}</label>
        </div>
        {value}
        <div className="SelectorToggleIcon">
          <i className={iconClass}/>
        </div>
      </div>
    );
  }

  renderToggle() {
    let options = this.props.listItems;
    let currentValue = null;
    if (!options.includes(this.props.currentValue)) {
      options.push(currentValue);
    }
    return (
      <div className="SelectorToggleWrapper">
        {currentValue}
        {options.map(this.renderToggleItem.bind(this))}
      </div>
    );
  }

  renderListItem(item, key) {
    return (
      <div className="SelectorListItemWrapper Clickable"
           onClick={() => this.props.onClick(item)}
           key={key}>
        {this.props.renderItem(item)}
      </div>
    );
  }

  renderDropdown() {
    if (this.props.isOpen) {
      return (
        <div className="SelectorDropdown">
          {this.props.listItems.map(this.renderListItem.bind(this))}
        </div>
      );
    }
  }

  render() {
    return (
      <div className="SelectorContainer cursor-on-hover">
        {this.renderToggle()}
        {this.renderDropdown()}
      </div>
    )
  }
}

Selector.propTypes = {
  isOpen: React.PropTypes.bool.isRequired,
  toggleOpen: React.PropTypes.func.isRequired,
  listItems: React.PropTypes.array.isRequired,
  onClick: React.PropTypes.func.isRequired,
  currentValue: React.PropTypes.any,
  renderItem: React.PropTypes.func,
  labelText: React.PropTypes.string,
};

Selector.defaultProps = {
  renderItem: defaultRenderItem,
  labelText: "Select:",
};

function defaultRenderItem(item) {
  return (
    <span className="SelectorItem">{item.toString()}</span>
  );
}

