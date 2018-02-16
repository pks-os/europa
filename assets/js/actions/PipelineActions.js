import Reducers from './../reducers/AddRepoReducers'
import * as GR from './../reducers/GeneralReducers'
import * as RAjax from './../util/RAjax'
import NPECheck from './../util/NPECheck'
import * as PipelineComponents from '../util/PipelineComponents';
import {
  notifState,
  isAddNotificationValid
} from './NotificationActions'

import {
  newRegistryState
} from './RegistryActions'
import {
  resetRepoDetailsState
} from './RepoActions'

export function pipelinesState() {
  return {
    isBlocked: false,
    initNewPipeline: false,
    newPipelineTemplate: {
      name: null,
      errorFields: []
    },
    pipelines: [],
    filteredPipelines: null,
    // XHR
    pipelinesXHR: false,
    newPipelineXHR: false,
    removePipelineXHR: false,

    // XHR Error
    newPipelineXHRError: null,
  }
}

export function singlePipelineState() {
  return {
    isBlocked: false,
    noPipeline: false,
    pipeline: null,
    newComponentData: null,
    section: null,
    stagePromotionData: {
      sourceRepoId: null,
      destinationComponent: null,
      destinationTag: null,
    },

    // XHR
    getPipelineXHR: false,
    removePipelineXHR: false,
    setContainerRepoXHR: false,
    addPipelineComponentXHR: false,
    movePipelineComponentXHR: false,
    removePipelineComponentXHR: false,
    removePipelineMainStageXHR: false,
    listManifestsXHR: false,
    runPromoteStageXHR: false,

    // XHR Error
    setContainerRepoXHRError: null,
    addPipelineComponentXHRError: null,
    movePipelineComponentXHRError: null,
    removePipelineComponentXHRError: null,
    removePipelineMainStageXHRError: null,
    listManifestsXHRError: null,
    runPromoteStageXHRError: null,
  }
}

export function clearPipelinesXHRErrors() {
  this.setState({
    pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
      newPipelineXHRError: null,
    })
  })
}

export function clearPipelineXHRErrors() {
  this.setState({
    pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
      newPipelineXHRError: null,
      setContainerRepoXHRError: null,
      addPipelineComponentXHRError: null,
      movePipelineComponentXHRError: null,
      removePipelineComponentXHRError: null,
      listManifestsXHRError: null,
      runPromoteStageXHRError: null,
    })
  })
}

export function resetSinglePipelineState() {
  this.setState({
    pipelineStore: GR.modifyProperty(this.state.pipelineStore, singlePipelineState.call(this))
  });
}

export function resetPipelinesState() {
  this.setState({
    pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, pipelinesState.call(this))
  });
}

export function setPipelinePageSection(section) {
  this.setState((prevState, props) => {
    return {
      pipelineStore: GR.modifyProperty(prevState.pipelineStore, {
        section: section
      })
    }
  })
}

export function updateRepoConnect(repo) {
  this.setState({
    pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
      newComponentData: {
        destinationContainerRepoDomain: repo.domain,
        destinationContainerRepoId: repo.id,
        destinationContainerRepoName: repo.name,
      }
    })
  })
}

export function toggleInitNewPipeline() {
  this.setState({
    pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
      initNewPipeline: !this.state.pipelinesStore.initNewPipeline
    })
  }, () => {
    // Reset if the user closes the modal without creating
    if (!this.state.pipelinesStore.initNewPipeline) {
      resetNewPipelineTemplate.call(this);
    }
  })
}

export function resetNewPipelineTemplate() {
  this.setState({
    pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
      newPipelineTemplate: pipelinesState()["newPipelineTemplate"]
    })
  })
}

export function updateNewPipelineTemplate(field, value) {
  this.setState({
    pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
      newPipelineTemplate: {
        ...this.state.pipelinesStore.newPipelineTemplate,
        [field]: value
      }
    })
  })
}

export function filterPipelines(filterString) {
  let filteredPipelines = this.state.pipelinesStore.pipelines.slice(0).filter(pipeline => {
    return pipeline.name.indexOf(filterString) != -1;
  });

  this.setState({
    pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
      filteredPipelines: filteredPipelines
    })
  })
}

