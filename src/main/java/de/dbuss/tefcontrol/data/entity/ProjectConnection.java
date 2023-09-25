package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(schema = "dbo", name = "project_connections")
public class ProjectConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotEmpty
    private String name;
    @NotEmpty
    private String dbName;
    @NotNull
    private String description;
    @NotEmpty
    private String hostname;
    @NotEmpty
    private String username;
    @NotEmpty
    private String password;

    // Define a one-to-many relationship between ProjectConnection and ProjectSql
    @OneToMany(mappedBy = "projectConnection", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProjectSql> listOfSqls;
}
