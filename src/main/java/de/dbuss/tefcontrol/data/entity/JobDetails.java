package de.dbuss.tefcontrol.data.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class JobDetails {
    private int step_id;
    private String stepName;
    private String lastRunStartDateTime;
    private String lastRunFinishDateTime;
    private int lastRunDurationSeconds;
    //private int run_date;
    //private String run_time;
    private String message;
}
