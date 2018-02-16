import React, {Component, PropTypes} from 'react'
import CenteredConfirm from '../components/CenteredConfirm'
import Dropdown from '../components/Dropdown'
import Msg from '../components/Msg'
import ConvertTimeFriendly from '../util/ConvertTimeFriendly'
import NPECheck from "../util/NPECheck";

export default class PipelinePromoteStage extends Component {
  constructor(props) {
    super(props);
    this.state = {
      imageDropdownOpen: false,
      loading: true,
      tagFilter: '',
      tagValidationError: false,
      events: {},
    };
  }

  componentDidMount() {
    let repoId = NPECheck(this.props, 'repo/id', '');

    if (!NPECheck(this.props, 'repoDetails/hasRetrievedEvents', false)) {
      this.context.actions.listRepoEvents(repoId);
    }
  }

  toggleDropdownState() {
    this.setState((prevState, props) => {
      return {
        imageDropdownOpen: !prevState.imageDropdownOpen,
      };
    });
  }

  updateTagFilter(e) {
    let tagValue = e.target.value;
    this.setState({
      tagFilter: e.target.value,
    })
  }

  updateDestinationTag(tagValue) {
    let validTagRe = /^[a-zA-Z0-9_][a-zA-Z0-9_\-\.]{0,127}$/;
    let isTagValid = !validTagRe.test(tagValue);
    this.setState({
      tagValidationError: isTagValid,
    }, () => this.context.actions.setPromoteStageDestinationTag(tagValue));
  }

  selectEventItem(event) {
    this.updateDestinationTag(event.imageTags[0]);
    this.context.actions.setPromoteStageSource(event);
  }

