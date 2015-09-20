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
package org.eclipse.cft.server.ui.internal.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.menus.IMenuService;

/**
 * Contributes debug context menu actions to the Server view based on valid
 * context selections. This is invoked any time a user right-clicks on the
 * Servers view, and therefore context selection checks should be fast.
 * 
 */
public class CloudFoundryActionContributionFactory extends AbstractMenuContributionFactory {

	private static final MenuActionHandler<?>[] ACTION_HANDLERS = new MenuActionHandler<?>[] {
			
		// TODO: Debug actions disabled until debug support is reenabled in future, post 1.5.0 MicroCloud Foundry.
	// new DebugMenuActionHandler(),
		
		// FIXNS: Disable Caldecott feature in 1.5.1 until feature is supported
		// in the client-lib
//		new ServerMenuActionHandler() 
		
	};

	public CloudFoundryActionContributionFactory() {
		super("popup:org.eclipse.wst.server.ui.ServersView?before=additions", null); //$NON-NLS-1$
	}

	@Override
	protected List<IAction> getActions(IMenuService menuService) {

		// Return all context menu actions that are applicable to the given
		// context
		Object context = menuService.getCurrentState();
		List<IAction> actions = new ArrayList<IAction>();
		for (MenuActionHandler<?> actionHandler : ACTION_HANDLERS) {
			List<IAction> handlerActions = actionHandler.getActions(context);
			if (handlerActions != null && !handlerActions.isEmpty()) {
				actions.addAll(handlerActions);
			}
		}
		return actions;
	}

}
