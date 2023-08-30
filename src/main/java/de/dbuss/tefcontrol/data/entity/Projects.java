package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(schema = "dbo", name = "Projects")
public class Projects {
    @Id
    @NotNull
    private Long id;

    private Long parent_id;

    @NotNull
    private String name;

    @NotNull
    private String description;

    private String page_URL;

    private String agent_Jobs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParent_id() {
        return parent_id;
    }

    public void setParent_id(Long parent_id) {
        this.parent_id = parent_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPage_URL() {
        return page_URL;
    }

    public void setPage_URL(String page_URL) {
        this.page_URL = page_URL;
    }

    public String getAgentJobs() {
        return agent_Jobs;
    }

    public void setAgentJobs(String agentJobs) {
        this.agent_Jobs = agentJobs;
    }
}
