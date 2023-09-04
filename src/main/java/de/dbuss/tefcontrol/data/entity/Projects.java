package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

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

    // Define a one-to-many relationship between Project and ProjectAttachments
    @OneToMany(mappedBy = "project", fetch = FetchType.EAGER, orphanRemoval = true)
    private List<ProjectAttachments> listOfAttachments = new ArrayList<>();

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

    public List<ProjectAttachments> getProjectAttachments() {
        return listOfAttachments;
    }

    public void setAttachments(List<ProjectAttachments> attachments) {
        this.listOfAttachments = attachments;
    }
}
