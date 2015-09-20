/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.ui.internal.wizards;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.ValidationEvents;
import org.eclipse.cft.server.ui.internal.CloudSpacesDelegate;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.ServerWizardValidator;
import org.eclipse.cft.server.ui.internal.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

/**
 * Validates an existing server that is being cloned. It will check the existing
 * server's credentials to ensure they are valid and can be used for remote
 * server authorisation, which is needed in order to fetch an updated list of
 * cloud spaces.
 */
public class CloneServerWizardValidator extends ServerWizardValidator {

	public CloneServerWizardValidator(CloudFoundryServer cloudServer, CloudSpacesDelegate cloudServerSpaceDelegate) {
		super(cloudServer, cloudServerSpaceDelegate);
	}

	@Override
	protected ValidationStatus validateLocally() {
		String userName = getCloudFoundryServer().getUsername();
		String password = getCloudFoundryServer().getPassword();
		String url = getCloudFoundryServer().getUrl();
		String message = null;

		boolean valuesFilled = false;
		int validationEventType = ValidationEvents.VALIDATION;

		if (userName == null || userName.trim().length() == 0) {
			message = Messages.ERROR_NO_USERNAME_SPACES;
		}
		else if (password == null || password.trim().length() == 0) {
			message = Messages.ERROR_NO_PASSWORD_SPACES;
		}
		else if (url == null || url.trim().length() == 0) {
			message = Messages.ERROR_NO_URL_SPACES;
		}
		else {
			valuesFilled = true;
			message = Messages.CLONE_SERVER_WIZARD_OK_MESSAGE;
		}

		int statusType = valuesFilled ? IStatus.OK : IStatus.ERROR;

		IStatus status = CloudFoundryPlugin.getStatus(message, statusType);

		return new ValidationStatus(status, validationEventType);

	}

}
