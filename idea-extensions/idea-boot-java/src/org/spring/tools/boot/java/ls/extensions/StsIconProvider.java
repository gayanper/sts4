package org.spring.tools.boot.java.ls.extensions;

import org.eclipse.lsp4j.SymbolKind;
import org.spring.tools.boot.java.ls.StsIcons;
import org.wso2.lsp4intellij.contributors.icon.LSPDefaultIconProvider;

import javax.swing.*;

public class StsIconProvider extends LSPDefaultIconProvider {
    @Override
    public Icon getSymbolIcon(SymbolKind kind) {
        if (kind == SymbolKind.Interface) {
            return StsIcons.getBeanIcon();
        }
        return super.getSymbolIcon(kind);
    }
}
