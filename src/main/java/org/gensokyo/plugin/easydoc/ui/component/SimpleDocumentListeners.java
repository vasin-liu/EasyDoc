package org.gensokyo.plugin.easydoc.ui.component;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class SimpleDocumentListeners {
    private SimpleDocumentListeners() {
        throw new UnsupportedOperationException();
    }

    public static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        };
    }
}
