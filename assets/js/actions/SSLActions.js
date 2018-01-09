import * as GA from './../reducers/GeneralReducers'
import * as RAjax from './../util/RAjax'
import NPECheck from './../util/NPECheck'
import Debounce from './../util/Debounce'
import Validate from './../util/Validate'

// *************************************************
// General SSL Actions
// *************************************************

export function sslState() {
	return {
		sslEnabled: false,
		ogSslEnabled: false,
		hasChanges: false,
		sslSettings: {
			dnsName: '',
			serverPrivateKey: '',
			serverCertificate: '',
			authorityCertificate: '',
			forceHttps: false,
		},
		errorFields: {
			names: [],
			keys: []
		},
		getXHR: false,
		getError: '',
		saveSuccess: false,
		saveXHR: false,
		saveError: '',
		isBlocked: false
	};
}

export function resetUsersState() {
	this.setState({
		ssl: usersState()
	});
}

export function clearSSLErrors() {
	this.setState({
		ssl: GA.modifyProperty(this.state.ssl, {
			saveError: '',
			errorFields: {}
		})
	});
}


export function toggleEnableSSL() {
	this.setState({
		ssl: GA.modifyProperty(this.state.ssl, {
			saveSuccess: false,
			sslEnabled: !this.state.ssl.sslEnabled
		})
	});
}

export function toggleForceHttps() {
	this.setState({
		ssl: GA.modifyProperty(this.state.ssl, {
			saveSuccess: false,
			hasChanges: true,
			sslSettings: {
				...this.state.ssl.sslSettings,
				['forceHttps']: !this.state.ssl.sslSettings.forceHttps
			}
		})
	});
}

export function updateSSLSettings(prop, e, eIsValue = false) {
	let value = (eIsValue) ? e : e.target.value;
	let newState = {
		ssl: GA.modifyProperty(this.state.ssl, {
			saveSuccess: false,
			hasChanges: true,
			sslSettings: {
				...this.state.ssl.sslSettings,
				[prop]: value
			}
		})
	};

	this.setState(newState);
}


export function saveSSLSettings() {
	return new Promise((resolve, reject) => {

		let creds = this.state.ssl.sslSettings
		let isEnabled = NPECheck(this.state, 'ssl/sslEnabled', false)

		if (isEnabled) {
			let isValid = isSSLValid(creds);

			if (isValid.names.length) {
				this.setState({
					ssl: GA.modifyProperty(this.state.ssl, {
						errorFields: isValid
					})
				}, () => {
					reject();
				});
				return
			}

		} else {
			creds = {
				dnsName: NPECheck(this.state, 'ssl/sslSettings/dnsName'),
				forceHttps: NPECheck(this.state, 'ssl/sslSettings/forceHttps')
			}
		}

		this.setState({
			ssl: GA.modifyProperty(this.state.ssl, {
				saveXHR: true
			})
		}, () => {

			RAjax.POST.call(this, 'SaveSslSettings', creds)
				.then((res) => {
					this.setState({
						ssl: GA.modifyProperty(this.state.ssl, {
							saveXHR: false,
							saveSuccess: true,
						})
					}, () => resolve(res));

				})
				.catch((err) => {
					console.error(err);
					let errorMsg = NPECheck(err, 'error/message', 'There was an error saving your SSL settings.');

					this.setState({
						ssl: GA.modifyProperty(this.state.ssl, {
							saveXHR: false,
							saveError: errorMsg,
							saveSuccess: false,
						})
					}, () => reject(err));

				});
		});
	});
}

export function getSSLSettings() {
	return new Promise((resolve, reject) => {
		this.setState({
			ssl: GA.modifyProperty(this.state.ssl, {
				getXHR: true
			})
		}, () => {
			RAjax.GET.call(this, 'GetSslSettings')
				.then((res) => {
					let sslEnabled = isSSLEnabled(res);
					this.setState({
						ssl: GA.modifyProperty(this.state.ssl, {
							getXHR: false,
							sslSettings: res,
							sslEnabled: sslEnabled,
							ogSslEnabled: sslEnabled,
							hasChanges: false,
							isBlocked: false,
						})
					}, () => resolve(res));
				})
				.catch((err) => {
					console.error(err);
					let errorMsg = NPECheck(err, 'error/message', 'There was an error retreiving your SSL settings.');
					if (errorMsg == 'You do not have access to this operation') {
						this.setState({
							ssl: GA.modifyProperty(this.state.ssl, {
								getXHR: false,
								getError: errorMsg,
								isBlocked: true
							})
						}, () => reject(err));
					} else {
						this.setState({
							ssl: GA.modifyProperty(this.state.ssl, {
								getXHR: false,
								getError: errorMsg,
								isBlocked: false,
							})
						}, () => reject(err));
					}
				});
		});
	});
}

export function isSSLValid(sslSettings) {
	let required = {
		dnsName: 'DNS Name',
		serverPrivateKey: 'Server Private Key',
		serverCertificate: 'Server Certificate',
		authorityCertificate: 'Authority Certificate',
		forceHttps: 'Force HTTPS',
	};

	let isValid = Validate(sslSettings, required);

	return isValid;
}

function isSSLEnabled(sslSettings) {
	let s = sslSettings;
	return !!(s.serverPrivateKey && s.serverCertificate && s.authorityCertificate)
}