// Read Permissions
export function listPipelines() {
  return new Promise((resolve, reject) => {
    this.setState({
      pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
        pipelinesXHR: !NPECheck(this.state, 'pipelinesStore/pipelines/length', false),
      })
    }, () => {
      RAjax.GET.call(this, 'ListPipelines')
      .then(res => {
        this.setState({
          pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
            isBlocked: false,
            pipelines: res,
            pipelinesXHR: false,
          })
        }, () => resolve());
      })
      .catch(err => {
        let errorMsg = NPECheck(err, 'error/message', 'There was an error loading your pipelines');
        if (errorMsg == 'You do not have access to this operation') {
          this.setState({
            pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
              isBlocked: true,
              pipelinesXHR: false,
            })
          }, () => reject());
        } else {
          this.setState({
            pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
              isBlocked: false,
              pipelinesXHR: false,
            })
          }, () => reject());
        }
      });
    });
  });
}

export function getPipeline(pipelineId) {
  return new Promise((resolve, reject) => {
    this.setState({
      pipelinesStore: GR.modifyProperty(this.state.pipelineStore, {
        getPipelineXHR: true,
      })
    }, () => {
      RAjax.GET.call(this, 'GetPipeline', {
        pipelineId
      })
      .then(res => {
        this.setState({
          pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
            isBlocked: false,
            pipeline: res,
            getPipelineXHR: false,
          })
        }, () => resolve(res));
      })
      .catch(err => {
        let errorMsg = NPECheck(err, 'error/message', 'There was an error loading your pipeline');
        if (errorMsg == 'You do not have access to this operation') {
          this.setState({
            pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
              isBlocked: true,
              getPipelineXHR: false,
              noPipeline: false,
            })
          }, () => reject());
        } else if (errorMsg == 'The specified Pipeline was not found') {
          this.setState({
            pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
              noPipeline: true,
              isBlocked: false,
              getPipelineXHR: false,
            })
          }, () => reject());
        } else {
          this.setState({
            pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
              noPipeline: false,
              isBlocked: false,
              getPipelineXHR: false,
            })
          }, () => reject());
        }
      });
    });
  });
}

export function setContainerRepo() {
  const postData = {
    pipelineId: NPECheck(this.state.pipelineStore, 'pipeline/id', null),
    containerRepoId: NPECheck(this.state.pipelineStore, 'newComponentData/destinationContainerRepoId', null)
  };

  return new Promise((resolve, reject) => {
    this.setState({
      pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
        setContainerRepoXHR: true,
        setContainerRepoXHRError: false,
      })
    }, () => {
      RAjax.POST.call(this, 'SetPipelineContainerRepoId', {}, postData)
      .then(res => {
        this.setState({
          pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
            pipeline: res,
            setContainerRepoXHR: false,
            setContainerRepoXHRError: false,
            newComponentData: null,
            section: null
          })
        }, () => resolve());
      })
      .catch(err => {
        this.setState({
          pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
            setContainerRepoXHR: false,
            setContainerRepoXHRError: NPECheck(err, 'error/message', "")
          })
        }, () => reject());
      });
    });
  });
}

export function createPipeline() {
  let newPipeline = {
    ...this.state.pipelinesStore.newPipelineTemplate
  };

  // Remove validation if clean
  delete newPipeline["errorFields"];

  return new Promise((resolve, reject) => {
    this.setState({
      pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
        newPipelineXHR: true,
      })
    }, () => {
      RAjax.POST.call(this, 'NewPipeline', {}, newPipeline)
      .then(res => {
        // Redirect to the pipeline
        this.context.router.push(`/pipelines/${res.name}`);
        resolve();
      })
      .catch(err => {
        this.setState({
          pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
            newPipelineXHR: false,
            newPipelineXHRError: NPECheck(err, 'error/message', "")
          })
        });
      });
    });
  });
}

