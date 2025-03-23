package jobs;

import memory.Memory;
import types.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ReadFileJob extends Job {
    private final ReadFile readFile;
    private static final int THREAD_COUNT = 4;

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

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<Void>> futures = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> chunk = new ArrayList<>();
            boolean skipHeader = file.getName().endsWith(".csv");
            if (skipHeader) {
                reader.readLine();
            }

            int lineCount = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                chunk.add(line);
                lineCount++;

                if (lineCount % THREAD_COUNT == 0) {
                    futures.add(executor.submit(new FileChunkProcessor(new ArrayList<>(chunk), memory)));
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                futures.add(executor.submit(new FileChunkProcessor(chunk, memory)));
            }

            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.err.println("Error reading file: " + readFile.getPath());
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
            setJobStatus(JobStatus.COMPLETED);
            System.out.println("Finished processing file: " + readFile.getName());
        }
    }

    /**
     * Record za procesovanje segmenta fajla
     *
     * @param lines  Linije
     * @param memory Pristup memoriji
     */
    private record FileChunkProcessor(List<String> lines, Memory memory) implements Callable<Void> {
        @Override
        public Void call() {
            for (String line : lines) {
                String[] parts = line.split("[;,]");

                if (parts.length < 2) {
                    continue;
                }

                String stationName = parts[0].trim();
                double temperature;
                try {
                    temperature = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                char firstLetter = Character.toUpperCase(stationName.charAt(0));

                synchronized (memory) {
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
            return null;
        }
    }
}