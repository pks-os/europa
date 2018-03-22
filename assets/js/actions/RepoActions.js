/*
  @author Sam Heutmaker [samheutmaker@gmail.com]
*/

import Reducers from './../reducers/AddRepoReducers'
import * as GA from './../reducers/GeneralReducers'
import * as RAjax from './../util/RAjax'
import Validate from './../util/Validate'
import NPECheck from './../util/NPECheck'
import Debounce from './../util/Debounce'
import ErrorMessages from './../util/ErrorMessages'
import {
  notifState,
  isAddNotificationValid
} from './NotificationActions'
import {
  newRegistryState
} from './RegistryActions'
import {
  getRepoRedirect
} from './../util/RedirectHelper'

// *************************************************
// General Repo Actions
// *************************************************


// Read Permissions
export function listRepos(repoId) {
  return new Promise((resolve, reject) => {
    this.setState((prevState, props) => {
      return {
        reposXHR: (prevState.repos.length > 1) ? false : true,
        reposFilterQuery: ""
      }
    }, () => {

      let params = {};
      let op = 'ListContainerRepos';

      if (repoId) {
        params.id = repoId;
        op = 'GetContainerRepo'
      }

      RAjax.GET.call(this, op, params)
      .then((res) => {

        if (!Array.isArray(res)) {
          res = [res];
        }

        let reposMap = res.reduce((cur, repo) => {
          cur[repo.id] = repo;
          return cur;
        }, {});

        let reposNameMap = res.reduce((cur, repo) => {
          cur[getRepoRedirect(repo)] = repo;
          return cur;
        }, {});

        this.setState({
          repos: res,
          reposMap: reposMap,
          reposNameMap: reposNameMap,
          reposXHR: false
        }, () => resolve());
      })
      .catch((err) => {
        console.error(err);
        let errorMsg = `${err.error.message}`;

        if (errorMsg === ErrorMessages.UNAUTHORIZED) {
          this.setState((prevState, props) => {
            return {
              reposXHR: false,
              repoDetails: GA.modifyProperty(prevState.repoDetails, {
                isBlocked: true
              })
            }
          }, () => reject());
        } else {
          this.setState({
            reposXHR: false
          }, () => reject());
        }

      });
    });
  });
}

export function filterRepos(e, eIsValue) {
  let value = (eIsValue) ? e : e.target.value;

  this.setState({
    reposFilterQuery: value
  });
}

export function toggleReposMirrorSelectorOpen() {
  this.setState((prevState, props) => {
    return {
      reposMirrorSelectorOpen: !prevState.reposMirrorSelectorOpen,
    }
  });
}

export function setReposMirrorFilter(filter) {
  this.setState({
    reposMirrorSelectorOpen: false,
    reposMirrorFilter: filter,
  });
}


// *************************************************
// Add Repo Actions
// *************************************************

export function addRepoState() {
  return {
    errorMsg: '',
    errorFields: [],
    validateOnInput: false,
    newRepoCredsType: 'EXISTING',
    success: null,
    XHR: false,
    selectExistingCredentialsDropdown: false,
    selectRepoDropdown: false,
    reposInRegistryXHR: false,
    reposInRegistry: [],
    reposInRegistryQuery: '',
    newRepo: {
      repo: {
        credId: '',
        name: ''
      }
    },
    isCreatingLocalRepo: false,
    isCreatingRepoMirror: false,
    createLocalName: '',
    createLocalXHR: false,
    createLocalError: '',
    createMirrorName: '',
    createMirrorSourceId: '',
    createMirrorSourceName: '',
    createMirrorXHR: false,
    createMirrorError: '',
  };
}

export function resetAddRepoState() {
  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, addRepoState.call(this))
  });
}

export function updateNewLocalRepoName(e, eIsValue = false) {
  let value = (eIsValue) ? e : e.target.value;
  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, {
      createLocalName: value,
      createLocalError: ''
    })
  });
}

export function clearCreateLocalRepoErrors() {
  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, {
      createLocalError: ''
    })
  });
}

export function toggleCreateNewLocalRepo() {
  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, {
      isCreatingLocalRepo: !NPECheck(this.state, 'addRepo/isCreatingLocalRepo', true)
    })
  });
}

export function updateNewRepoMirrorName(e, eIsValue = false) {
  let value = (eIsValue) ? e : e.target.value;
  this.setState((prevState, props) => {
    return {
      addRepo: GA.modifyProperty(prevState.addRepo, {
        createMirrorName: value,
        createMirrorError: '',
      })
    };
  });
}