export function removePipeline() {
  const postData = {
    pipelineId: this.state.pipelineStore.pipeline.id
  };

  return new Promise((resolve, reject) => {
    this.setState({
      pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
        removePipelineXHR: true,
      })
    }, () => {
      RAjax.POST.call(this, 'RemovePipeline', {}, postData)
      .then(res => {
        this.context.router.push('/pipelines');
        setTimeout(function () {
          resetSinglePipelineState.call(this)
        }.bind(this), 0)
      })
      .catch(err => {
        this.setState({
          pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
            removePipelineXHR: false,
            removePipelineXHRError: NPECheck(err, 'error/message', "")
          })
        }, () => reject());
      });
    });
  });
}

export function addPipelineComponent(componentType, beforeComponentId = null) {
  const postData = {
    type: componentType['value'],
    pipelineId: this.state.pipelineStore.pipeline.id,
    beforeComponentId: beforeComponentId,
  };

  const content = componentType.customProperties.reduce((result, propertyName) => {
    result[propertyName] = NPECheck(this.state.pipelineStore, `newComponentData/${propertyName}`, null);
    return result;
  }, {});

  return new Promise((resolve, reject) => {
    this.setState({
      pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
        addPipelineComponentXHR: true,
      })
    }, () => {
      RAjax.POST.call(this, 'AddPipelineComponent', content, postData)
      .then(res => {
        this.setState({
          pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
            pipeline: res,
            newComponentData: null,
            addPipelineComponentXHR: false,
            addPipelineComponentXHRError: false,
            section: null,
          })
        }, () => resolve(res));
      })
      .catch(err => {
        this.setState({
          pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
            addPipelineComponentXHR: false,
            addPipelineComponentXHRError: NPECheck(err, 'error/message', "")
          })
        });
      });
    });
  });
}

export function movePipelineComponent(postData) {
  return new Promise((resolve, reject) => {
    this.setState({
      pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
        movePipelineComponentXHR: true,
      })
    }, () => {
      RAjax.POST.call(this, 'MovePipelineComponent', {}, postData)
      .then(res => {
        // TODO
        resolve();
      })
      .catch(err => {
        this.setState({
          pipelinesStore: GR.modifyProperty(this.state.pipelinesStore, {
            movePipelineComponentXHR: false,
          })
        });
      });
    });
  });
}

export function removePipelineComponent(pipelineComponentId) {
  const postData = {
    pipelineComponentId: pipelineComponentId,
    pipelineId: this.state.pipelineStore.pipeline.id
  };

  return new Promise((resolve, reject) => {
    this.setState({
      pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
        removePipelineComponentXHR: pipelineComponentId,
      })
    }, () => {
      RAjax.POST.call(this, 'RemovePipelineComponent', {}, postData)
      .then(res => {
        this.setState({
          pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
            pipeline: res,
            removePipelineComponentXHR: false,
          })
        }, () => resolve(res));
      })
      .catch(err => {
        this.setState({
          pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
            removePipelineComponentXHR: false,
            removePipelineComponentXHRError: NPECheck(err, 'error/message', "")
          })
        });
      });
    });
  });
}

export function removeMainPipelineStage() {
  const postData = {
    pipelineId: NPECheck(this.state.pipelineStore, 'pipeline/id', null),
  };

  return new Promise((resolve, reject) => {
    this.setState({
      pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
        removePipelineMainStageXHR: true,
      })
    }, () => {
      RAjax.POST.call(this, 'DeletePipelineContainerRepoId', {}, postData)
      .then(res => {
        this.setState({
          pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
            pipeline: res,
            removePipelineMainStageXHR: false,
          })
        }, () => resolve(res));
      })
      .catch(err => {
        this.setState({
          pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
            removePipelineMainStageXHR: false,
            removePipelineMainStageXHRError: NPECheck(err, 'error/message', "")
          })
        });
      });
    });
  });
}

export function openPromoteStage(sourceRepoId, destinationComponent) {
  this.setState((prevState, props) => {
    return {
      pipelineStore: GR.modifyProperty(prevState.pipelineStore, {
        stagePromotionData: {
          sourceRepoId: sourceRepoId,
          destinationComponent: destinationComponent,
        },
      })
    }
  }, () => setPipelinePageSection.call(this, 'PROMOTE_STAGE'));
}

