package threads;

import jobs.*;
import memory.Memory;
import types.Job;
import types.JobStatus;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Cli implements Runnable {
    private final Memory memory;
    private AtomicBoolean running = new AtomicBoolean(true);

    public Cli() {
        this.memory = Memory.getInstance();
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("CLI pokrenut. Unesite komandu (start, stop, status, map, export_map):");

        while (running.get()) {
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
                memory.getJobQueue().put(new MapJob("map-job"));
                break;
            case "EXPORTMAP":
                handleExportMapCommand(args);
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

        memory.getJobQueue().put(new ScanJob(jobName, minTemp, maxTemp, letter.charAt(0), output));
    }

    /**
     * Metoda koja vraca status odredjenog posla
     *
     * @param args Argumenti koji su dati
     */
    private void handleStatusCommand(Map<String, String> args) {
        String jobName = parseStringArg(args, "job", "j");
        for (Job job : memory.getJobQueue()) {
            if (job.getName().equals(jobName)) {
                System.out.println("Current status: " + job.getJobStatus());
            }
        }
    }

    /**
     * Metod za gasenje programa
     *
     * @param args Argumenti koji su dati
     */
    private void handleShutdownCommand(Map<String, String> args) {
        stop();
        boolean saveJobs = args.containsKey("save-jobs") || args.containsKey("s");

        memory.getCli().stop();
        memory.getDirectoryMonitor().stop();
        memory.getJobDispatcher().stop();

        if (saveJobs) {
            System.out.println("Saving pending jobs...");

            File saveConfigFile = new File("load_config");
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveConfigFile))) {
                for (Job job : memory.getJobQueue()) {
                    if (job.getJobStatus() == JobStatus.PENDING) {
                        out.writeObject(job);
                    }
                }
                System.out.println("Pending jobs saved successfully.");
            } catch (IOException e) {
                System.out.println("Error saving jobs: " + e.getMessage());
            }
        }

        joinThread(memory.getDirectoryMonitorThread());
        joinThread(memory.getJobDispatcherThread());
        joinThread(memory.getPeriodicMonitorThread());

        System.out.println("Shutting down the program.");
        System.exit(0);
    }

    /**
     * Metod za pridruzivanje threadova
     *
     * @param thread Thread koji treba pridruziti trenutnom
     */
    private void joinThread(Thread thread) {
        try {
            if (thread != null && thread.isAlive()) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

        memory.setJobDispatcher(new JobDispatcher(4, 4, memory.getJobQueue()));
        memory.setDirectoryMonitor(new DirectoryMonitor(dirPath, memory.getJobQueue()));
        memory.setPeriodicReport(new PeriodicReport("log.csv"));

        Memory memory = Memory.getInstance();
        memory.setJobDispatcherThread(new Thread(memory.getJobDispatcher()));
        memory.setDirectoryMonitorThread(new Thread(memory.getDirectoryMonitor()));
        memory.setPeriodicMonitorThread(new Thread(memory.getPeriodicReport()));

        memory.getJobDispatcherThread().start();
        System.out.println("Started job dispatcher");
        memory.getDirectoryMonitorThread().start();
        System.out.println("Started directory monitor on path: " + dirPath);
        memory.getPeriodicMonitorThread().start();
        System.out.println("Started periodic monitor on path: " + "log.csv");

        if (loadJobs) {
            System.out.println("Loading saved jobs...");

            File loadConfigFile = new File("load_config");
            if (loadConfigFile.exists()) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(loadConfigFile))) {
                    CountDownLatch latch = new CountDownLatch(1);
                    int jobCount = 0;

                    while (true) {
                        try {
                            Job job = (Job) in.readObject();

                            boolean added = memory.getJobQueue().offer(job);
                            if (added) {
                                System.out.println("Loaded job: " + job);
                                jobCount++;
                            } else {
                                System.out.println("Failed to add job to the queue: " + job);
                            }
                        } catch (EOFException e) {
                            break;
                        } catch (ClassNotFoundException | IOException e) {
                            System.out.println("Error loading jobs: " + e.getMessage());
                            break;
                        }
                    }

                    if (jobCount > 0) {
                        latch = new CountDownLatch(jobCount);
                        System.out.println("Waiting for all jobs to be loaded...");
                    }

                    latch.await();
                    System.out.println("All jobs loaded, continuing execution.");
                } catch (IOException e) {
                    System.out.println("Error reading the load_config file: " + e.getMessage());
                }
            } else {
                System.out.println("No load_config file found.");
            }
        }
    }

    /**
     * Metod za pokretanje export komande
     *
     * @param args Argumenti koji su dati
     */
    private void handleExportMapCommand(Map<String, String> args) throws InterruptedException {
        String filePath = args.getOrDefault("file", "log.csv");

        File logFile = new File(filePath);

        if (!logFile.exists()) {
            try {
                boolean created = logFile.createNewFile();
                if (created) {
                    System.out.println("Fajl " + filePath + " je kreiran.");
                } else {
                    System.out.println("Fajl " + filePath + " već postoji.");
                }
            } catch (IOException e) {
                System.err.println("Greška prilikom kreiranja fajla: " + e.getMessage());
                return;
            }
        }

        memory.getJobQueue().put(new ExportMapJob("export-map-job", filePath));
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