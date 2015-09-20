/*******************************************************************************
 * Copyright (c) 2013, 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.standalone.internal.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.standalone.internal.Messages;
import org.eclipse.cft.server.standalone.internal.startcommand.JavaTypeResolver;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * Resolves all the package fragment roots and main type for a give Java
 * project. It includes handling all required Java projects for the given java
 * project.
 * 
 */
public class JavaPackageFragmentRootHandler {

	private IJavaProject javaProject;

	private IType type;

	private CloudFoundryServer cloudServer;

	public JavaPackageFragmentRootHandler(IJavaProject javaProject,
			CloudFoundryServer cloudServer) {
		this.javaProject = javaProject;
		this.cloudServer = cloudServer;
	}

	public IPackageFragmentRoot[] getPackageFragmentRoots(
			IProgressMonitor monitor) throws CoreException {
		IType type = getMainType(monitor);

		IPath[] classpathEntries = null;
		ILaunchConfiguration configuration = null;
		try {
			configuration = createConfiguration(type);
			classpathEntries = getRuntimeClasspaths(configuration);
		} finally {
			if (configuration != null) {
				configuration.delete();
			}
		}

		List<IPackageFragmentRoot> pckRoots = new ArrayList<IPackageFragmentRoot>();

		// Since the java project may have other required projects, fetch the
		// ordered
		// list of required projects that will be used to search for package
		// fragment roots
		// corresponding to the resolved class paths. The order of the java
		// projects to search in should
		// start with the most immediate list of required projects of the java
		// project.
		List<IJavaProject> javaProjectsToSearch = getOrderedJavaProjects(javaProject);

		// Find package fragment roots corresponding to the path entries.
		// Search through all java projects, not just the immediate java project
		// for the application that is being pushed to CF.
		for (IPath path : classpathEntries) {

			for (IJavaProject javaProject : javaProjectsToSearch) {

				try {
					IPackageFragmentRoot[] roots = javaProject
							.getPackageFragmentRoots();

					if (roots != null) {
						List<IPackageFragmentRoot> foundRoots = new ArrayList<IPackageFragmentRoot>();

						for (IPackageFragmentRoot packageFragmentRoot : roots) {
							// Note that a class path entry may correspond to a
							// Java project's target directory.
							// Different fragment roots may use the same target
							// directory, so relationship between fragment roots
							// to a class path entry may be many-to-one.

							if (isRootAtEntry(packageFragmentRoot, path)) {
								foundRoots.add(packageFragmentRoot);
							}
						}

						// Stop after the first successful search
						if (!foundRoots.isEmpty()) {
							pckRoots.addAll(foundRoots);
							break;
						}
					}

				} catch (Exception e) {
					throw CloudErrorUtil.toCoreException(e);
				}
			}

		}

		return pckRoots.toArray(new IPackageFragmentRoot[pckRoots.size()]);

	}

	/**
	 * Attempts to resolve a main type in the associated Java project. Throws
	 * {@link CoreException} if it failed to resolve main type or no main type
	 * found.
	 * 
	 * @param monitor
	 * @return non-null Main type.
	 * @throws CoreException
	 *             if failure occurred while resolving main type or no main type
	 *             found
	 */
	public IType getMainType(IProgressMonitor monitor) throws CoreException {
		if (type == null) {
			type = new JavaTypeResolver(javaProject, cloudServer.getServer()
					.getServerType().getId()).getMainTypesFromSource(monitor);
			if (type == null) {
				throw CloudErrorUtil
						.toCoreException(Messages.JavaCloudFoundryArchiver_ERROR_NO_MAIN);
			}
		}
		return type;
	}

	protected List<IJavaProject> getOrderedJavaProjects(IJavaProject project) {
		List<String> collectedProjects = new ArrayList<String>();
		getOrderedJavaProjectNames(
				Arrays.asList(project.getProject().getName()),
				collectedProjects);

		List<IJavaProject> projects = new ArrayList<IJavaProject>();

		for (String name : collectedProjects) {
			IProject prj = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(name);
			if (prj != null) {
				IJavaProject jvPrj = JavaCore.create(prj);
				if (jvPrj != null && jvPrj.exists()) {
					projects.add(jvPrj);
				}
			}
		}

		return projects;

	}

