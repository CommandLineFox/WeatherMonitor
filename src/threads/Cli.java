package threads;

import jobs.*;
import memory.Memory;
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
        System.out.println("CLI pokrenut. Unesite komandu (start, stop, status, map, export_map):");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            try {
                handleCommand(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Greška: " + e.getMessage());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Metod za obradu komandi
     *
     * @param input Input sa konzole
     * @throws InterruptedException Error koji moze da baci executor servis
     */
    private void handleCommand(String input) throws InterruptedException {
        String[] tokens = input.split("\\s+");
        String command = tokens[0].toUpperCase();

        Map<String, String> args = parseArguments(Arrays.copyOfRange(tokens, 1, tokens.length));

        switch (command) {
            case "SCAN":
                handleScanCommand(args);
                break;
            case "STATUS":
                handleStatusCommand(args);
                break;
            case "MAP":
                jobQueue.put(new MapJob("map-job"));
                break;
            case "EXPORTMAP":
                jobQueue.put(new ExportMapJob("export-map-job"));
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

    /**
     * Metod za parsiranje argumenata
     *
     * @param tokens Argumenti
     * @return Vraca mapovane argumente
     */
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

    /**
     * Metod za scan komandu
     *
     * @param args Argumenti koji su dati
     * @throws InterruptedException Error koji moze da baci executor servis
     */
    private void handleScanCommand(Map<String, String> args) throws InterruptedException {
        double minTemp = parseDoubleArg(args, "min", "m");
        double maxTemp = parseDoubleArg(args, "max", "M");
        String letter = parseStringArg(args, "letter", "l");
        String output = parseStringArg(args, "output", "o");
        String jobName = parseStringArg(args, "job", "j");

        jobQueue.put(new ScanJob(jobName, minTemp, maxTemp, letter.charAt(0), output));
    }

    /**
     * Metoda koja vraca status odredjenog posla
     *
     * @param args Argumenti koji su dati
     */
    private void handleStatusCommand(Map<String, String> args) {
        String jobName = parseStringArg(args, "job", "j");
        System.out.println("Status");
    }

    /**
     * Metod za gasenje programa
     *
     * @param args Argumenti koji su dati
     * @throws InterruptedException Error koji moze da baci executor servis
     */
    private void handleShutdownCommand(Map<String, String> args) throws InterruptedException {
        boolean saveJobs = args.containsKey("save-jobs") || args.containsKey("s");
        jobQueue.put(new StopJob("stop-job", saveJobs));
    }

    /**
     * Metod za pokretanje programa
     *
     * @param args Argumenti koji su dati
     * @throws InterruptedException Error koji moze da baci executor servis
     */
    private void handleStartCommand(Map<String, String> args) throws InterruptedException {
        boolean loadJobs = args.containsKey("load-jobs") || args.containsKey("l");

        Scanner scanner = new Scanner(System.in);
        System.out.print("Unesite putanju do direktorijuma: ");
        String dirPath = scanner.nextLine();

        JobDispatcher jobDispatcher = new JobDispatcher(4, 4, jobQueue);
        DirectoryMonitor directoryMonitor = new DirectoryMonitor(dirPath, jobQueue);

        Memory memory = Memory.getInstance();
        memory.setJobDispatcherThread(new Thread(jobDispatcher));
        memory.setDirectoryMonitorThread(new Thread(directoryMonitor));

        memory.getJobDispatcherThread().start();
        System.out.println("Started job dispatcher");
        memory.getDirectoryMonitorThread().start();
        System.out.println("Started directory monitor on path: " + dirPath);

        if (loadJobs) {
            System.out.println("Have to load old jobs");
        }
    }

    /**
     * Metoda za parsiranje double argumenata
     *
     * @param args     Argumenti koji su dati
     * @param longOpt  Duzi naziv argumenta
     * @param shortOpt Kraci naziv argumenta
     * @return Vracanje argumenta
     */
    private double parseDoubleArg(Map<String, String> args, String longOpt, String shortOpt) {
        String value = args.getOrDefault(longOpt, args.get(shortOpt));
        if (value == null) throw new IllegalArgumentException("Nedostaje argument: --" + longOpt);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Neispravan broj za --" + longOpt + ": " + value);
        }
    }

    /**
     * Metod za parsiranje string argumenata
     *
     * @param args     Argumenti koji su dati
     * @param longOpt  Duzi naziv argumenta
     * @param shortOpt Kraci naziv argumenta
     * @return Vracanjje argumenta
     */
    private String parseStringArg(Map<String, String> args, String longOpt, String shortOpt) {
        String value = args.getOrDefault(longOpt, args.get(shortOpt));
        if (value == null) throw new IllegalArgumentException("Nedostaje argument: --" + longOpt);
        return value;
    }
}