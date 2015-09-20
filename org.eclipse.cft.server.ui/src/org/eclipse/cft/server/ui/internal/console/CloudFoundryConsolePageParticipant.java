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
package org.eclipse.cft.server.ui.internal.console;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryConsolePageParticipant implements IConsolePageParticipant {

	public void activated() {
		// ignore
	}

	public void deactivated() {
		// ignore
	}

	public void dispose() {
		// ignore
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public void init(IPageBookViewPage page, IConsole console) {
		if (isCloudFoundryConsole(console)) {
			CloseConsoleAction closeAction = new CloseConsoleAction(console);
			closeAction.setImageDescriptor(CloudFoundryImages.CLOSE_CONSOLE);
			IToolBarManager manager = page.getSite().getActionBars().getToolBarManager();
			manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);
		}
	}

	protected boolean isCloudFoundryConsole(IConsole console) {
		if (console instanceof MessageConsole) {
			MessageConsole messageConsole = (MessageConsole) console;
			Object cfServerObj = messageConsole.getAttribute(ApplicationLogConsole.ATTRIBUTE_SERVER);
			Object cfAppModuleObj = messageConsole.getAttribute(ApplicationLogConsole.ATTRIBUTE_APP);
			if (cfServerObj instanceof CloudFoundryServer && cfAppModuleObj instanceof CloudFoundryApplicationModule) {
				CloudFoundryServer cfServer = (CloudFoundryServer) cfServerObj;
				CloudFoundryApplicationModule appModule = (CloudFoundryApplicationModule) cfAppModuleObj;

				CloudConsoleManager manager = ConsoleManagerRegistry.getConsoleManager(cfServer);
				if (manager != null) {
					MessageConsole existingConsole = manager.findCloudFoundryConsole(cfServer.getServer(), appModule);
					return messageConsole == existingConsole;
				}
			}
		}
		return false;
	}
}
