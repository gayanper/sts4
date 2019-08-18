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
package org.springframework.ide.vscode.boot.xml.hyperlinks;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4xml.dom.DOMAttr;
import org.eclipse.lsp4xml.dom.DOMNode;
import org.springframework.ide.vscode.commons.util.text.TextDocument;

/**
 * @author Alex Boyko
 */
public interface XMLHyperlinkProvider {
	
	Location getDefinition(TextDocument doc, String namespace, DOMNode node, DOMAttr attributeAt);

}
