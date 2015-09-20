/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.eclipse.cft.server.core.internal.client.AbstractWaitWithProgressJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class CloudFoundryLoginHandler {

	private final CloudFoundryOperations operations;

	private static final String DEFAULT_PROGRESS_LABEL = Messages.CloudFoundryLoginHandler_LABEL_PERFORM_CF_OPERATION;

	private static final int DEFAULT_PROGRESS_TICKS = 100;

	/**
	 * 
	 * @param operations must not be null
	 * @param cloudServer can be null if no server has been created yet
	 */
	public CloudFoundryLoginHandler(CloudFoundryOperations operations) {
		this.operations = operations;
	}

	/**
	 * Attempts to log in once. If login fails, Core exception is thrown
	 * @throws CoreException if login failed. The reason for the login failure
	 * is contained in the core exception's
	 */
	public OAuth2AccessToken login(IProgressMonitor monitor) throws CoreException {
		return login(monitor, 1, 0);
	}

	/**
	 * Attempts a log in for the specified amount of attempts, and waits by the
	 * specified sleep time between each attempt. If at the end of the attempts,
	 * login has failed, Core exception is thrown.
	 */
	public OAuth2AccessToken login(IProgressMonitor monitor, int tries, long sleep) throws CoreException {
		return internalLogin(monitor, tries, sleep);
	}

	protected OAuth2AccessToken internalLogin(IProgressMonitor monitor, int tries, long sleep) throws CoreException {
		return new AbstractWaitWithProgressJob<OAuth2AccessToken>(tries, sleep) {

			@Override
			protected OAuth2AccessToken runInWait(IProgressMonitor monitor) throws CoreException {
				// Do not wrap CloudFoundryException or RestClientException in a
				// CoreException.
				// as they are uncaught exceptions and can be inspected directly
				// by the shouldRetryOnError(..) method.
				return operations.login();
			}

			@Override
			protected boolean shouldRetryOnError(Throwable t) {
				return shouldAttemptClientLogin(t);
			}

		}.run(monitor);
	}

	protected SubMonitor getProgressMonitor(IProgressMonitor progressMonitor) {
		return progressMonitor instanceof SubMonitor ? (SubMonitor) progressMonitor : SubMonitor.convert(
				progressMonitor, DEFAULT_PROGRESS_LABEL, DEFAULT_PROGRESS_TICKS);
	}

	public boolean shouldAttemptClientLogin(Throwable t) {
		return CloudErrorUtil.getInvalidCredentialsError(t) != null;
	}

	/**
	 * 
	 * @return true if there was a proxy update. False any other case.
	 * @throws CoreException
	 */
	public boolean updateProxyInClient(CloudFoundryOperations client) throws CoreException {
		// if (client != null && cloudURL != null) {
		// try {
		// URL actualUrl = new URL(cloudURL);
		// HttpProxyConfiguration proxyConfiguration =
		// CloudFoundryClientFactory.getProxy(actualUrl);
		// // FIXNS: As of CF Java client-lib version 1.0.2, update proxy
		// // API has been removed. Therefore unless a new client
		// // is created on proxy change, or the client indirectly detects
		// // proxy changes via system properties
		// // Proxy support for CF Eclipse will not work unless a user
		// // reconnects the server instance when the client
		// // is created.
		// client.updateHttpProxyConfiguration(proxyConfiguration);
		//
		// return true;
		// }
		// catch (MalformedURLException e) {
		// throw
		// CloudErrorUtil.toCoreException("Failed to update proxy settings due to "
		// + e.getMessage(), e);
		// }
		// }
		return false;
	}

}
