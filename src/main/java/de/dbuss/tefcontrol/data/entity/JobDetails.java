package de.dbuss.tefcontrol.data.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class JobDetails {
    private int run_date;
    private String run_time;
    private String message;
}