export function updateNewMirrorSource(repo) {
  let sourceRepoId = repo.id;
  let sourceRepoName = repo.name;
  if (sourceRepoId === null || sourceRepoName === null) {
    return;
  }
  this.setState((prevState, props) => {
    return {
      addRepo: GA.modifyProperty(prevState.addRepo, {
        createMirrorSourceId: sourceRepoId,
        createMirrorSourceName: sourceRepoName,
        createMirrorError: '',
      })
    };
  });
}

export function clearCreateRepoMirrorErrors() {
  this.setState((prevState, props) => {
    return {
      addRepo: GA.modifyProperty(prevState.addRepo, {
        createMirrorError: '',
      })
    };
  });
}

export function toggleCreateNewRepoMirror() {
  this.setState((prevState, props) => {
    return {
      addRepo: GA.modifyProperty(prevState.addRepo, {
        isCreatingRepoMirror: !NPECheck(prevState, 'addRepo/isCreatingRepoMirror', true)
      })
    };
  });
}

// Create Permissions
export function createLocalRepo() {
  return new Promise((resolve, reject) => {

    let repoName = NPECheck(this.state, 'addRepo/createLocalName', '');

    if (!repoName) {
      this.setState({
        addRepo: GA.modifyProperty(this.state.addRepo, {
          createLocalError: 'Invalid repository name.'
        })
      }, () => reject());
      return;
    }

    this.setState({
      addRepo: GA.modifyProperty(this.state.addRepo, {
        createLocalXHR: true
      })
    }, () => {

      RAjax.POST.call(this, 'CreateLocalRepo', {}, {
        repoName
      })
      .then((res) => {
        this.setState({
          addRepo: GA.modifyProperty(this.state.addRepo, {
            createLocalXHR: false
          })
        }, () => resolve(res));
      })
      .catch((err) => {
        console.error(err);
        let errorMsg = `There was an error creating your repository: ${err.error.message}`
        this.setState({
          addRepo: GA.modifyProperty(this.state.addRepo, {
            createLocalXHR: false,
            createLocalError: errorMsg
          })
        }, () => reject());
      });
    });
  });
}

export function createRepoMirror() {
  return new Promise((resolve, reject) => {

    let repoName = NPECheck(this.state, 'addRepo/createMirrorName', '');

    if (!repoName) {
      this.setState((prevState, props) => {
        return {
          addRepo: GA.modifyProperty(prevState.addRepo, {
            createMirrorError: 'Invalid repository name.'
          })
        }
      }, () => reject());
      return;
    }

    let sourceRepo = NPECheck(this.state, 'addRepo/createMirrorSourceId', '');

    if (!sourceRepo) {
      this.setState((prevState, props) => {
        return {
          addRepo: GA.modifyProperty(prevState.addRepo, {
            createMirrorError: 'Invalid source repository.'
          })
        }
      }, () => reject());
      return;
    }

    this.setState((prevState, props) => {
      return {
        addRepo: GA.modifyProperty(this.state.addRepo, {
          createMirrorXHR: true
        })
      }
    }, () => {

      RAjax.POST.call(this, 'CreateRepoMirror', {}, {
        repoName: repoName,
        sourceRepoId: sourceRepo,
      })
        .then((res) => {
          this.setState((prevState, props) => {
            return {
              addRepo: GA.modifyProperty(this.state.addRepo, {
                createMirrorXHR: false
              })
            };
          }, () => resolve(res));
        })
        .catch((err) => {
          console.error(err);
          let errorMsg = `There was an error creating your repository: ${err.error.message}`
          this.setState((prevState, props) => {
            return {
              addRepo: GA.modifyProperty(this.state.addRepo, {
                createMirrorXHR: false,
                createMirrorError: errorMsg
              })
            };
          }, () => reject());
        });
    });
  });
}
export function updateNewRepoField(keyPath, e, eIsValue = false) {
  let value = (eIsValue) ? e : e.target.value;

  this.setState({
    addRepo: Reducers(this.state.addRepo, {
      type: 'UPDATE_NEW_REPO',
      data: {
        keyPath,
        value
      }
    })
  }, () => {
    if (this.state.addRepo.validateOnInput) isAddRepoValid.call(this, true);
    if (keyPath == 'repo/credId') listReposForRegistry.call(this);
  })
};

