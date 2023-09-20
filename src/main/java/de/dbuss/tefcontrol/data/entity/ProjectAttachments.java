package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
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

    private Date upload_date;

    @Lob
    private byte[] filecontent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    private Projects project;

    private Integer filesizekb;
}

