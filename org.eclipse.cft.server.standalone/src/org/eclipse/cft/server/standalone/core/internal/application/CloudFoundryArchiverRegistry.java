package org.eclipse.cft.server.standalone.core.internal.application;

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class CloudFoundryArchiverRegistry {
	
	public static final CloudFoundryArchiverRegistry INSTANCE = new CloudFoundryArchiverRegistry();
	private ICloudFoundryArchiver archiver = null;

	private CloudFoundryArchiverRegistry() {
		// Cannot be created from outside
	}
	
	public ICloudFoundryArchiver getArchiver() throws CoreException {
		if (this.archiver == null) {
			createArchiver();
		}
		return this.archiver;
	}

	private void createArchiver() throws CoreException {
		final String ARCHIVER_DELEGATE = "org.eclipse.cft.server.standalone.core.archiverDelegate";
		final String ARCHIVER_ELEMENT = "archiver";
		final String CLASS_ATTR = "class";
		
		// At present it just picks the first archiver extension

		IExtensionPoint archiverExtnPoint = Platform.getExtensionRegistry().getExtensionPoint(ARCHIVER_DELEGATE);
		if (archiverExtnPoint != null) {	
			for (IExtension extension : archiverExtnPoint.getExtensions()) {
				for (IConfigurationElement config : extension.getConfigurationElements()) {
					if (ARCHIVER_ELEMENT.equals(config.getName())) {
						this.archiver = (ICloudFoundryArchiver) config
								.createExecutableExtension(CLASS_ATTR);
					}
				}
			}
		}

		throw CloudErrorUtil.toCoreException("Could not locate archivers");
	}

}
