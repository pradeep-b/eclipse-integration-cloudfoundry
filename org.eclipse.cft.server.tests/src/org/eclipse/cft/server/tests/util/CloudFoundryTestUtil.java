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
package org.eclipse.cft.server.tests.util;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class CloudFoundryTestUtil {

	/**
	 * @param host
	 * @param proxyType
	 * @return proxy or null if it cannot be resolved
	 */
	public static Proxy getProxy(String host, String proxyType) {
		Proxy foundProxy = null;
		try {

			URI uri = new URI(proxyType, "//" + host, null);
			List<Proxy> proxies = ProxySelector.getDefault().select(uri);

			if (proxies != null) {
				for (Proxy proxy : proxies) {
					if (proxy != Proxy.NO_PROXY) {
						foundProxy = proxy;
						break;
					}
				}
			}

		}
		catch (URISyntaxException e) {
			// No proxy
		}
		return foundProxy;
	}

	public static void waitIntervals(long timePerTick) {
		try {
			Thread.sleep(timePerTick);
		}
		catch (InterruptedException e) {

		}
	}

}
