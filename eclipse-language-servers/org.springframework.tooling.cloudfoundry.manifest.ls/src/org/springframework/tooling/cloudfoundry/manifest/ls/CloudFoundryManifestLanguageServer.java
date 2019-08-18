/*******************************************************************************
 * Copyright (c) 2016, 2019 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.tooling.cloudfoundry.manifest.ls;

import static org.springframework.tooling.ls.eclipse.commons.preferences.LanguageServerConsolePreferenceConstants.CLOUDFOUNDRY_SERVER;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseMessage;
import org.eclipse.lsp4j.services.LanguageServer;
import org.springframework.tooling.ls.eclipse.commons.STS4LanguageServerProcessStreamConnector;

/**
 * @author Martin Lippert
 */
public class CloudFoundryManifestLanguageServer extends STS4LanguageServerProcessStreamConnector {
	
	private LanguageServer languageServer;
	private URI rootPath;

	private static Object cfTargetOptionSettings = null;
	private static List<CloudFoundryManifestLanguageServer> servers = new CopyOnWriteArrayList<>();

	public CloudFoundryManifestLanguageServer() {
		super(CLOUDFOUNDRY_SERVER);
		
		initExplodedJarCommand(
				Paths.get("servers", "manifest-yaml-language-server"),
				"org.springframework.ide.vscode.manifest.yaml.ManifestYamlLanguageServerBootApp",
				"application.properties",
				Arrays.asList(
						"-Dlsp.lazy.completions.disable=true",
						"-Dlsp.completions.indentation.enable=true",
						"-noverify",
						"-XX:TieredStopAtLevel=1"
				)
		);

		setWorkingDirectory(getWorkingDirLocation());
	}
	
	@Override
	public void handleMessage(Message message, LanguageServer languageServer, URI rootPath) {
		if (message instanceof ResponseMessage) {
			ResponseMessage responseMessage = (ResponseMessage)message;
			if (responseMessage.getResult() instanceof InitializeResult) {
				this.languageServer = languageServer;
				this.rootPath = rootPath;
				
				updateLanguageServer();
				addLanguageServer(this);
			}
		}
	}
	
	public static void setCfTargetLoginOptions(Object cfTargetOptions) {
		cfTargetOptionSettings = cfTargetOptions;
		
		for (CloudFoundryManifestLanguageServer server : servers) {
			server.updateLanguageServer();
		}
	}

	@Override
	public void stop() {
		removeLanguageServer(this);
		super.stop();
	}
	
	@Override
	public Object getInitializationOptions(URI rootUri) {
		return cfTargetOptionSettings;
	}
	
	protected void updateLanguageServer() {
		DidChangeConfigurationParams params = new DidChangeConfigurationParams(getInitializationOptions(rootPath));
		languageServer.getWorkspaceService().didChangeConfiguration(params);
	}

	private static void addLanguageServer(CloudFoundryManifestLanguageServer server) {
		servers.add(server);
	}

	private static void removeLanguageServer(CloudFoundryManifestLanguageServer server) {
		servers.remove(server);
	}

	@Override
	protected String getPluginId() {
		return Constants.PLUGIN_ID;
	}
}
