package threads;

import utility.Memory;
import types.ParsedData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class PeriodicReport implements Runnable {
    private final String filePath;

    public PeriodicReport(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void run() {
        Memory memory = Memory.getInstance();

        while (memory.getRunning().get()) {
            try {
                Thread.sleep(60000);

                memory.getLogFileLock().lock();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                    writer.write("Periodic Report:\n");

                    for (Map.Entry<Character, ParsedData> entry : memory.getData().entrySet()) {
                        char letter = entry.getKey();
                        ParsedData data = entry.getValue();
                        writer.write(letter + "," + data.getAppearanceCount() + "," + String.format("%.2f", data.getValueSum()) + "\n");
                    }

                    writer.write("\n");

                } catch (IOException e) {
                    System.err.println("Error during writing into file: " + e.getMessage());
                } finally {
                    memory.getLogFileLock().unlock();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}