export function resetCurrentRepoSearch() {
  return new Promise((resolve, reject) => {
    this.setState({
      addRepo: GA.modifyProperty(this.state.addRepo, {
        reposInRegistry: [],
        reposInRegistryQuery: '',
        newRepo: {
          repo: {
            ...this.state.addRepo.newRepo.repo,
            name: ''
          }
        }
      })
    }, () => resolve());
  });
}

export function listReposForRegistry() {
  listReposInRegistryDebounced.call(this);
}

let listReposInRegistryDebounced = Debounce(function () {
  let credId = NPECheck(this.state.addRepo, 'newRepo/repo/credId', null);
  let registry = (this.state.addRepo.newRepoCredsType == 'EXISTING') ? this.state.registriesMap[credId] : this.state.addRegistry.newRegistry

  let registriesWithRepos = ['GCR', 'ECR', 'DOCKERHUB'];

  if (registry && registriesWithRepos.includes(registry.provider)) {
    this.setState({
      addRepo: GA.modifyProperty(this.state.addRepo, {
        reposInRegistryXHR: (!NPECheck(this.state, 'addRepo/reposInRegistry/length', false)),
        reposInRegistryQuery: '',
      })
    }, () => {
      RAjax.POST.call(this, 'ListReposInRegistry', {}, credId ? {
        credId
      } : registry)
      .then((res) => {
        this.setState({
          addRepo: GA.modifyProperty(this.state.addRepo, {
            reposInRegistry: res,
            errorMsg: '',
            reposInRegistryXHR: false,
          })
        });
      })
      .catch((err) => {
        this.setState({
          addRepo: GA.modifyProperty(this.state.addRepo, {
            reposInRegistry: [],
            errorMsg: 'Unable to list repositories for selected registry. Please check your credentials.',
            reposInRegistryXHR: false,
          })
        });
      });
    });
  } else {
    this.setState({
      addRepo: GA.modifyProperty(this.state.addRepo, {
        reposInRegistry: [],
      })
    });
  }
}, 200);

export function clearReposInRegistry() {
  return new Promise((resolve, reject) => {
    this.setState({
      addRepo: GA.modifyProperty(this.state.addRepo, {
        reposInRegistry: [],
      })
    }, () => resolve());
  });
}

export function updateReposInRegisterQuery(e, eIsValue) {
  let value = (eIsValue) ? e : e.target.value;

  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, {
      reposInRegistryQuery: value
    })
  });
}

export function toggleSelectRepoDropdown() {
  return new Promise((resolve, reject) => {
    this.setState({
      addRepo: GA.modifyProperty(this.state.addRepo, {
        selectRepoDropdown: !this.state.addRepo.selectRepoDropdown
      })
    }, () => resolve());
  });
}

export function setNewRepoCredsType(type) {
  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, {
      ...addRepoState.call(this),
      newRepoCredsType: type,
    }),
    addRegistry: GA.modifyProperty(this.state.addRegistry, {
      errorFields: [],
      errorMsg: '',
      newRegistry: newRegistryState.call(this)
    })
  });
}

export function selectCredsForNewRepo(e, value) {
  let id;

  if (e) {
    id = JSON.parse(e.target.value).id
  } else if (value) {
    id = value
  }

  updateNewRepoField.call(this, 'repo/credId', id, true);
}

export function toggleSelectExistingCredsDropdown() {
  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, {
      selectExistingCredentialsDropdown: !this.state.addRepo.selectExistingCredentialsDropdown
    })
  });
}

// Create Permissions
export function addRepoRequest(afterAddCb) {
  if (!isAddRepoValid.call(this, true)) return;

  let postData = {
    repo: this.state.addRepo.newRepo.repo,
  };

  let notif = NPECheck(this.state, 'notif/newNotification', {});

  let shouldIncludeNotif = Object.keys(notif)
  .reduce((cur, next) => {
    cur = (!!notif[next]) ? cur + 1 : cur
    return cur;
  }, 0) > 1;

  if (shouldIncludeNotif) {
    if (!isAddNotificationValid.call(this)) return;

    postData.notification = notif;
  }

  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, {
      XHR: true
    })
  }, () => {

    RAjax.POST.call(this, 'SaveContainerRepo', postData)
    .then((res) => {
      this.setState({
        addRepo: GA.modifyProperty(this.state.addRepo, {
          XHR: false,
          success: true,
        }),
        notif: notifState.call(this)
      }, () => {
        listRepos.call(this)

        if (afterAddCb) afterAddCb(res.id);
      })
    })
    .catch((err) => {
      console.error(err);
      let errorMsg = `There was an error adding your repository: ${err.error.message}`
      this.setState({
        addRepo: GA.modifyProperty(this.state.addRepo, {
          XHR: false,
          success: false,
          errorMsg,
        })
      })
    });
  });
}

