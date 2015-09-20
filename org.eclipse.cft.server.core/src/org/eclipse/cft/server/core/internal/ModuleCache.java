/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.ServerCore;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Manages the cloud state of the modules in the form of {@link ServerData}.
 * This can not be managed in the server or behavior delegate since those get
 * disposed every time a working copy is saved. The module cache may be accessed
 * by multiple threads therefore multi-threaded access needs to be taken into
 * account when modifying server data state.
 * @author Steffen Pingel
 */
public class ModuleCache {

	public static class ServerData {

		private final List<CloudFoundryApplicationModule> cloudModules = new ArrayList<CloudFoundryApplicationModule>();

		/** Cached password in case secure store fails. */
		private String password;

		private IServer server;

		/**
		 * Modules added in this session.
		 */
		private final List<IModule> undeployedModules = new ArrayList<IModule>();

		private final Map<String, CloudFoundryApplicationModule> mapProject = new HashMap<String, CloudFoundryApplicationModule>();

		private int[] applicationMemoryChoices;

		ServerData(IServer server) {
			this.server = server;
		}

		public synchronized void clear() {
			cloudModules.clear();
		}

		/**
		 * 
		 * @param application
		 * @return Non-null new {@link CloudFoundryApplicationModule}
		 */
		public synchronized CloudFoundryApplicationModule createModule(CloudApplication application) {
			CloudFoundryApplicationModule appModule = new CloudFoundryApplicationModule(application.getName(), server);
			appModule.setCloudApplication(application);
			add(appModule);
			return appModule;
		}

		/**
		 * Updates the cache of local module -> cloud module mapping. This is
		 * used when the deployed name changes (e.g. a user specifies a
		 * different deployment name than the local module name that typically
		 * matches the workspace project name for the app, if the project is
		 * accessible).
		 * @param module whose mapping to a local module needs to be updated and
		 * persisted.
		 */
		public synchronized void updateCloudApplicationModule(CloudFoundryApplicationModule module) {
			// Update the map of module ID -> Deployed Application name
			if (module.getLocalModule() != null) {
				Map<String, String> mapping = getLocalModuleToCloudModuleMapping();
				mapping.put(module.getLocalModule().getId(), module.getDeployedApplicationName());
				setLocalModuleToCloudModuleMapping(mapping);
			}
		}

		/**
		 * 
		 * @return never null. May be empty
		 */
		public synchronized Collection<CloudFoundryApplicationModule> getExistingCloudModules() {
			return new ArrayList<CloudFoundryApplicationModule>(cloudModules);
		}

		public synchronized String getPassword() {
			return password;
		}

		public synchronized boolean isUndeployed(IModule module) {
			return undeployedModules.contains(module);
		}

		public synchronized void remove(CloudFoundryApplicationModule module) {
			if (module == null) {
				return;
			}
			cloudModules.remove(module);
			if (module.getLocalModule() != null) {
				Map<String, String> mapping = getLocalModuleToCloudModuleMapping();
				mapping.remove(module.getLocalModule().getId());
				setLocalModuleToCloudModuleMapping(mapping);
			}
		}

		public synchronized void removeObsoleteModules(Set<CloudFoundryApplicationModule> allModules) {
			HashSet<CloudFoundryApplicationModule> deletedModules = new HashSet<CloudFoundryApplicationModule>(
					cloudModules);
			deletedModules.removeAll(allModules);
			if (deletedModules.size() > 0) {
				Map<String, String> mapping = getLocalModuleToCloudModuleMapping();
				boolean mappingModified = false;
				for (CloudFoundryApplicationModule deletedModule : deletedModules) {
					if (deletedModule.getLocalModule() != null) {
						mappingModified |= mapping.remove(deletedModule.getLocalModule().getId()) != null;
					}
				}
				if (mappingModified) {
					setLocalModuleToCloudModuleMapping(mapping);
				}
			}
		}

		public synchronized void setPassword(String password) {
			this.password = password;
		}

		public synchronized void tagAsDeployed(IModule module) {
			undeployedModules.remove(module);
		}

