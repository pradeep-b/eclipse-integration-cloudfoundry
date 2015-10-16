package org.eclipse.cft.server.standalone.internal.application;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public interface ICloudFoundryArchiver {
	
	public ApplicationArchive getApplicationArchive(IProgressMonitor monitor)
			throws CoreException;
	
}