export function setPromoteStageSource(event) {
  this.setState((prevState, props) => {
    return {
      pipelineStore: GR.modifyProperty(prevState.pipelineStore, {
        stagePromotionData: GR.modifyProperty(prevState.pipelineStore.stagePromotionData, {
          sourceEvent: event,
          sourceTag: event.imageSha,
        }),
      }),
    };
  });
}

export function setPromoteStageDestinationTag(tagName) {
  this.setState((prevState, props) => {
    return {
      pipelineStore: GR.modifyProperty(prevState.pipelineStore, {
        stagePromotionData: GR.modifyProperty(prevState.pipelineStore.stagePromotionData, {
          destinationTag: tagName,
        }),
      }),
    };
  });
}

export function runPromoteStage() {
  let pipelineId = NPECheck(this.state.pipelineStore, 'pipeline/id', null);
  let componentId = NPECheck(this.state.pipelineStore, 'stagePromotionData/destinationComponent/id', null);
  let sourceRepoId = NPECheck(this.state.pipelineStore, 'stagePromotionData/sourceRepoId', null);
  let sourceTag = NPECheck(this.state.pipelineStore, 'stagePromotionData/sourceTag', null);
  let destinationTag = NPECheck(this.state.pipelineStore, 'stagePromotionData/destinationTag', null);
  if (pipelineId === null ||
      componentId === null ||
      sourceRepoId === null ||
      sourceTag === null ||
      destinationTag === null) {
    this.setState((prevState, props) => {
      return {
        pipelineStore: GR.modifyProperty(prevState.pipelineStore, {
          runPromoteStageXHRError: 'Missing required data to promote',
        })
      };
    });
    return;
  }

  let params = {
    pipelineId: pipelineId,
    componentId: componentId,
    sourceRepoId: sourceRepoId,
    sourceTag: sourceTag,
    destinationTag: destinationTag,
  };

  return new Promise((resolve, reject) => {
    this.setState((prevState, props) => {
      return {
        pipelineStore: GR.modifyProperty(this.state.pipelineStore, {
          runPromoteStageXHR: true,
        })
      }
    }, () => {
      RAjax.POST.call(this, 'RunPipelineManualPromotion', {}, params)
      .then(res => {
        this.setState((prevState, props) => {
          return {
            pipelineStore: GR.modifyProperty(prevState.pipelineStore, {
              stagePromotionData: {
                sourceRepoId: null,
                destinationComponent: null,
                destinationTag: null,
              },
              section: null,
              pipeline: res,
              runPromoteStageXHR: false,
            }),
          };
        }, () => resolve(res));
      })
      .catch(err => {
        this.setState((prevState, props) => {
          return {
            pipelineStore: GR.modifyProperty(prevState.pipelineStore, {
              runPromoteStageXHR: false,
              runPromoteStageXHRError: NPECheck(err, 'error/message', ""),
            }),
          };
        });
      });
    });
  });
}

export function clearPromoteStage() {
  resetRepoDetailsState.call(this);
  this.setState((prevState, props) => {
    return {
      stagePromotionData: {
        event: null,
        sourceTag: null,
        sourceRepoId: null,
        destinationComponent: null,
        destinationTag: null,
      },
    }
  }, () => setPipelinePageSection.call(this, null));
}

export function togglePipelineComponentAutomaticPromotion(component) {
  let componentList = NPECheck(this.state.pipelineStore, 'pipeline/components', []);
  let componentIndex = componentList.indexOf(component);
  if (componentIndex === -1) {
    return;
  }
  if (componentIndex === 0) {
    setPipelineComponentToManualPromotion.call(this, component);
  } else {
    let previousComponent = componentList[componentIndex - 1];
    if (PipelineComponents.guessPipelineComponentType(previousComponent) === PipelineComponents.types.manualPromotionGate) {
      removePipelineComponent.call(this, previousComponent.id);
    } else {
      setPipelineComponentToManualPromotion.call(this, component);
    }
  }
}

function setPipelineComponentToManualPromotion(component) {
  addPipelineComponent.call(this, PipelineComponents.types.manualPromotionGate, component.id);
}
