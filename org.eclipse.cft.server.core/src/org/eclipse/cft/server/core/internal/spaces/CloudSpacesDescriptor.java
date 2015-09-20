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
package org.eclipse.cft.server.core.internal.spaces;

public class CloudSpacesDescriptor {

	private final String descriptorID;

	private final CloudOrgsAndSpaces spaces;

	public CloudSpacesDescriptor(CloudOrgsAndSpaces spaces, String userName, String password, String actualServerURL,
			boolean selfSigned) {
		this.spaces = spaces;
		descriptorID = getDescriptorID(userName, password, actualServerURL, selfSigned);

	}

	public CloudOrgsAndSpaces getOrgsAndSpaces() {
		return spaces;
	}

	public String getID() {
		return descriptorID;
	}

	public static String getDescriptorID(String userName, String password, String actualURL, boolean selfSigned) {
		if (userName == null || password == null || actualURL == null) {
			return null;
		}
		return userName + password + actualURL + selfSigned;
	}

}