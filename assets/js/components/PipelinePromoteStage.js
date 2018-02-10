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
            pollEventsInterval: null,
            manifestsList: [],
            manifestsMap: {},
        };
    }

    componentDidMount() {
        let repoId = NPECheck(this.props, 'repo/id', '');

        if(!NPECheck(this.props, 'repoDetails/hasRetrievedManifests', true)) {
            this.context.actions.listRepoManifests(repoId, false, null, true);
        }

        this.setState((prevState, props) => {
            return {
                pollEventsInterval: setInterval(() => this.reloadManifests(repoId), 5000),
            }
        });
    }

    componentWillUnmount() {
        clearInterval(this.state.pollEventsInterval);
    }

    reloadManifests(repoId) {
        let prevMarker = NPECheck(this.props, 'repoDetails/manifestsPrevMarker', false);

        if(!prevMarker) {
            this.context.actions.listRepoManifests(repoId, true, null, true);
        }

        this.setState((prevState, props) => {
            let manifestsList = NPECheck(props, 'repoDetails/manifests', [])
                .sort((a, b) => {
                    return (b.pushTime - a.pushTime);
                });
            let manifestsMap = manifestsList.reduce((result, manifest) => {
                result[manifest.manifestId] = manifest;
                return result;
            }, {});
            return {
                loading: false,
                manifestsList: manifestsList,
                manifestsMap: manifestsMap,
            }
        })
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
    renderManifestItem(manifestId, index) {
        let manifest = this.state.manifestsMap[manifestId];
        let repo = this.props.reposMap[manifest.containerRepoId];
        let friendlyTime = (manifest.pushTime) ? ConvertTimeFriendly(manifest.pushTime) : 'Unknown';
        return (
            <div key={manifest.manifestId}
                 className="ListItem FlexRow"
                 onClick={() => this.context.actions.setPromoteStageSource(manifest)}>
                <label>{repo.name}</label>
                <dl>
                    <dt>Last pushed:</dt>
                    <dd>{friendlyTime}</dd>
                    <dt>Tags:</dt>
                    {manifest.tags.map((tag, tagIndex) => {
                        return (
                            <dd key={tagIndex}>{tag}</dd>
                        );
                    })}
                    <dt>Image SHA:</dt>
                    <dd>{manifestId.split(':')[1]}</dd>
                </dl>
            </div>
        )
    }

    listManifestIds() {
        return (this.state.manifestsList.map(manifest => {
            return (manifest.manifestId);
        }));
    }

    renderDropdown() {
        let err = this.props.pipelineStore.listManifestsXHRError;
        if (err !== null) {
            return <Msg text={err} />
        }

        return (
            <Dropdown isOpen={this.state.imageDropdownOpen}
                      toggleOpen={this.toggleDropdownState.bind(this)}
                      listItems={this.listManifestIds()}
                      renderItem={(manifestId, index) => this.renderManifestItem(manifestId, index)}
                      inputPlaceholder="Docker Image Repository"
                      inputClassName="BlueBorder FullWidth White"
                      inputValue={NPECheck(this.props.pipelineStore, 'stagePromotionData/sourceManifestId', "")}
                      XHR={this.state.loading} />
        )
    }

    renderConfirm() {
        return (
            <div>
                {this.renderErrorMsg()}
                <CenteredConfirm confirmButtonText="Promote"
                                 noMessage={true}
                                 confirmButtonStyle={{}}
                                 onConfirm={this.context.actions.runPromoteStage}
                                 onCancel={this.context.actions.clearPromoteStage} />
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
