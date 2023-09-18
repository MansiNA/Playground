package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;

import java.sql.Date;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJobEnabled() {
        return jobEnabled;
    }

    public void setJobEnabled(String jobEnabled) {
        this.jobEnabled = jobEnabled;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getJob_Activity() {
        return job_activity;
    }

    public void setJob_Activity(String jobactivity) {
        this.job_activity = jobactivity;
    }

    public String getDuration_Min() {
        return duration_Min;
    }

    public void setDuration_Min(String duration_Min) {
        this.duration_Min = duration_Min;
    }

    public String getJobStartDate() {
        return jobStartDate;
    }

    public void setJobStartDate(String jobStartDate) {
        this.jobStartDate = jobStartDate;
    }

    public String getJobLastExecutedStep() {
        return jobLastExecutedStep;
    }

    public void setJobLastExecutedStep(String jobLastExecutedStep) {
        this.jobLastExecutedStep = jobLastExecutedStep;
    }

    public String getJobExecutedStepDate() {
        return jobExecutedStepDate;
    }

    public void setJobExecutedStepDate(String jobExecutedStepDate) {
        this.jobExecutedStepDate = jobExecutedStepDate;
    }

    public String getJobStopDate() {
        return jobStopDate;
    }

    public void setJobStopDate(String jobStopDate) {
        this.jobStopDate = jobStopDate;
    }

    public String getJobNextRunDate() {
        return jobNextRunDate;
    }

    public void setJobNextRunDate(String jobNextRunDate) {
        this.jobNextRunDate = jobNextRunDate;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

}