export function clearAddRepoSuccess() {
  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, {
      success: null
    })
  });
}

export function canAddRepo() {
  return this.state.addRepo.errorMsg == '' && this.state.addRegistry.errorFields.length == 0;
}


export function clearAddRepoError() {
  this.setState({
    addRepo: GA.modifyProperty(this.state.addRepo, {
      errorMsg: '',
      errorFields: [],
      validateOnInput: false
    })
  });
}

function isAddRepoValid(validateOnInput) {

  let required = {
    repo: {
      credId: 'Registry Provider',
      name: 'Docker Repository'
    }
  };

  let errorFields = Validate.call(this, this.state.addRepo.newRepo, required);

  if (errorFields.names.length) {
    let errorMsg = `Missing required fields: ${errorFields.names.join(', ')}`;
    this.setState({
      addRepo: GA.modifyProperty(this.state.addRepo, {
        errorMsg,
        validateOnInput,
        errorFields: errorFields.keys,
      })
    });
    return false
  } else {
    this.setState({
      addRepo: GA.modifyProperty(this.state.addRepo, {
        errorMsg: '',
        errorFields: []
      })
    });
    return true
  }
}

// *************************************************
// Add Mirrors Actions
// *************************************************

export function addMirrorsState() {
  return {
    selectedRepos: {},
    selectorOpen: false,
    addMirrorsError: '',
    addMirrorsXHR: false,
  }
}

export function resetAddMirrorsState() {
  this.setState((prevState, props) => {
    return {
      addMirrors: GA.modifyProperty(prevState.addMirrors, addMirrorsState.call(this)),
    };
  }, () => resetAddRepoState.call(this));
}

export function clearAddMirrorsError() {
  this.setState((prevState, props) => {
    return {
      addMirrors: GA.modifyProperty(prevState.addMirrors, {
        addMirrorsError: '',
      }),
    };
  });
}

export function addMirrors(afterAddCb) {
  let selectedRepos = getAddMirrorsSelectedRepoData.call(this);
  if (!canAddMirrors.call(this)) {
    let errorMsg = (selectedRepos.length === 0) ? 'No repos selected to mirror' : 'Proposed name conflicts with existing repo';
    this.setState((prevState, props) => {
      return {
        addMirrors: GA.modifyProperty(prevState.addMirrors, {
          addMirrorsError: errorMsg,
        })
      };
    });
    return;
  }

  let credId = NPECheck(this.state, 'addRepo/newRepo/repo/credId', null);
  if (credId == null) {
    this.setState((prevState, props) => {
      return {
        addMirrors: GA.modifyProperty(prevState.addMirrors, {
          addMirrorsError: 'No registry credentials specified',
        })
      };
    });
    return;
  }

  let postData = {
    repos: selectedRepos,
  };

  let params = {
    credId,
  };

  this.setState((prevState, props) => {
    return {
      addMirrors: GA.modifyProperty(prevState.addMirrors, {
        addMirrorsXHR: true,
      }),
    };
  }, () => {
    RAjax.POST.call(this, 'CreateRepoMirrorsBatch', postData, params)
      .then((res) => {
        this.setState((prevState, props) => {
          return {
            addMirrors: GA.modifyProperty(this.state.addMirrors, {
              addMirrorsXHR: false,
            }),
          };
        }, () => {
          listRepos.call(this);
          if (afterAddCb) {
            afterAddCb();
          }
        });
      })
      .catch((err) => {
        console.log(err);
        let errorMsg = `There was an error adding mirrors: ${err.error.message}`;
        this.setState((prevState, props) => {
          return {
            addMirrors: GA.modifyProperty(this.state.addMirrors, {
              addMirrorsXHR: false,
              addMirrorsError: errorMsg,
            })
          }
        });
      });
  });
}

