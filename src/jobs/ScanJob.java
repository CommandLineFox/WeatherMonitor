package jobs;

import types.JobStatus;
import utility.Memory;
import types.Job;
import types.JobType;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class ScanJob extends Job implements Serializable {
    private final double minimum;
    private final double maximum;
    private final char startLetter;
    private final String outputFileName;

    public ScanJob(String name, double minimum, double maximum, char startLetter, String outputFileName) {
        super(JobType.SCAN, name);
        this.minimum = minimum;
        this.maximum = maximum;
        this.startLetter = Character.toUpperCase(startLetter);
        this.outputFileName = outputFileName;
    }

    @Override
    public void execute() {
        setJobStatus(JobStatus.RUNNING);
        Memory memory = Memory.getInstance();
        memory.getJobHistory().put(this.getName(), this);

        if (!memory.getRunning().get()) {
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFileName))) {
            List<Path> files;
            try (Stream<Path> stream = Files.list(Paths.get(memory.getSearchDirPath()))) {
                files = stream.filter(Files::isRegularFile).toList();
            } catch (IOException e) {
                System.err.println("Error listing files: " + e.getMessage());
                return;
            }

            CountDownLatch latch = new CountDownLatch(files.size());

            for (Path file : files) {
                if (!memory.getRunning().get()) {
                    System.out.println("Scan job aborted before processing files.");
                    break;
                }

                executor.submit(() -> {
                    if (memory.getRunning().get()) {
                        processFile(file, writer);
                    }
                    latch.countDown();
                });
            }

            latch.await();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error processing scan job: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        System.out.println("Scan job completed: " + outputFileName);
    }

    /**
     * Metod za procesiranje fajla
     *
     * @param file   Fajl koji treba procesovati
     * @param writer Writer za pisanje
     */
    private void processFile(Path file, BufferedWriter writer) {
        Memory memory = Memory.getInstance();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            boolean skipHeader = file.toString().endsWith(".csv");
            if (skipHeader) reader.readLine(); // Skip CSV header

            String line;
            while ((line = reader.readLine()) != null) {
                if (!memory.getRunning().get()) {
                    System.out.println("Scan job interrupted while processing " + file.getFileName());
                    break;
                }

                Station station = parseLine(line);
                if (station != null) {
                    writeResult(writer, station);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getFileName());
        }
    }

    /**
     * Metod za parsiranje pojedinacne linije
     *
     * @param line Linija koja se parsira
     * @return Podaci za upisivanje
     */
    private Station parseLine(String line) {
        Memory memory = Memory.getInstance();
        if (!memory.getRunning().get()) {
            return null;
        }

        String[] parts = line.split("[;,]");
        if (parts.length < 2) return null;

        String stationName = parts[0].trim();
        if (stationName.isEmpty() || Character.toUpperCase(stationName.charAt(0)) != startLetter) return null;

        try {
            double temperature = Double.parseDouble(parts[1].trim());
            if (temperature < minimum || temperature > maximum) return null;
            return new Station(stationName, temperature);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Metod za pisanje u fajl
     *
     * @param writer  Writer kojim se pise
     * @param station Podaci koji se upisuju
     */
    private void writeResult(BufferedWriter writer, Station station) {
        synchronized (writer) {
            try {
                writer.write(station.name() + ";" + station.temperature());
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Error writing to output file: " + outputFileName);
            }
        }
    }

    private record Station(String name, double temperature) {
    }
}