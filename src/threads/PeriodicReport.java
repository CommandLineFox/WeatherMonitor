package threads;

import memory.Memory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PeriodicReport implements Runnable {
    private final String filePath;

    public PeriodicReport(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(60000);
                Memory.getInstance().getLogFileLock().lock();
                try {
                    File logFile = new File(filePath);
                    if (!logFile.exists()) {
                        logFile.createNewFile();
                    }

                    try (FileWriter writer = new FileWriter(logFile, true)) {
                        writer.write("Periodic Report: Example Data\n");
                    }

                } catch (IOException e) {
                    System.err.println("Greška prilikom obrade fajla: " + e.getMessage());
                } finally {
                    Memory.getInstance().getLogFileLock().unlock();
                }
            } catch (InterruptedException e) {
                System.err.println("Greška sa pauzom periodičnog izveštaja: " + e.getMessage());
                break;
            }
        }
    }
}
