package threads;

import jobs.ReadFileJob;
import types.Job;
import types.ReadFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class DirectoryMonitor implements Runnable {
    private final Path directory;
    private final BlockingQueue<Job> jobQueue;
    private final Map<String, Long> lastModifiedMap; // Praćenje poslednje izmene fajlova

    public DirectoryMonitor(String dirPath, BlockingQueue<Job> jobQueue) {
        this.directory = Paths.get(dirPath);
        this.jobQueue = jobQueue;
        this.lastModifiedMap = new HashMap<>();
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watchService.take(); // Blokira dok ne dođe event
                } catch (InterruptedException e) {
                    System.out.println("Watcher thread interrupted. Shutting down...");
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    Path filePath = directory.resolve((Path) event.context());
                    File file = filePath.toFile();

                    // Ako fajl nije .txt ili .csv ili ne postoji, preskoči iteraciju
                    if (!(filePath.toString().endsWith(".txt") || filePath.toString().endsWith(".csv")) || !file.exists()) {
                        continue;
                    }

                    String fileName = filePath.getFileName().toString();
                    long newLastModified = file.lastModified();

                    synchronized (lastModifiedMap) {
                        Long lastModified = lastModifiedMap.get(filePath.toString());
                        if (lastModified == null || lastModified < newLastModified) {
                            lastModifiedMap.put(filePath.toString(), newLastModified);
                            jobQueue.put(new ReadFileJob("Read", new ReadFile(fileName, filePath.toString(), newLastModified)));
                            System.out.println("Queued ReadFileJob for: " + fileName);
                        }
                    }
                }

                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Watcher error: " + e.getMessage());
        }
    }
}
