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
    private final Map<String, Long> lastModifiedMap;

    public DirectoryMonitor(String dirPath, BlockingQueue<Job> jobQueue) {
        this.directory = Paths.get(dirPath);
        this.jobQueue = jobQueue;
        this.lastModifiedMap = new HashMap<>();
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

            processExistingFiles();

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

    /**
     * Metod za procesovanje postojecih fajlova
     */
    public void processExistingFiles() {
        File dir = directory.toFile();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt") || name.endsWith(".csv"));

        if (files == null) {
            return;
        }

        for (File file : files) {
            long lastModified = file.lastModified();
            lastModifiedMap.put(file.getAbsolutePath(), lastModified);
            try {
                jobQueue.put(new ReadFileJob("Read", new ReadFile(file.getName(), file.getAbsolutePath(), lastModified)));
                System.out.println("Queued existing file: " + file.getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while queuing existing file: " + file.getName());
            }
        }
    }
}