		public synchronized void tagAsUndeployed(IModule module) {
			undeployedModules.add(module);
		}

		public synchronized void tagForReplace(CloudFoundryApplicationModule appModule) {
			if (appModule != null) {
				mapProject.put(appModule.getDeployedApplicationName(), appModule);
			}
		}

		public synchronized void untagForReplace(CloudFoundryApplicationModule appModule) {
			if (appModule != null) {
				mapProject.remove(appModule.getDeployedApplicationName());
			}
		}

		public synchronized CloudFoundryApplicationModule getTaggedForReplace(CloudFoundryApplicationModule appModule) {
			return appModule != null ? mapProject.get(appModule.getDeployedApplicationName()) : null;
		}

		private void add(CloudFoundryApplicationModule module) {
			cloudModules.add(module);
		}

		private String convertMapToString(Map<String, String> map) {
			if (map == null) {
				return ""; //$NON-NLS-1$
			}
			StringBuilder result = new StringBuilder();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				result.append(entry.getKey());
				result.append(","); //$NON-NLS-1$
				result.append(entry.getValue());
				result.append(","); //$NON-NLS-1$
			}
			return result.toString();
		}

		private Map<String, String> convertStringToMap(String str) {
			if (str == null) {
				return new HashMap<String, String>();
			}
			Map<String, String> result = new HashMap<String, String>();
			String[] tokens = str.split(","); //$NON-NLS-1$
			for (int i = 0; i < tokens.length - 1; i += 2) {
				result.put(tokens[i], tokens[i + 1]);
			}
			return result;
		}

		/**
		 * Local modules are mapped to deployed applications, represented by
		 * cloud modules, by mapping the local module ID (typically, the module
		 * type + local module name) to the deployed application name.
		 * @return map containing local module ID (key) to deployed cloud
		 * application name (value)
		 */
		private Map<String, String> getLocalModuleToCloudModuleMapping() {
			IEclipsePreferences node = new InstanceScope().getNode(CloudFoundryPlugin.PLUGIN_ID);
			String string = node.get(KEY_MODULE_MAPPING_LIST + ":" + getServerId(), ""); //$NON-NLS-1$ //$NON-NLS-2$
			return convertStringToMap(string);
		}

		private CloudFoundryApplicationModule getCloudModuleByDeployedAppName(String deployedApplicationName) {
			for (CloudFoundryApplicationModule module : cloudModules) {
				if (deployedApplicationName.equals(module.getDeployedApplicationName())) {
					return module;
				}
			}
			return null;
		}

		/**
		 * A {@link CloudFoundryApplicationModule} is a Cloud Foundry-aware
		 * module representing a deployed application. If it exists, it means
		 * that the application is currently or has been already processed by
		 * the CF plugin. If it does not exist (its null), it means it still
		 * needs to be created separately.
		 * @param localName must be the local module name. In some cases it may
		 * be the same as the deployed application name (in case the module is
		 * external and has not corresponding accessible workspace project), but
		 * they may be different as well, in case the local name (i.e. the
		 * project name) differs from the user-defined deployed name.
		 * @return
		 */
		private CloudFoundryApplicationModule getCloudModuleToLocalModuleName(String localName) {
			for (CloudFoundryApplicationModule module : cloudModules) {
				if (localName.equals(module.getName())) {
					return module;
				}
			}
			return null;
		}

		private String getServerId() {
			return server.getAttribute(CloudFoundryServer.PROP_SERVER_ID, (String) null);
		}

