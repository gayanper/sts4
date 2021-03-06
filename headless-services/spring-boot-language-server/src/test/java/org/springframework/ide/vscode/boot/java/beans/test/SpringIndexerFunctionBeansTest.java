/*******************************************************************************
 * Copyright (c) 2017, 2018 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.vscode.boot.java.beans.test;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.ide.vscode.boot.bootiful.BootLanguageServerTest;
import org.springframework.ide.vscode.boot.bootiful.SymbolProviderTestConf;
import org.springframework.ide.vscode.boot.java.utils.SpringSymbolIndex;
import org.springframework.ide.vscode.commons.languageserver.java.JavaProjectFinder;
import org.springframework.ide.vscode.project.harness.BootLanguageServerHarness;
import org.springframework.ide.vscode.project.harness.ProjectsHarness;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Martin Lippert
 */
@RunWith(SpringRunner.class)
@BootLanguageServerTest
@Import(SymbolProviderTestConf.class)
public class SpringIndexerFunctionBeansTest {

	@Autowired private BootLanguageServerHarness harness;
	@Autowired private SpringSymbolIndex indexer;
	@Autowired private JavaProjectFinder projectFinder;

	private File directory;

	@Before
	public void setup() throws Exception {
		harness.intialize(null);

		directory = new File(ProjectsHarness.class.getResource("/test-projects/test-annotation-indexing-beans/").toURI());

		String projectDir = directory.toURI().toString();

		// trigger project creation
		projectFinder.find(new TextDocumentIdentifier(projectDir)).get();

		CompletableFuture<Void> initProject = indexer.waitOperation();
		initProject.get(5, TimeUnit.SECONDS);
	}

	@Test
	public void testScanSimpleFunctionBean() throws Exception {
		String docUri = directory.toPath().resolve("src/main/java/org/test/FunctionClass.java").toUri().toString();
		SpringIndexerHarness.assertDocumentSymbols(indexer, docUri,
				SpringIndexerHarness.symbol("@Configuration", "@+ 'functionClass' (@Configuration <: @Component) FunctionClass"),
				SpringIndexerHarness.symbol("@Bean", "@> 'uppercase' (@Bean) Function<String,String>")
		);
	}

	@Test
	public void testScanSimpleFunctionClass() throws Exception {
		String docUri = directory.toPath().resolve("src/main/java/org/test/ScannedFunctionClass.java").toUri().toString();
		SpringIndexerHarness.assertDocumentSymbols(indexer, docUri,
				SpringIndexerHarness.symbol("ScannedFunctionClass", "@> 'scannedFunctionClass' Function<String,String>")
		);
	}

	@Test
	public void testScanSpecializedFunctionClass() throws Exception {
		String docUri = directory.toPath().resolve("src/main/java/org/test/FunctionFromSpecializedClass.java").toUri().toString();
		SpringIndexerHarness.assertDocumentSymbols(indexer, docUri,
				SpringIndexerHarness.symbol("FunctionFromSpecializedClass", "@> 'functionFromSpecializedClass' Function<String,String>")
		);
	}

	@Test
	public void testScanSpecializedFunctionInterface() throws Exception {
		String docUri = directory.toPath().resolve("src/main/java/org/test/FunctionFromSpecializedInterface.java").toUri().toString();
		SpringIndexerHarness.assertDocumentSymbols(indexer, docUri,
				SpringIndexerHarness.symbol("FunctionFromSpecializedInterface", "@> 'functionFromSpecializedInterface' Function<String,String>")
		);
	}

	@Test
	public void testNoSymbolForAbstractClasses() throws Exception {
		String docUri = directory.toPath().resolve("src/main/java/org/test/SpecializedFunctionClass.java").toUri().toString();
		SpringIndexerHarness.assertDocumentSymbols(indexer, docUri);
	}

	@Test
	public void testNoSymbolForSubInterfaces() throws Exception {
		String docUri = directory.toPath().resolve("src/main/java/org/test/SpecializedFunctionInterface.java").toUri().toString();
		SpringIndexerHarness.assertDocumentSymbols(indexer, docUri);
	}

	@Test
	public void testScanInconsistentInterfaceHierarchy() throws Exception {
		String docUri = directory.toPath().resolve("src/main/java/org/test/LoopedFunctionClass.java").toUri().toString();
		SpringIndexerHarness.assertDocumentSymbols(indexer, docUri);
	}

}
