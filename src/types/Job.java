package types;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
public abstract class Job implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final JobType type;
    private final String name;
    @Setter
    private JobStatus jobStatus;

    public Job(JobType type, String name) {
        this.type = type;
        this.name = name;
        this.jobStatus = JobStatus.PENDING;
    }

    public abstract void execute();
}