  renderEventItem(eventId) {
    let event = this.getEventById(eventId);
    let repo = this.props.reposMap[event.repoId];
    let friendlyTime = (event.eventTime) ? ConvertTimeFriendly(event.eventTime) : 'Unknown';
    return (
      <div key={event.id}
           className="ListItem FlexRow"
           onClick={() => this.selectEventItem(event)}>
        <div className="promote-stage-event">
          <div className="promote-stage-repo">
            <i className="icon-dis-disk"/>
            <span>{repo.name}</span>
          </div>
          <div className="promote-stage-metadata">
            <div className="promote-stage-metadata-item promote-stage-metadata-pushed">
              <strong>Pushed:</strong>
              <span>{friendlyTime}</span>
            </div>
            <div className="promote-stage-metadata-item promote-stage-metadata-tags">
              <strong>Tags:</strong>
              <span> {event.imageTags.join(", ")}</span>
            </div>
            <div className="promote-stage-metadata-item promote-stage-metadata-sha">
              <strong>Image SHA:</strong>
              <span>{event.imageSha}</span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  getEventById(eventId) {
    let eventsList = NPECheck(this.props, 'repoDetails/events', []);
    let event = eventsList.find(event => {
      return (event.id === eventId);
    });
    return event;
  }

  listEventIds() {
    let eventsList = NPECheck(this.props, 'repoDetails/events', []);
    return (eventsList.map(event => event.id));
  }

  filterEvents(eventId) {
    let event = this.getEventById(eventId);
    let arrayIdx = event.imageTags.findIndex(tag => {
      let strIdx = tag.indexOf(this.state.tagFilter);
      return (strIdx !== -1);
    });
    let tagFilterStatus = (arrayIdx !== -1);
    let typeFilterStatus = (event.eventType === 'PUSH');
    return (tagFilterStatus && typeFilterStatus);
  }

  renderSelectedEvent() {
    let event = NPECheck(this.props.pipelineStore, 'stagePromotionData/sourceEvent', null);
    if (event === null) {
      return (
        <div className="promote-stage-from-selected cursor-on-hover"
             onClick={this.toggleDropdownState.bind(this)}>
          <span className="placeholder">Select an image…</span>
        </div>
      );
    }

    let repo = this.props.reposMap[event.repoId];
    let friendlyTime = (event.eventTime) ? ConvertTimeFriendly(event.eventTime) : 'Unknown';
    return (
      <div className="promote-stage-from-selected cursor-on-hover"
           onClick={this.toggleDropdownState.bind(this)}>
        <div className="promote-stage-event">
          <div className="promote-stage-repo">
            <i className="icon-dis-disk"/>
            <span>{repo.name}</span>
          </div>
          <div className="promote-stage-metadata">
            <div className="promote-stage-metadata-item promote-stage-metadata-pushed">
              <strong>Pushed:</strong>
              <span>{friendlyTime}</span>
            </div>
            <div className="promote-stage-metadata-item promote-stage-metadata-tags">
              <strong>Tags:</strong>
              <span> {event.imageTags.join(", ")}</span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  renderDropdown() {
    let err = NPECheck(this.props, 'repoDetails/eventsError', null);
    if (err !== null && err !== '') {
      return <Msg text={err}/>
    }

    let classNames = (this.state.imageDropdownOpen) ?
      "BlueBorder FullWidth Search Tiny promote-event-search dropdown-visible" :
      "BlueBorder FullWidth Search Tiny promote-event-search dropdown-hidden";

    return (
      <Dropdown isOpen={this.state.imageDropdownOpen}
                toggleOpen={this.toggleDropdownState.bind(this)}
                listItems={this.listEventIds()}
                renderItem={eventId => this.renderEventItem(eventId)}
                inputPlaceholder="Search by image tag"
                inputClassName={classNames}
                inputValue={this.state.tagFilter}
                inputOnChange={this.updateTagFilter.bind(this)}
                filterFn={this.filterEvents.bind(this)}
                XHR={NPECheck(this.props, 'repoDetails/eventsXHR', false)}/>
    )
  }

  renderDestinationTagInput() {
    let inputClassNames = (this.state.tagValidationError) ? "BlueBorder Blue Error" : "BlueBorder Blue";
    let labelClassNames = (this.state.tagValidationError) ? "promote-stage-destination-tag-label Error" : "promote-stage-destination-tag-label";
    return (
      <div className="promote-stage-destination-tag">
        <input className={inputClassNames}
               value={this.props.pipelineStore.stagePromotionData.destinationTag}
               onChange={(e) => this.updateDestinationTag(e.target.value)}/>
        <div className={labelClassNames}>
          <strong>Tag:</strong>
        </div>
      </div>
    );
  }

  renderTagValidationErrorMsg() {
    if (this.state.tagValidationError) {
      return (
        <Msg
          text="Destination tag must be valid ASCII and may contain lowercase and uppercase letters, digits, underscores, periods and dashes. A tag name may not start with a period or a dash and may contain a maximum of 128 characters."/>
      );
    }
  }

  renderXHRErrorMsg() {
    let err = NPECheck(this.props.pipelineStore, 'runPromoteStageXHRError', null);
    if (err !== null) {
      return (
        <Msg text={err}
             close={() => this.context.actions.clearPipelineXHRErrors()}/>
      );
    }
  }

  renderButton() {
    let sourceTag = NPECheck(this.props.pipelineStore, 'stagePromotionData/sourceTag', null);
    let destinationTag = NPECheck(this.props.pipelineStore, 'stagePromotionData/destinationTag', null);
    if (sourceTag !== null &&
      destinationTag !== null &&
      !this.state.tagValidationError) {
      return (
        <CenteredConfirm confirmButtonText="Promote"
                         noMessage={true}
                         confirmButtonStyle={{}}
                         onConfirm={() => this.context.actions.runPromoteStage()}
                         onCancel={() => this.context.actions.clearPromoteStage()}/>
      );
    }
  }

  renderConfirm() {
    return (
      <div>
        {this.renderTagValidationErrorMsg()}
        {this.renderXHRErrorMsg()}
        {this.renderButton()}
      </div>
    )
  }

  render() {
    let destinationRepo = this.props.reposMap[this.props.destinationComponent.destinationContainerRepoId];
    if (destinationRepo === null) {
      return <Msg text="Missing data for promotion stage"/>
    }
    let lastEvent = NPECheck(destinationRepo, 'lastEvent', {
      imageTags: [],
      imageSha: "N/A"
    });
    let friendlyTime = (lastEvent.eventTime) ? ConvertTimeFriendly(lastEvent.eventTime) : 'Unknown';

    return (
      <div className="pipeline-promote-stage">
        <div className="ContentRow">
          <div className="pipeline-promote-from FlexColumn">
            <div className="FlexColumn">
              <label className="pipeline-promote-from">
                From – <span className="hint">If you choose to, you can promote a different image tag</span>
              </label>
            </div>
            {this.renderSelectedEvent()}
            {this.renderDropdown()}
          </div>
        </div>
        <div className="ContentRow">
          <div className="pipeline-promote-to FlexColumn">
            <div className="FlexColumn">
              <label className="pipeline-promote-to">
                To – <span className="hint">If you choose to, you can select a different image tag to promote to by specifying the tag.</span>
              </label>
            </div>
            <div className="promote-stage-to-event">
              <div className="promote-stage-event">
                <div className="promote-stage-repo">
                  <i className="icon-dis-disk"/>
                  <span>{destinationRepo.name}</span>
                </div>
                <div className="promote-stage-metadata">
                  <div className="promote-stage-metadata-item promote-stage-metadata-pushed">
                    <strong>Pushed:</strong>
                    <span>{friendlyTime}</span>
                  </div>
                </div>
              </div>
              {this.renderDestinationTagInput()}
            </div>
          </div>
        </div>
        <div className="Flex1">
          {this.renderConfirm()}
        </div>
      </div>
    );
  }
}

PipelinePromoteStage.propTypes = {
  repo: PropTypes.object.isRequired,
  destinationComponent: PropTypes.object.isRequired,
};

PipelinePromoteStage.childContextTypes = {
  actions: PropTypes.object,
  router: PropTypes.object,
};

PipelinePromoteStage.contextTypes = {
  actions: PropTypes.object,
  router: PropTypes.object,
};