export function getAddMirrorsSelectedRepoData() {
  let remoteRepos = NPECheck(this.state, 'addRepo/reposInRegistry', []);
  let configuredRepos = NPECheck(this.state, 'addMirrors/selectedRepos', {});
  let selectedRepos = [];
  remoteRepos.forEach(repoName => {
    let lastElement = repoName.split("/").pop();
    let repoData = {};
    if (configuredRepos.hasOwnProperty(repoName)) {
      if (!configuredRepos[repoName].hasOwnProperty("selected") || configuredRepos[repoName].selected) {
        repoData = {
          sourceRepoName: repoName,
          destinationRepoName: (configuredRepos[repoName].localRepoName || lastElement),
        };
        selectedRepos.push(repoData);
      }
    } else {
      repoData = {
        sourceRepoName: repoName,
        destinationRepoName: lastElement,
      };
      selectedRepos.push(repoData);
    }
  });
  return selectedRepos;
}

export function canAddMirrors() {
  let selectedRepos = getAddMirrorsSelectedRepoData.call(this);

  if (selectedRepos.length === 0) {
    return false;
  }
  let conflictingRepos = selectedRepos.filter(repoData => this.state.reposNameMap.hasOwnProperty(repoData.destinationRepoName));
  return (conflictingRepos.length === 0);
}

export function getAddMirrorsIsRepoSelected(repoName) {
  return getAddMirrorsRepoStateField(repoName, "selected", this.state, true);
}

export function getAddMirrorsLocalRepoName(repoName) {
  let repoNameLastComponent = repoName.split('/').pop();
  return getAddMirrorsRepoStateField(repoName, "localRepoName", this.state, repoNameLastComponent);
}

export function getAddMirrorsSelectorStatus() {
  let repos = NPECheck(this.state, 'addRepo/reposInRegistry', []);
  let all = true;
  let none = true;
  repos.forEach((repo) => {
    if (getAddMirrorsRepoStateField(repo, "selected", this.state, true)) {
      none = false;
    } else {
      all = false;
    }
  });
  if (all) {
    return getAddMirrorsSelectorOptions.call(this, "all");
  }
  if (none) {
    return getAddMirrorsSelectorOptions.call(this, "none");
  }
}

// We can't use NPECheck for this because the properties might have slashes in them.
function getAddMirrorsRepoStateField(repoName, fieldName, state, defaultValue) {
  let result = null;
  if (state.addMirrors.selectedRepos.hasOwnProperty(repoName)) {
    result = state.addMirrors.selectedRepos[repoName][fieldName];
  }
  if (result == null) {
    result = defaultValue;
  }
  return result;
}

export function getAddMirrorsSelectorOptions(choice = null) {
  let options = {
    all: {
      name: "All",
      action: setAddMirrorsAllSelected.bind(this),
    },
    none: {
      name: "None",
      action: setAddMirrorsNoneSelected.bind(this),
    },
  };
  if (choice != null) {
    return options[choice.toLowerCase()];
  }
  return Object.values(options);
}

export function updateAddMirrorsRepoStateField(repoName, fieldName, value, callback = null) {
  this.setState((prevState, props) => {
    let change = {};
    let repoState = (prevState.addMirrors.selectedRepos.hasOwnProperty(repoName))
      ? prevState.addMirrors.selectedRepos[repoName]
      : {};
    change[repoName] = {
      ...repoState,
    };
    change[repoName][fieldName] = value;
    let addMirrors = {
      selectedRepos: GA.modifyProperty(prevState.addMirrors.selectedRepos, change),
    };
    return {
      addMirrors: GA.modifyProperty(prevState.addMirrors, addMirrors),
    };
  }, callback);
}

export function toggleAddMirrorsRepoStateField(repoName, fieldName, defaultValue, callback = null) {
  this.setState((prevState, props) => {
    let change = {};
    let repoState = (prevState.addMirrors.selectedRepos.hasOwnProperty(repoName))
      ? prevState.addMirrors.selectedRepos[repoName]
      : {};
    change[repoName] = {
      ...repoState,
    };
    change[repoName][fieldName] = !getAddMirrorsRepoStateField(repoName, fieldName, prevState, defaultValue);
    let addMirrors = {
      selectedRepos: GA.modifyProperty(prevState.addMirrors.selectedRepos, change),
    };
    return {
      addMirrors: GA.modifyProperty(prevState.addMirrors, addMirrors),
    };
  }, callback);
}

