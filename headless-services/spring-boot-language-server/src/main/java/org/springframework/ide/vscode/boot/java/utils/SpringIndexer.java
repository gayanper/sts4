/*******************************************************************************
 * Copyright (c) 2019 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.utils;

import org.springframework.ide.vscode.commons.java.IJavaProject;

import com.google.common.base.Supplier;

/**
 * @author Martin Lippert
 */
public interface SpringIndexer {

	String[] getFileWatchPatterns();
	boolean isInterestedIn(String docURI);

	void initializeProject(IJavaProject project) throws Exception;
	void removeProject(IJavaProject project) throws Exception;

	void updateFile(IJavaProject project, String docURI, long lastModified, Supplier<String> content) throws Exception;
	void removeFile(IJavaProject project, String docURI) throws Exception;


}
