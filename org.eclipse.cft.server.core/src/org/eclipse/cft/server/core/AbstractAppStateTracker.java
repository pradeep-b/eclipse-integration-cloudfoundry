/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
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
 *     IBM - initial API and implementation
 ********************************************************************************/

package org.eclipse.cft.server.core;

import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;

/**
 * App state tracker abstract class that all app state tracker should extends.
 * TODO: Move this class to public package when public API packages are defined.
 * @author eyuen
 */
public abstract class AbstractAppStateTracker {
	
	protected IServer server;
	
	/**
	 * Get the current application state
	 * @param appModule the application to check the state on
	 * @return the application state. The state is expected to be server state constants that are defined in
	 * org.eclipse.wst.server.core.IServer, e.g. IServer.STATE_STARTED.
	 */
	public abstract int getApplicationState(ICloudFoundryApplicationModule appModule);

	/**
	 * initialize the server object for tracking purpose
	 * @param curServer the server to be tracked.
	 */
	public void setServer(IServer curServer) {
		server = curServer;
	}

	/**
	 * Start tracking the given application module and this can also be used to start the initialization
	 * of the tracking, e.g. start monitoring the console output.
	 * @param appModule The application module to be tracked
	 */
	public abstract void startTracking(CloudFoundryApplicationModule appModule);

	/**
	 * Stop tracking the given application module and this can also be used to clean up
	 * the tracking, e.g. stop monitoring the console output.
	 * @param appModule The application module to be tracked
	 */
	public abstract void stopTracking(CloudFoundryApplicationModule appModule);

	/**
	 * Start tracking the given application module and this can also be used to start the initialization.
	 * Intended to be overridden.  If not overridden, then calls original implemented startTracking(CloudFoundryApplicationModule appModule).  
	 * Overrides should not have to call super.
	 * of the tracking, e.g. start monitoring the console output.
	 * @param appModule The application module to be tracked
	 * @param monitor The progress monitor to allow for canceling tracking
	 */
	public void startTracking(CloudFoundryApplicationModule appModule, IProgressMonitor monitor) {
		startTracking(appModule);
	}

	/**
	 * Stop tracking the given application module and this can also be used to clean up
 	 * Intended to be overridden.  If not overridden, then calls original implemented stopTracking(CloudFoundryApplicationModule appModule).  
	 * Overrides should not have to call super.
	 * the tracking, e.g. stop monitoring the console output.
	 * @param appModule The application module to be tracked
	 * @param monitor The progress monitor to allow for canceling tracking
	 */
	public void stopTracking(CloudFoundryApplicationModule appModule, IProgressMonitor monitor) {
		stopTracking(appModule);
	}
}
