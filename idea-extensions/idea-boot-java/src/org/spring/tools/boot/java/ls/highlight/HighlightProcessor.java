package org.spring.tools.boot.java.ls.highlight;

import com.intellij.openapi.editor.Editor;
import org.eclipse.lsp4j.CodeLens;

public interface HighlightProcessor {

    void preProcess(String documentUri, Editor editor);

    void process(String documentUri, CodeLens codeLens, Editor editor);
}
