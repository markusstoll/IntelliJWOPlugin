package org.wocommunity.plugins.intellij;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

abstract class SimpleDocumentListener implements DocumentListener {
    public abstract void update(DocumentEvent e);

    @Override
    public void insertUpdate(DocumentEvent e) {
        update(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        update(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        update(e);
    }
}