		private void setLocalModuleToCloudModuleMapping(Map<String, String> list) {
			String string = convertMapToString(list);
			IEclipsePreferences node = new InstanceScope().getNode(CloudFoundryPlugin.PLUGIN_ID);
			CloudFoundryPlugin.trace("Updated mapping: " + string); //$NON-NLS-1$
			node.put(KEY_MODULE_MAPPING_LIST + ":" + getServerId(), string); //$NON-NLS-1$
			try {
				node.flush();
			}
			catch (BackingStoreException e) {
				CloudFoundryPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
								"Failed to update application mappings", e)); //$NON-NLS-1$
			}
		}

		synchronized CloudFoundryApplicationModule getExistingCloudModule(IModule module) {
			if (module == null) {
				return null;
			}
			// See if the cloud module for the given local IModule has been
			// created.
			CloudFoundryApplicationModule appModule = getCloudModuleToLocalModuleName(module.getName());
			if (appModule != null) {
				return appModule;
			}

			// Otherwise check if there is a mapping between the IModule ID and
			// the deployed application name, and
			// search for a cloud module that matches the deployed application
			// name
			String deployedAppName = getLocalModuleToCloudModuleMapping().get(module.getId());
			if (deployedAppName != null) {
				appModule = getCloudModuleByDeployedAppName(deployedAppName);
				if (appModule != null) {
					return appModule;
				}
				// If not available, it means it needs to be created below.
			}
			return null;
		}

		synchronized CloudFoundryApplicationModule getOrCreateCloudModule(IModule module) {

			// See if the cloud module for the given local IModule has been
			// created.
			CloudFoundryApplicationModule appModule = getExistingCloudModule(module);
			if (appModule != null) {
				return appModule;
			}

			// Otherwise check if there is a mapping between the IModule ID and
			// the deployed application name, and
			// search for a cloud module that matches the deployed application
			// name
			String deployedAppName = getLocalModuleToCloudModuleMapping().get(module.getId());
			if (deployedAppName == null) {
				deployedAppName = module.getName();
			}

			// no mapping found, create new Cloud Foundry-aware module. Note
			// that the
			// deployedAppName and the module name need not be the same.
			appModule = new CloudFoundryApplicationModule(module, deployedAppName, server);

			add(appModule);
			return appModule;
		}

		void updateServerId(String oldServerId, String newServerId) {
			IEclipsePreferences node = new InstanceScope().getNode(CloudFoundryPlugin.PLUGIN_ID);
			String string = node.get(KEY_MODULE_MAPPING_LIST + ":" + oldServerId, ""); //$NON-NLS-1$ //$NON-NLS-2$
			node.remove(KEY_MODULE_MAPPING_LIST + ":" + oldServerId); //$NON-NLS-1$
			node.put(KEY_MODULE_MAPPING_LIST + ":" + newServerId, string); //$NON-NLS-1$
		}

		public synchronized void setApplicationMemoryChoices(int[] applicationMemoryChoices) {
			this.applicationMemoryChoices = applicationMemoryChoices;
		}

		public synchronized int[] getApplicationMemoryChoices() {
			return applicationMemoryChoices;
		}
	}

	/**
	 * List of appName, module id pairs.
	 */
	static final String KEY_MODULE_MAPPING_LIST = "org.eclipse.cft.moduleMapping"; //$NON-NLS-1$

	private Map<IServer, ServerData> dataByServer;

	private IServerLifecycleListener listener = new IServerLifecycleListener() {

		public void serverAdded(IServer server) {
			// ignore
		}

		public void serverChanged(IServer server) {
			// ignore

		}

		public void serverRemoved(IServer server) {
			remove(server);
		}
	};

	public ModuleCache() {
		dataByServer = new HashMap<IServer, ServerData>();
		ServerCore.addServerLifecycleListener(listener);
	}

	public void dispose() {
		ServerCore.removeServerLifecycleListener(listener);
	}

	public synchronized ServerData getData(IServer server) {
		ServerData data = dataByServer.get(server);
		if (data == null && server != null) {
			data = new ServerData(server);
			dataByServer.put(server, data);
		}
		return data;
	}

	protected synchronized void remove(IServer server) {
		dataByServer.remove(server);

		String serverId = server.getAttribute(CloudFoundryServer.PROP_SERVER_ID, (String) null);
		if (serverId != null) {
			IEclipsePreferences node = new InstanceScope().getNode(CloudFoundryPlugin.PLUGIN_ID);
			node.remove(KEY_MODULE_MAPPING_LIST + ":" + serverId); //$NON-NLS-1$
			try {
				node.flush();
			}
			catch (BackingStoreException e) {
				CloudFoundryPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
								"Failed to remove application mappings", e)); //$NON-NLS-1$
			}
		}
	}

}
