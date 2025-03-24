package jobs;

import lombok.Getter;
import memory.Memory;
import types.*;

import java.io.*;
import java.util.concurrent.*;

@Getter
public class ReadFileJob extends Job implements Serializable {
    private final ReadFile readFile;

    public ReadFileJob(String name, ReadFile readFile) {
        super(JobType.READ_FILE, name);
        this.readFile = readFile;
    }

    @Override
    public void execute() {
        setJobStatus(JobStatus.RUNNING);
        Memory memory = Memory.getInstance();

        File file = new File(readFile.getPath());
        if (!file.exists()) {
            System.err.println("File not found: " + readFile.getPath());
            return;
        }

        ExecutorService executor = Executors.newCachedThreadPool();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            boolean skipHeader = file.getName().endsWith(".csv");
            if (skipHeader) {
                reader.readLine();
            }

            String line;
            while ((line = reader.readLine()) != null) {
                final String currentLine = line;

                executor.submit(() -> {
                    processLine(currentLine, memory);
                });
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + readFile.getPath());
        } finally {
            executor.shutdown();
            setJobStatus(JobStatus.COMPLETED);
            System.out.println("Finished processing file: " + readFile.getName());
        }
    }

    /**
     * Procesovanje linija u fajlu
     *
     * @param line   Line to process
     * @param memory Memory instance
     */
    private void processLine(String line, Memory memory) {
        String[] parts = line.split("[;,]");

        if (parts.length < 2) {
            return;
        }

        String stationName = parts[0].trim();
        double temperature;
        try {
            temperature = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            return;
        }

        char firstLetter = Character.toUpperCase(stationName.charAt(0));

        synchronized (memory.getData()) {
            memory.getData().compute(firstLetter, (key, parsedData) -> {
                if (parsedData == null) {
                    parsedData = new ParsedData(0, 0);
                }

                parsedData.incrementStationCount();
                parsedData.addTemperature(temperature);
                return parsedData;
            });
        }
    }
}
