package org.wocommunity.plugins.intellij.components;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.util.Key;

/**
 * Projekt‑Service, die den FileEditorManagerListener **einmal** registriert.
 * Der Listener wird beim Öffnen eines WO‑Editor‑Fensters ausgeführt
 * und wählt die entsprechende WOComponentNode im Projekt‑View aus.
 */
@Service
public final class WOComponentSelectionService {

    // Key, um die Connection im Projekt‑UserData zu speichern
    private static final Key<FileEditorManagerListener> LISTENER_KEY =
            Key.create("WOComponentSelectionListener");

    public WOComponentSelectionService(Project project) {
        // 1️⃣ Nur registrieren, wenn noch keine Listener‑Instanz existiert
        if (project.getUserData(LISTENER_KEY) == null) {
            FileEditorManagerListener listener = new WOComponentSelectionListener();

            // 2️⃣ Listener per MessageBus des Projekts registrieren
            project.getMessageBus()
                   .connect()
                   .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener);

            // 3️⃣ Connection im Projekt‑UserData ablegen, damit wir später
            //     die Verbindung sauber trennen können (z. B. bei Projekt‑Schließung)
            project.putUserData(LISTENER_KEY, listener);
        }
    }
}
