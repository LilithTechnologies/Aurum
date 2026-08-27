package re.lilith.aurum.gui;

import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class used to make interfacing with {@link FileDialog} easier and asynchronous.
 */
public final class FileDialogUtil {
    private static final ExecutorService FILE_DIALOG_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Aurum File Dialog");
        thread.setDaemon(true);
        return thread;
    });

    private FileDialogUtil() {
    }

    /**
     * Opens an asynchronous file select dialog window.
     *
     * @param dialog  Whether to open a "save" dialog or an "open" dialog
     * @param title   The title of the dialog window
     * @param origin  The path that the window should start at
     * @param filters The file extension filters used by the dialog, each formatted as {@code "*.extension"}
     * @return a {@link CompletableFuture} which is completed once a file is selected or the dialog is cancelled.
     */
    public static CompletableFuture<Optional<Path>> fileSelectDialog(DialogType dialog, String title, @Nullable Path origin, String... filters) {
        CompletableFuture<Optional<Path>> future = new CompletableFuture<>();

        FILE_DIALOG_EXECUTOR.submit(() -> {
            FileDialog fileDialog = new FileDialog((java.awt.Frame) null, title, dialog == DialogType.SAVE ? FileDialog.SAVE : FileDialog.LOAD);

            if (origin != null) {
                fileDialog.setDirectory(origin.toAbsolutePath().getParent().toString());
                fileDialog.setFile(origin.getFileName().toString());
            }

            if (filters.length > 0) {
                fileDialog.setFilenameFilter((_, name) -> matchesAny(name, filters));
            }

            fileDialog.setVisible(true);

            String directory = fileDialog.getDirectory();
            String file = fileDialog.getFile();
            fileDialog.dispose();

            future.complete(file == null ? Optional.empty() : Optional.of(Paths.get(directory, file)));
        });

        return future;
    }

    private static boolean matchesAny(String name, String[] filters) {
        String lowerName = name.toLowerCase(Locale.ROOT);

        for (String filter : filters) {
            int dot = filter.lastIndexOf('.');
            if (dot < 0) {
                continue;
            }

            if (lowerName.endsWith(filter.substring(dot).toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    public enum DialogType {
        SAVE, OPEN
    }
}