export function updateAddMirrorsRepoStateFieldAllRepos(fieldName, value, callback = null) {
  this.setState((prevState, props) => {
    let repos = NPECheck(prevState, 'addRepo/reposInRegistry', []);
    let change = {};
    repos.forEach((repoName) => {
      let repoState = (prevState.addMirrors.selectedRepos.hasOwnProperty(repoName))
        ? prevState.addMirrors.selectedRepos[repoName]
        : {};
      change[repoName] = {
        ...repoState,
      };
      change[repoName][fieldName] = value;
    });
    let addMirrors = {
      selectedRepos: GA.modifyProperty(prevState.addMirrors.selectedRepos, change),
    };
    return {
      addMirrors: GA.modifyProperty(prevState.addMirrors, addMirrors),
    };
  }, callback);
}

function setAddMirrorsAllSelected() {
  updateAddMirrorsRepoStateFieldAllRepos.call(this, "selected", true);
}

function setAddMirrorsNoneSelected() {
  updateAddMirrorsRepoStateFieldAllRepos.call(this, "selected", false);
}

export function activateAddMirrorsSelector(option) {
  getAddMirrorsSelectorOptions.call(this).find((x) => x.name === option).action.call(this);
}

// *************************************************
// Repo Detail Actions
// *************************************************


export function repoDetailsState() {
  return {
    isBlocked: false,
    activeRepo: {},
    noRepo: false,
    repoOverviewContent: '',
    repoOverviewContentOriginal: '',
    isOverviewModified: false,
    saveRepoOverviewXHR: false,

    editOverview: false,
    repoOverviewError: '',

    pageXHR: false,
    deleteXHR: false,
    isDeleting: false,
    deleteRepoError: '',
    showSettings: false,
    timelineSection: 'OVERVIEW',

    events: [],
    eventsXHR: false,
    eventsError: '',
    hasRetrievedEvents: false,
    eventsPrevMarker: null,
    eventsNextMarker: null,
    activeEventId: null,

    manifests: [],
    manifestsXHR: false,
    manifestsError: '',
    hasRetrievedManifests: false,
    selectedManifests: [],
    showPullCommands: false,

    publicStatusChange: '',
    publicConfirm: false,
    publicXHR: false,
    publicError: '',
  };
}

export function resetRepoDetailsState() {
  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, repoDetailsState.call(this))
  });
}


export function clearRepoDetailsErrors() {
  this.setState((prevState, props) => {
    return {
      repoDetails: GA.modifyProperty(prevState.repoDetails, {
        repoOverviewError: '',
        eventsError: '',
        manifestsError: '',
        deleteRepoError: '',
        publicError: ''
      })
    }
  });
}

export function toggleRepoDetailsPageXHR(loading) {
  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      pageXHR: loading
    })
  });
}

export function setActiveRepoDetails(repoId) {
  return new Promise((resolve, reject) => {
    let newState = {}
    let repo = this.state.reposMap[repoId];

    if (!repo || !repo.id) {
      newState = {
        noRepo: true
      };
    } else {
      newState = {
        activeRepo: repo
      };
    }

    this.setState({
      repoDetails: GA.modifyProperty(this.state.repoDetails, newState)
    }, () => resolve());
  });
}

// Read Permissions
export function getRepoOverview(repoId) {
  return new Promise((resolve, reject) => {
    if (!repoId) repoId = NPECheck(this.state, 'repoDetails/activeRepo/id', '');

    this.setState({
      repoDetails: GA.modifyProperty(this.state.repoDetails, {})
    }, () => {
      RAjax.GET.call(this, 'GetRepoOverview', {
        repoId
      })
      .then((res) => {

        this.setState({
          repoDetails: GA.modifyProperty(this.state.repoDetails, {
            repoOverviewContent: res.content || '',
            repoOverviewContentOriginal: res.content || '',
            isOverviewModified: false
          })
        }, () => resolve());

      })
      .catch((err) => {
        console.error(err);
        let errorMsg = `${NPECheck(err, 'error/message', '')}`

        if (errorMsg === ErrorMessages.UNAUTHORIZED) {
          this.setState({
            repoDetails: GA.modifyProperty(this.state.repoDetails, {
              isBlocked: true,
              saveRepoOverviewXHR: false,
            })
          }, () => reject());
        } else {
          this.setState({
            repoDetails: GA.modifyProperty(this.state.repoDetails, {
              saveRepoOverviewXHR: false,
              repoOverviewError: errorMsg
            })
          }, () => reject());
        }

      });
    })
  });
}

export function updateRepoOverviewContent(e, eIsValue = false) {
  let value = (eIsValue) ? e : e.target.value;
  let ogValue = NPECheck(this.state, 'repoDetails/repoOverviewContentOriginal', '');

  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      repoOverviewContent: value,
      isOverviewModified: value != ogValue
    })
  });
}

