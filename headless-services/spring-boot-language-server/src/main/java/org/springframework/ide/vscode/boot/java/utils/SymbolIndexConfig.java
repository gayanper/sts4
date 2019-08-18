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

public interface SymbolIndexConfig {
	
	static class Builder {
		
		private boolean scanXml = false;
		
		private boolean scanTestJavaSources = false;
		
		private String[] xmlScanFoldersGlobs = new String[0];
		
		private Builder() {
			
		}

		public Builder scanXml(boolean scanXml) {
			this.scanXml = scanXml;
			return this;
		}
		
		public Builder scanTestJavaSources(boolean scanTestJavaSources) {
			this.scanTestJavaSources = scanTestJavaSources;
			return this;
		}
		
		public Builder xmlScanFoldersGlobs(String[] xmlScanFoldersGlobs) {
			this.xmlScanFoldersGlobs = xmlScanFoldersGlobs;
			return this;
		}
		
		public SymbolIndexConfig build() {
			return new SymbolIndexConfig() {

				@Override
				public boolean isScanXml() {
					return scanXml;
				}

				@Override
				public boolean isScanTestJavaSources() {
					return scanTestJavaSources;
				}

				@Override
				public String[] getXmlScanFoldersGlobs() {
					return xmlScanFoldersGlobs;
				}
				
			};
		}
	}
	
	boolean isScanXml();

	boolean isScanTestJavaSources();

	String[] getXmlScanFoldersGlobs();
	
	static Builder builder() {
		return new Builder();
	}

}
