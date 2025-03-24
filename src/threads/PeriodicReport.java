package threads;

import memory.Memory;
import types.ParsedData;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeriodicReport implements Runnable {
    private final String filePath;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public PeriodicReport(String filePath) {
        this.filePath = filePath;
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        Memory memory = Memory.getInstance();

        while (running.get()) {
            try {
                Thread.sleep(60000);

                memory.getLogFileLock().lock();
                try (FileWriter writer = new FileWriter(filePath, true)) {
                    writer.write("Periodic Report:\n");
                    for (Map.Entry<Character, ParsedData> entry : memory.getData().entrySet()) {
                        char letter = entry.getKey();
                        ParsedData data = entry.getValue();
                        writer.write(letter + "," + data.getAppearanceCount() + "," + String.format("%.2f", data.getValueSum()) + "\n");
                    }
                    writer.write("\n");
                } catch (IOException e) {
                    System.err.println("Greška prilikom pisanja u fajl: " + e.getMessage());
                } finally {
                    memory.getLogFileLock().unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("PeriodicReport nit je prekinuta.");
                break;
            }
        }
    }
}