export function toggleRepoOverviewEdit() {
  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      editOverview: !this.state.repoDetails.editOverview
    })
  });
}

// Modify Permissions
export function saveRepoOverview() {
  return new Promise((resolve, reject) => {
    this.setState({
      repoDetails: GA.modifyProperty(this.state.repoDetails, {
        saveRepoOverviewXHR: true
      })
    }, () => {
      let content = this.state.repoDetails.repoOverviewContent;
      let repoId = this.state.repoDetails.activeRepo.id;
      RAjax.POST.call(this, 'SaveRepoOverview', {
        content
      }, {
        repoId
      })
      .then((res) => {

        this.setState({
          repoDetails: GA.modifyProperty(this.state.repoDetails, {
            saveRepoOverviewXHR: false,
            editOverview: false
          })
        }, () => resolve());

      })
      .catch((err) => {
        console.error(err);
        let errorMsg = `${NPECheck(err, 'error/message', '')}`
        this.setState({
          repoDetails: GA.modifyProperty(this.state.repoDetails, {
            saveRepoOverviewXHR: false,
            repoOverviewError: errorMsg
          })
        }, () => reject());

      });
    });
  });
}

export function discardRepoOverviewChanges() {
  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      repoOverviewContent: this.state.repoDetails.repoOverviewContentOriginal,
      isOverviewModified: false,
      editOverview: false
    })
  });
}


export function toggleActiveRepoDelete() {
  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      isDeleting: !this.state.repoDetails.isDeleting
    })
  })
}

export function confirmPublicStatusChange(publicStatusChange) {
  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      publicConfirm: !NPECheck(this.state, 'repoDetails/publicConfirm', true),
      publicStatusChange
    })
  });
}

// Modify Permissions
export function setRepoPublic(isPublic) {
  return new Promise((resolve, reject) => {
    this.setState({
      repoDetails: GA.modifyProperty(this.state.repoDetails, {
        publicXHR: true
      })
    }, () => {

      let repoId = NPECheck(this.state, 'repoDetails/activeRepo/id', '');

      let params = {
        repoId,
        public: isPublic
      };

      RAjax.POST.call(this, 'SetRepoPublic', {}, params)
      .then((res) => {
        this.setState({
          repoDetails: GA.modifyProperty(this.state.repoDetails, {
            publicXHR: false,
            publicConfirm: false,
            publicStatusChange: null
          })
        }, () => resolve());

      })
      .catch((err) => {
        let errorMsg = `${NPECheck(err, 'error/message', '')}`
        this.setState({
          repoDetails: GA.modifyProperty(this.state.repoDetails, {
            publicXHR: false,
            publicError: errorMsg,
            publicConfirm: false,
            publicStatusChange: null
          })
        }, () => reject());
      });

    });
  });
}

// Delete Permissions
export function deleteActiveRepo(afterDeleteCb) {
  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      deleteXHR: true
    })
  }, () => {
    RAjax.POST.call(this, 'DeleteContainerRepo', {}, {
      id: this.state.repoDetails.activeRepo.id
    })
    .then((res) => {
      this.setState({
        repoDetails: GA.modifyProperty(this.state.repoDetails, {
          deleteXHR: false
        })
      }, () => {
        if (afterDeleteCb) afterDeleteCb();
      });
    })
    .catch((err) => {
      console.error(err);
      let errorMsg = `${NPECheck(err, 'error/message', '')}`
      this.setState({
        repoDetails: GA.modifyProperty(this.state.repoDetails, {
          deleteXHR: false,
          deleteRepoError: errorMsg
        })
      });
    });
  });
}

export function toggleActiveRepoSettings() {
  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      showSettings: !this.state.repoDetails.showSettings
    })
  })
}

export function setTimelineSection(section = '') {
  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      timelineSection: section
    })
  });
}

