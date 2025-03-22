package threads;

import jobs.*;
import types.*;

import java.util.*;
import java.util.concurrent.BlockingQueue;

public class Cli implements Runnable {
    private final BlockingQueue<Job> jobQueue;

    public Cli(BlockingQueue<Job> jobQueue) {
        this.jobQueue = jobQueue;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("CLI pokrenut. Unesite komande (`SCAN`, `STATUS`, `MAP`, `EXPORTMAP`, `SHUTDOWN`, `START`):");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            try {
                handleCommand(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Greška: " + e.getMessage());
            }
        }
    }

    private void handleCommand(String input) {
        String[] tokens = input.split("\\s+");
        String command = tokens[0].toUpperCase();

        // Parsiramo argumente
        Map<String, String> args = parseArguments(Arrays.copyOfRange(tokens, 1, tokens.length));

        switch (command) {
            case "SCAN":
                handleScanCommand(args);
                break;
            case "STATUS":
                handleStatusCommand(args);
                break;
            case "MAP":
                jobQueue.offer(new MapJob("map-job"));
                break;
            case "EXPORTMAP":
                jobQueue.offer(new ExportMapJob("export-map-job"));
                break;
            case "SHUTDOWN":
                handleShutdownCommand(args);
                break;
            case "START":
                handleStartCommand(args);
                break;
            default:
                throw new IllegalArgumentException("Nepoznata komanda: " + command);
        }
    }

    private Map<String, String> parseArguments(String[] tokens) {
        Map<String, String> args = new HashMap<>();
        String key = null;

        for (String token : tokens) {
            if (token.startsWith("--")) {
                key = token.substring(2);
            } else if (token.startsWith("-")) {
                key = token.substring(1);
            } else {
                if (key == null) throw new IllegalArgumentException("Neispravan format argumenta: " + token);
                args.put(key, token);
                key = null;
            }
        }

        return args;
    }

    private void handleScanCommand(Map<String, String> args) {
        double minTemp = parseDoubleArg(args, "min", "m");
        double maxTemp = parseDoubleArg(args, "max", "M");
        String letter = parseStringArg(args, "letter", "l");
        String output = parseStringArg(args, "output", "o");
        String jobName = parseStringArg(args, "job", "j");

        jobQueue.offer(new ScanJob(jobName, minTemp, maxTemp, letter.charAt(0), output));
    }

    private void handleStatusCommand(Map<String, String> args) {
        String jobName = parseStringArg(args, "job", "j");
        System.out.println("Status");
    }

    private void handleShutdownCommand(Map<String, String> args) {
        boolean saveJobs = args.containsKey("save-jobs") || args.containsKey("s");
        jobQueue.offer(new StopJob("stop-job", saveJobs));
    }

    private void handleStartCommand(Map<String, String> args) {
        boolean loadJobs = args.containsKey("load-jobs") || args.containsKey("l");
        jobQueue.offer(new StartJob("start-job", loadJobs));
    }

    private double parseDoubleArg(Map<String, String> args, String longOpt, String shortOpt) {
        String value = args.getOrDefault(longOpt, args.get(shortOpt));
        if (value == null) throw new IllegalArgumentException("Nedostaje argument: --" + longOpt);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Neispravan broj za --" + longOpt + ": " + value);
        }
    }

    private String parseStringArg(Map<String, String> args, String longOpt, String shortOpt) {
        String value = args.getOrDefault(longOpt, args.get(shortOpt));
        if (value == null) throw new IllegalArgumentException("Nedostaje argument: --" + longOpt);
        return value;
    }
}
