package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(schema = "dbo", name = "ProjectAttachments")
public class ProjectAttachments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generated ID
    private Long id;

    @NotEmpty
    private String description;

    @NotEmpty
    private String filename;

    @Lob
    private byte[] filecontent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Projects project;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
    public byte[] getFilecontent() {
        return filecontent;
    }
    public void setFilecontent(byte[] filecontent) {
        this.filecontent = filecontent;
    }
    public Projects getProject() {
        return project;
    }
    public void setProject(Projects project) {
        this.project = project;
    }

}