// Read Permissions
export function listRepoEvents(repoId, skipXHR, marker, isBackward = null) {
  return new Promise((resolve, reject) => {
    this.setState({
      repoDetails: GA.modifyProperty(this.state.repoDetails, {
        eventsXHR: (skipXHR) ? false : true
      })
    }, () => {

      let params = {
        repoId,
      };

      if (marker) {
        params.marker = marker;
      }

      if (isBackward) {
        params.backward = 'true';
      }

      RAjax.GET.call(this, 'ListRepoEvents', params)
      .then((res) => {
        this.setState({
          repoDetails: GA.modifyProperty(this.state.repoDetails, {
            events: res.events,
            eventsPrevMarker: res.prevMarker,
            eventsNextMarker: res.nextMarker,
            eventsXHR: false,
            hasRetrievedEvents: true
          })
        }, () => resolve());
      })
      .catch((err) => {
        console.error(err);
        let errorMsg = `${NPECheck(err, 'error/message', '')}`
        if (errorMsg === ErrorMessages.UNAUTHORIZED) {
          this.setState({
            repoDetails: GA.modifyProperty(this.state.repoDetails, {
              isBlocked: true,
              eventsXHR: false
            })
          }, () => reject());
        } else {
          this.setState({
            repoDetails: GA.modifyProperty(this.state.repoDetails, {
              eventsXHR: false,
              eventsError: errorMsg
            })
          }, () => reject());
        }
      })
    })
  });
}

export function paginateEventsForward() {
  let repoId = NPECheck(this.state, 'repoDetails/activeRepo/id', null);
  listRepoEvents.call(this, repoId, true, this.state.repoDetails.eventsNextMarker);
}

export function paginateEventsBackward() {
  let repoId = NPECheck(this.state, 'repoDetails/activeRepo/id', null);
  listRepoEvents.call(this, repoId, true, this.state.repoDetails.eventsPrevMarker, true);
}

export function toggleEventDetails(eventId = null) {
  return new Promise((resolve, reject) => {
    this.setState({
      repoDetails: GA.modifyProperty(this.state.repoDetails, {
        activeEventId: (this.state.repoDetails.activeEventId == eventId) ? null : eventId
      })
    }, () => resolve(!!this.state.repoDetails.activeEventId));
  });
}

// *************************************************
// Tag Actions
// *************************************************

// Read Permissions
export function listRepoManifests(repoId, skipXHR, marker, isBackward = null) {
  return new Promise((resolve, reject) => {
    this.setState({
      repoDetails: GA.modifyProperty(this.state.repoDetails, {
        manifestsXHR: (skipXHR) ? false : true
      })
    }, () => {

      let params = {
        repoId
      };

      if (marker) {
        params.marker = marker;
      }

      if (isBackward) {
        params.backward = 'true';
      }

      RAjax.GET.call(this, 'ListRepoManifests', params)
      .then((res) => {
        this.setState({
          repoDetails: GA.modifyProperty(this.state.repoDetails, {
            manifests: res.list,
            manifestsPrevMarker: res.prev,
            manifestsNextMarker: res.next,
            manifestsXHR: false,
            manifestsError: '',
            hasRetrievedManifests: true
          })
        }, () => resolve())
      })
      .catch((err) => {
        console.error(err);
        let errorMsg = NPECheck(err, 'error/message', '');
        if (errorMsg === ErrorMessages.UNAUTHORIZED) {
          this.setState({
            repoDetails: GA.modifyProperty(this.state.repoDetails, {
              isBlocked: true,
              manifestsXHR: false,
            })
          }, () => reject());
        } else {
          this.setState({
            repoDetails: GA.modifyProperty(this.state.repoDetails, {
              manifestsXHR: false,
              manifestsError: errorMsg
            })
          }, () => reject())
        }

      });
    });
  });
}

export function paginateManifestsForward() {
  let repoId = NPECheck(this.state, 'repoDetails/activeRepo/id', null);
  listRepoManifests.call(this, repoId, true, this.state.repoDetails.manifestsNextMarker);
}

export function paginateManifestsBackward() {
  let repoId = NPECheck(this.state, 'repoDetails/activeRepo/id', null);
  listRepoManifests.call(this, repoId, true, this.state.repoDetails.manifestsPrevMarker, true);
}

export function toggleSelectedManifest(manifest = null) {
  let selectedManifests = [...NPECheck(this.state, 'repoDetails/selectedManifests', [])];

  if (selectedManifests.includes(manifest)) {
    selectedManifests = selectedManifests.filter((manifestTest) => manifestTest.manifestId != manifest.manifestId);
  } else {
    selectedManifests = [...selectedManifests, manifest];
  }

  this.setState({
    repoDetails: GA.modifyProperty(this.state.repoDetails, {
      selectedManifests
    })
  });
}

export function toggleShowPullCommands() {
  return new Promise((resolve) => {
    this.setState({
      repoDetails: GA.modifyProperty(this.state.repoDetails, {
        showPullCommands: !this.state.repoDetails.showPullCommands
      })
    }, () => resolve());
  });
}
