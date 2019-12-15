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
package org.springframework.ide.vscode.commons.languageserver.definition;

import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ide.vscode.commons.languageserver.util.DefinitionHandler;
import org.springframework.ide.vscode.commons.languageserver.util.SimpleLanguageServer;
import org.springframework.ide.vscode.commons.util.text.TextDocument;

import com.google.common.collect.ImmutableList;

/**
 * {@link SimpleDefinitionFinder} provides a 'dummy' implementation of
 * @author Kris De Volder
 */
public class SimpleDefinitionFinder<T extends SimpleLanguageServer> implements DefinitionHandler {
	
	private static final Logger log = LoggerFactory.getLogger(SimpleDefinitionFinder.class);

	protected final T server;

	public SimpleDefinitionFinder(T server) {
		this.server = server;
	}

	@Override
	public List<LocationLink> handle(TextDocumentPositionParams params) {
		try {
			TextDocument doc = server.getTextDocumentService().get(params);
			if (doc != null) {
				int offset = doc.toOffset(params.getPosition());
				int start = offset;
				while (Character.isLetter(doc.getSafeChar(start))) {
					start--;
				}
				start = start+1;
				int end = offset;
				while (Character.isLetter(doc.getSafeChar(end))) {
					end++;
				}
				String word = doc.textBetween(start, end);
				String text = doc.get();
				int def = text.indexOf(word);
				if (def>=0) {
					Range targetRange = doc.toRange(def, word.length());
					LocationLink link = new LocationLink(params.getTextDocument().getUri(),
						targetRange, targetRange, doc.toRange(start, end - start)
					);
					return ImmutableList.of(link);
				}
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return Collections.emptyList();
	}

}
