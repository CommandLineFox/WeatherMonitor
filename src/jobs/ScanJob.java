package jobs;

import memory.Memory;
import types.Job;
import types.JobType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
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
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFileName));
             Stream<Path> stream = Files.list(Paths.get(Memory.getInstance().getSearchDirPath()))) {

            List<Path> files = stream.filter(Files::isRegularFile).toList();
            CountDownLatch latch = new CountDownLatch(files.size());

            for (Path file : files) {
                executor.submit(() -> {
                    processFile(file, writer);
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

    private void processFile(Path file, BufferedWriter writer) {
        try (Stream<String> lines = Files.lines(file)) {
            boolean skipHeader = file.toString().endsWith(".csv");

            lines.skip(skipHeader ? 1 : 0)
                    .map(this::parseLine)
                    .filter(Objects::nonNull)
                    .forEach(station -> writeResult(writer, station));
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getFileName());
        }
    }

    private Station parseLine(String line) {
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

    private synchronized void writeResult(BufferedWriter writer, Station station) {
        try {
            writer.write(station.name() + ";" + station.temperature());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to output file: " + outputFileName);
        }
    }

    private record Station(String name, double temperature) {
    }
}