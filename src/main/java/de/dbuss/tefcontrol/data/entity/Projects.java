package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
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

    private String role_access;

    // Define a one-to-many relationship between Project and ProjectAttachments
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProjectAttachments> listOfAttachments = new ArrayList<>();

    // Define a one-to-many relationship between Projects and ProjectSql
    @OneToMany(mappedBy = "project", fetch = FetchType.EAGER, orphanRemoval = true)
    private List<ProjectSql> listOfSqls = new ArrayList<>();

    // Define a one-to-many relationship between Projects and ProjectSql
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProjectQSEntity> listOfProjectQs = new ArrayList<>();
}
