package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Getter
@Setter
@Entity
@Table(schema = "dbo", name = "job_status")
public class AgentJobs {

    @Id
    private String name;

    private String jobEnabled;

    private String jobDescription;

    private String job_activity;

    private String duration_Min;

    private String jobStartDate;

    private String jobLastExecutedStep;

    private String jobExecutedStepDate;

    private String jobStopDate;
    private String jobNextRunDate;
    private String result;
    private String job_id;

}
