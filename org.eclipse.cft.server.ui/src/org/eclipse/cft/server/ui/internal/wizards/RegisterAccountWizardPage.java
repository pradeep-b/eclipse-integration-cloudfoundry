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
package org.eclipse.cft.server.ui.internal.wizards;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * @author Steffen Pingel
 */
public class RegisterAccountWizardPage extends WizardPage {

	private CloudFoundryServer cloudServer;

	private Text emailText;

	private Text passwordText;

	private Text verifyPasswordText;

	protected RegisterAccountWizardPage(CloudFoundryServer cloudServer) {
		super(Messages.RegisterAccountWizardPage_TEXT_REGISTER_ACC);
		this.cloudServer = cloudServer;
		setTitle(Messages.RegisterAccountWizardPage_TITLE_REGISTER_ACC);
		setDescription(NLS.bind(Messages.RegisterAccountWizardPage_TEXT_SIGNUP, cloudServer.getUrl()));
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label emailLabel = new Label(composite, SWT.NONE);
		emailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		emailLabel.setText(Messages.COMMONTXT_EMAIL_WITH_COLON);

		emailText = new Text(composite, SWT.BORDER);
		emailText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		emailText.setEditable(true);
		emailText.setFocus();
		if (cloudServer.getUsername() != null) {
			emailText.setText(cloudServer.getUsername());
		}
		emailText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				update();
			}
		});

		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		passwordLabel.setText(Messages.COMMONTXT_PW);

		passwordText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		passwordText.setEditable(true);
		if (cloudServer.getPassword() != null) {
			passwordText.setText(cloudServer.getPassword());
		}
		passwordText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				update();
			}
		});

		Label verifyPasswordLabel = new Label(composite, SWT.NONE);
		verifyPasswordLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		verifyPasswordLabel.setText(Messages.RegisterAccountWizardPage_LABEL_VERIFY);

		verifyPasswordText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		verifyPasswordText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		verifyPasswordText.setEditable(true);
//		if (cloudServer.getPassword() != null) {
//			verifyPasswordText.setText(cloudServer.getPassword());
//		}
		verifyPasswordText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				update();
			}
		});

		update();
		setControl(composite);
	}

	public String getEmail() {
		return emailText.getText();
	}

	public String getPassword() {
		return passwordText.getText();
	}

	private void update() {
		String errorMessage = null;
		String message = null;
		if (emailText.getText().length() == 0) {
			message = Messages.RegisterAccountWizardPage_TEXT_ENTER_EMAIL;
		}
		else if (passwordText.getText().length() == 0) {
			message = Messages.RegisterAccountWizardPage_TEXT_ENTER_PW;
		}
		else if (verifyPasswordText.getText().length() == 0) {
			message = Messages.RegisterAccountWizardPage_TEXT_ENTER_PW_VERIFICATION;
		}
		else if (!passwordText.getText().equals(verifyPasswordText.getText())) {
			errorMessage = Messages.RegisterAccountWizardPage_ERROR_PW_NO_MATCH;
		}
		setMessage(message);
		setErrorMessage(errorMessage);
		setPageComplete(message == null && errorMessage == null);
	}

}
