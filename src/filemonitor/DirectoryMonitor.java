package filemonitor;

import types.ReadFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class DirectoryMonitor implements Runnable {
    private final Path directory;
    private final BlockingQueue<ReadFile> queue;
    private final Map<String, ReadFile> lastModifiedMap; // Promenjena mapa za praćenje ReadFile objekata
    private final ExecutorService executor;

    public DirectoryMonitor(String dirPath, BlockingQueue<ReadFile> queue) {
        this.directory = Paths.get(dirPath);
        this.queue = queue;
        this.executor = Executors.newFixedThreadPool(2);

        this.lastModifiedMap = new HashMap<>();
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    System.out.println("Watcher thread interrupted. Shutting down...");
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    Path filePath = directory.resolve((Path) event.context());
                    File file = filePath.toFile();

                    if ((filePath.toString().endsWith(".txt") || filePath.toString().endsWith(".csv")) && file.exists()) {
                        String fileName = filePath.getFileName().toString();
                        long newLastModified = file.lastModified();

                        synchronized (lastModifiedMap) {
                            ReadFile existingFile = lastModifiedMap.get(fileName);
                            if (existingFile != null) {
                                if (existingFile.getLastModified() < newLastModified) {
                                    existingFile.setLastModified(newLastModified);
                                    queue.put(existingFile);
                                    System.out.println("Updated file in processing queue: " + fileName);
                                }
                            } else {
                                ReadFile newFile = new ReadFile(fileName, filePath.toString(), newLastModified);
                                lastModifiedMap.put(fileName, newFile);
                                queue.put(newFile);
                                System.out.println("Sent file to processing queue: " + fileName);
                            }
                        }
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Watcher error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }
}
