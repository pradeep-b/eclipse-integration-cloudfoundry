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
package org.eclipse.cft.server.ui.internal.console;

import org.eclipse.cft.server.core.internal.log.LogContentType;
import org.eclipse.cft.server.core.internal.log.TraceType;
import org.eclipse.swt.SWT;

public class TraceStreamProvider extends ConsoleStreamProvider {

	@Override
	public ConsoleStream getStream(LogContentType type) {
		int swtColour = -1;
		if (TraceType.HTTP_OK.equals(type)) {
			swtColour = SWT.COLOR_BLUE;
		}
		else if (TraceType.HTTP_ERROR.equals(type)) {
			swtColour = SWT.COLOR_RED;
		}
		else if (TraceType.HTTP_GENERAL.equals(type)) {
			swtColour = SWT.COLOR_BLACK;
		}

		return swtColour > -1 ? new SingleConsoleStream(new UILogConfig(swtColour)) : null;
	}

	@Override
	public LogContentType[] getSupportedTypes() {
		return new LogContentType[] { TraceType.HTTP_OK, TraceType.HTTP_ERROR, TraceType.HTTP_GENERAL };
	}

}