	protected void getOrderedJavaProjectNames(
			List<String> sameLevelRequiredProjects,
			List<String> collectedProjects) {
		// The order in which required projects are collected is as follows,
		// with the RHS
		// being required projects of the LHS
		// A -> BC
		// B -> D
		// C -> E
		// = total 5 projects, added in the order that they are encountered.
		// so final ordered list should be ABCDE
		if (sameLevelRequiredProjects == null) {
			return;
		}
		List<String> nextLevelRequiredProjects = new ArrayList<String>();
		// First add the current level java projects in the order they appear
		// and also collect each one's required names.
		for (String name : sameLevelRequiredProjects) {
			try {
				IProject project = ResourcesPlugin.getWorkspace().getRoot()
						.getProject(name);
				if (project != null) {
					IJavaProject jvPrj = JavaCore.create(project);
					if (jvPrj != null && jvPrj.exists()) {
						if (!collectedProjects.contains(name)) {
							collectedProjects.add(name);
						}
						String[] names = jvPrj.getRequiredProjectNames();
						if (names != null && names.length > 0) {
							for (String reqName : names) {
								if (!nextLevelRequiredProjects
										.contains(reqName)) {
									nextLevelRequiredProjects.add(reqName);
								}
							}
						}
					}
				}

			} catch (JavaModelException e) {
				CloudFoundryPlugin.logError(e);
			}

		}

		// Now recurse to fetch the required projects for the
		// list of java projects that were added at the current level above
		if (!nextLevelRequiredProjects.isEmpty()) {
			getOrderedJavaProjectNames(nextLevelRequiredProjects,
					collectedProjects);

		}
	}

	protected ILaunchConfiguration createConfiguration(IType type)
			throws CoreException {

		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

		ILaunchConfigurationType configType = manager
				.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);

		ILaunchConfigurationWorkingCopy workingCopy = configType.newInstance(
				null, manager.generateLaunchConfigurationName(type
						.getTypeQualifiedName('.')));
		workingCopy.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
				type.getFullyQualifiedName());
		workingCopy.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, type
						.getJavaProject().getElementName());
		workingCopy.setMappedResources(new IResource[] { type
				.getUnderlyingResource() });
		return workingCopy.doSave();
	}

	protected IPath[] getRuntimeClasspaths(ILaunchConfiguration configuration)
			throws CoreException {
		IRuntimeClasspathEntry[] entries = JavaRuntime
				.computeUnresolvedRuntimeClasspath(configuration);
		entries = JavaRuntime.resolveRuntimeClasspath(entries, configuration);

		ArrayList<IPath> userEntries = new ArrayList<IPath>(entries.length);
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {

				String location = entries[i].getLocation();
				if (location != null) {
					IPath entry = Path.fromOSString(location);
					if (!userEntries.contains(entry)) {
						userEntries.add(entry);
					}
				}
			}
		}
		return userEntries.toArray(new IPath[userEntries.size()]);
	}

	/**
	 * 
	 * Determines if the given package fragment root corresponds to the class
	 * path entry path.
	 * <p/>
	 * Note that different package fragment roots may point to the same class
	 * path entry.
	 * <p/>
	 * Example:
	 * <p/>
	 * A Java project may have the following package fragment roots:
	 * <p/>
	 * - src/main/java
	 * <p/>
	 * - src/main/resources
	 * <p/>
	 * Both may be using the same output folder:
	 * <p/>
	 * target/classes.
	 * <p/>
	 * In this case, the output folder will have a class path entry -
	 * target/classes - and it will be the same for both roots, and this method
	 * will return true for both roots if passed the entry for target/classes
	 * 
	 * @param root
	 *            to check if it corresponds to the given class path entry path
	 * @param entry
	 * @return true if root is at the given entry
	 */
	private static boolean isRootAtEntry(IPackageFragmentRoot root, IPath entry) {
		try {
			IClasspathEntry cpe = root.getRawClasspathEntry();
			if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath outputLocation = cpe.getOutputLocation();
				if (outputLocation == null) {
					outputLocation = root.getJavaProject().getOutputLocation();
				}

				IPath location = ResourcesPlugin.getWorkspace().getRoot()
						.findMember(outputLocation).getLocation();
				if (entry.equals(location)) {
					return true;
				}
			}
		} catch (JavaModelException e) {
			CloudFoundryPlugin.logError(e);
		}

		IResource resource = root.getResource();
		if (resource != null && entry.equals(resource.getLocation())) {
			return true;
		}

		IPath path = root.getPath();
		if (path != null && entry.equals(path)) {
			return true;
		}

		return false;
	}
}
