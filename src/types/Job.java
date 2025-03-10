package types;

import lombok.Getter;

@Getter
public abstract class Job {
    private final JobType type;
    private final String name;

    public Job(JobType type, String name) {
        this.type = type;
        this.name = name;
    }

    public abstract void execute();
}