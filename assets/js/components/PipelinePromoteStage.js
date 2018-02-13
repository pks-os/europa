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
            events: {},
        };
    }

    componentDidMount() {
        let repoId = NPECheck(this.props, 'repo/id', '');

        if(!NPECheck(this.props, 'repoDetails/hasRetrievedEvents', false)) {
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

    renderErrorMsg() {
        let err = NPECheck(this.props.pipelineStore, 'runPromoteStageXHRError', null);
        if (err !== null) {
            return (
                <Msg text={err}
                     close={() => this.context.actions.cleanPipelineXHRErrors()} />
            );
        }
    }

    renderEventItem(eventId, index) {
        let event = this.getEventById(eventId);
        let repo = this.props.reposMap[event.repoId];
        let friendlyTime = (event.eventTime) ? ConvertTimeFriendly(event.eventTime) : 'Unknown';
        return (
            <div key={event.id}
                 className="ListItem FlexRow"
                 onClick={() => this.context.actions.setPromoteStageSource(event)}>
                 <label>{repo.name}</label>
                <dl>
                    <dt>Last pushed:</dt>
                    <dd>{friendlyTime}</dd>
                    <dt>Tags:</dt>
                    {event.imageTags.map((tag, tagIndex) => {
                        return (
                            <dd key={tagIndex}>{tag}</dd>
                        );
                    })}
                    <dt>Image SHA:</dt>
                    <dd>{event.imageSha}</dd>
                </dl>
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

    renderDropdown() {
        let err = this.props.pipelineStore.listManifestsXHRError;
        if (err !== null) {
            return <Msg text={err} />
        }

        return (
            <Dropdown isOpen={this.state.imageDropdownOpen}
                      toggleOpen={this.toggleDropdownState.bind(this)}
                      listItems={this.listEventIds()}
                      renderItem={(eventId, index) => this.renderEventItem(eventId, index)}
                      inputPlaceholder="Docker Image Repository"
                      inputClassName="BlueBorder FullWidth White"
                      inputValue={NPECheck(this.props.pipelineStore, 'stagePromotionData/sourceManifestId', "")}
                      filterFn={this.filterEvents.bind(this)}
                      XHR={NPECheck(this.props, 'repoDetails/eventsXHR', false)} />
        )
    }

    renderConfirm() {
        return (
            <div>
                {this.renderErrorMsg()}
                <CenteredConfirm confirmButtonText="Promote"
                                 noMessage={true}
                                 confirmButtonStyle={{}}
                                 onConfirm={() => this.context.actions.runPromoteStage()}
                                 onCancel={() => this.context.actions.clearPromoteStage()} />
            </div>
        )
    }

    render() {
        let destinationRepo = this.props.reposMap[this.props.destinationComponent.destinationContainerRepoId];
        if (destinationRepo === null) {
            return <Msg text="Missing data for promotion stage" />
        }
        let lastEvent = NPECheck(destinationRepo, 'lastEvent', {
            imageTags: [],
            imageSha: "N/A"
        });
        let friendlyTime = (lastEvent.eventTime) ? ConvertTimeFriendly(lastEvent.eventTime) : 'Unknown';

        return (
            <div>
                <div className="Flex1">
                    <label className="FlexColumn">
                        From — <span className="hint">If you choose to, you can promote a different image tag</span>
                    </label>
                    {this.renderDropdown()}
                </div>
                <div className="Flex1">
                    <div className="FlexColumn">
                        <label className="FlexColumn">
                            To – <span className="hint">If you choose to, you can select a different image tag to promote to by specifying the tag.</span>
                        </label>
                        <div className="FlexColumn">
                            <label>{destinationRepo.name}</label>
                            <div className="meta-details">
                                <strong>Last Pushed:</strong>
                                <span>{friendlyTime}</span>
                            </div>
                        </div>
                    </div>
                    <input className="BlueBorder Blue"
                           value={this.props.pipelineStore.stagePromotionData.destinationTag}
                           placeholder="Tag"
                           onChange={(e) => this.context.actions.setPromoteStageDestinationTag(e.target.value)} />
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
