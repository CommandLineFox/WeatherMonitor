package threads;

import jobs.*;
import utility.Memory;
import types.Job;
import types.JobStatus;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class Cli implements Runnable {
    private final Memory memory;

    public Cli() {
        this.memory = Memory.getInstance();
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("CLI started. use commands (start, stop, status, map, export_map):");

        while (Memory.getInstance().getRunning().get()) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            try {
                handleCommand(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
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
                throw new IllegalArgumentException("Unknown command: " + command);
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
            if (token.startsWith("--") || token.startsWith("-")) {
                key = token.startsWith("--") ? token.substring(2) : token.substring(1);
                args.put(key, "");
            } else {
                if (key == null) {
                    throw new IllegalArgumentException("Neispravan format argumenta: " + token);
                }
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
        Job job = memory.getJobHistory().get(jobName);

        if (job == null) {
            System.out.println("Couldn't find that job");
            return;
        }

        System.out.println(jobName + " is currently " + job.getJobStatus());
    }

    /**
     * Metod za gasenje programa
     *
     * @param args Argumenti koji su dati
     */
    private void handleShutdownCommand(Map<String, String> args) {
        boolean saveJobs = args.containsKey("save-jobs") || args.containsKey("s");

        if (saveJobs) {
            System.out.println("Saving pending jobs...");
            File saveConfigFile = new File("load_config");

            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveConfigFile))) {
                memory.getJobHistory().forEach((name, job) -> {
                    try {
                        if (job.getJobStatus() != JobStatus.COMPLETED) {
                            out.writeObject(job);
                            System.out.println("Job " + name + " saved");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                System.out.println("Pending jobs saved successfully.");
            } catch (IOException e) {
                System.out.println("Error saving jobs: " + e.getMessage());
            }
        }

        memory.getRunning().set(false);

        interruptThread(memory.getDirectoryMonitorThread());
        interruptThread(memory.getJobDispatcherThread());
        interruptThread(memory.getPeriodicMonitorThread());
        interruptThread(memory.getCliThread());

        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }

        System.out.println("Shutting down the program.");
        System.exit(0);
    }

    /**
     * Metod za prekidanje threadova tokom gasenja
     *
     * @param thread Thread koji treba da se prekine
     */
    private void interruptThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
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

        if (loadJobs) {
            System.out.println("Loading saved jobs...");

            File loadConfigFile = new File("load_config");
            if (loadConfigFile.exists()) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(loadConfigFile))) {
                    int jobCount = 0;
                    List<Job> loadedJobs = new ArrayList<>();

                    while (true) {
                        try {
                            Job job = (Job) in.readObject();
                            loadedJobs.add(job);
                            jobCount++;
                        } catch (EOFException e) {
                            break;
                        } catch (ClassNotFoundException | IOException e) {
                            System.out.println("Error loading jobs: " + e.getMessage());
                            break;
                        }
                    }

                    if (jobCount > 0) {
                        CountDownLatch latch = new CountDownLatch(jobCount);
                        System.out.println("Waiting for all jobs to be loaded...");

                        for (Job job : loadedJobs) {
                            boolean added = memory.getJobQueue().offer(job);
                            if (added) {
                                System.out.println("Loaded job: " + job);
                                latch.countDown();
                            } else {
                                System.out.println("Failed to add job to the queue: " + job);
                            }
                        }

                        latch.await();
                        System.out.println("All jobs loaded, continuing execution.");
                    } else {
                        System.out.println("No jobs to load.");
                    }
                } catch (IOException e) {
                    System.out.println("Error reading the load_config file: " + e.getMessage());
                }
            } else {
                System.out.println("No load_config file found.");
            }
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the watch directory path: ");
        String dirPath = scanner.nextLine();

        memory.getRunning().set(true);
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
                    System.out.println("File " + filePath + " has been created.");
                } else {
                    System.out.println("File " + filePath + " already exists.");
                }
            } catch (IOException e) {
                System.err.println("Error during creating file: " + e.getMessage());
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
        if (value == null) throw new IllegalArgumentException("Missing argument: --" + longOpt);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for --" + longOpt + ": " + value);
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
        if (value == null) throw new IllegalArgumentException("Missing argument: --" + longOpt);
        return value;
    }
}