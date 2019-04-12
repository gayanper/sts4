package org.spring.tools.boot.java.ls.highlight;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.intellij.codeInsight.daemon.impl.HintRenderer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import java.util.Collection;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Range;

public class InlayHighlightProcessor implements HighlightProcessor {

    private Multimap<String, Inlay<HintRenderer>> inlays = ArrayListMultimap.create();

    @Override
    public void preProcess(String documentUri, Editor editor) {
        final Collection<Inlay<HintRenderer>> rangeHighlighters = inlays.get(documentUri);
        if (rangeHighlighters != null) {
            rangeHighlighters.forEach(Inlay::dispose);
        }
    }

    @Override
    public void process(String documentUri, CodeLens codeLens, Editor editor) {
        if (codeLens.getData() == null) {
            return;
        }

        final Document document = editor.getDocument();
        Range range = codeLens.getRange();
        int endOffset =
            document.getLineStartOffset(range.getEnd().getLine()) + range.getEnd().getCharacter();

        HintRenderer renderer = new HintRenderer(codeLens.getData().toString());
        Inlay<HintRenderer> inlay = editor.getInlayModel().addInlineElement(endOffset, renderer);
        inlays.put(documentUri, inlay);
    }
}
