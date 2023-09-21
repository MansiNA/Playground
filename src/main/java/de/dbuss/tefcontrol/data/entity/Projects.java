package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
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

    private String agent_db;

    // Define a one-to-many relationship between Project and ProjectAttachments
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProjectAttachments> listOfAttachments = new ArrayList<>();
}
