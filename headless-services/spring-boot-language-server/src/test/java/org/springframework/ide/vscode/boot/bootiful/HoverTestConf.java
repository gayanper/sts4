/*******************************************************************************
 * Copyright (c) 2018, 2019 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.bootiful;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.ide.vscode.boot.app.BootLanguageServerParams;
import org.springframework.ide.vscode.boot.editor.harness.PropertyIndexHarness;
import org.springframework.ide.vscode.boot.java.links.SourceLinkFactory;
import org.springframework.ide.vscode.boot.java.links.SourceLinks;
import org.springframework.ide.vscode.boot.java.utils.SymbolCache;
import org.springframework.ide.vscode.boot.java.utils.SymbolCacheVoid;
import org.springframework.ide.vscode.boot.metadata.ValueProviderRegistry;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectFinder;
import org.springframework.ide.vscode.commons.languageserver.util.SimpleLanguageServer;
import org.springframework.ide.vscode.commons.util.text.LanguageId;
import org.springframework.ide.vscode.project.harness.BootLanguageServerHarness;

@Configuration
@Import(AdHocPropertyHarnessTestConf.class)
public class HoverTestConf {

	@Bean SymbolCache symbolCache() {
		return new SymbolCacheVoid();
	}

	@Bean PropertyIndexHarness indexHarness(ValueProviderRegistry valueProviders) {
		return new PropertyIndexHarness(valueProviders);
	}

	@Bean BootLanguageServerHarness harness(SimpleLanguageServer server, BootLanguageServerParams serverParams, PropertyIndexHarness indexHarness, JavaProjectFinder projectFinder) throws Exception {
		return new BootLanguageServerHarness(server, serverParams, indexHarness, projectFinder, LanguageId.JAVA, ".java");
	}

	@Bean BootLanguageServerParams serverParams(SimpleLanguageServer server, ValueProviderRegistry valueProviders, PropertyIndexHarness indexHarness) {
		BootLanguageServerParams testDefaults = BootLanguageServerParams.createTestDefault(server, valueProviders);
		return new BootLanguageServerParams(
				indexHarness.getProjectFinder(),
				testDefaults.projectObserver,
				indexHarness.getIndexProvider(),
				testDefaults.typeUtilProvider
		);
	}

	@Bean JavaProjectFinder projectFinder(BootLanguageServerParams serverParams) {
		return serverParams.projectFinder;
	}

	@Bean SourceLinks sourceLinks() {
		return SourceLinkFactory.NO_SOURCE_LINKS;
	}
}