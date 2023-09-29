package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(schema = "dbo", name = "project_sqls")
public class ProjectSql {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Many ProjectSql entities can belong to one Projects entity
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private Projects project;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER) // Many ProjectSql entities can belong to one ProjectConnection entity
    @JoinColumn(name = "connection_id", referencedColumnName = "id")
    private ProjectConnection projectConnection;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "sql")
    private String sql;

    @Column(name = "create_date")
    private Date createDate;
}
