package de.dbuss.tefcontrol.data.entity;

import de.dbuss.tefcontrol.data.entity.Projects;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "project_qs", schema = "dbo")
public class ProjectQSEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Date create_date;
    private String description;
    private String name;
    private String sql;
    @Transient
    private String result;

    @ManyToOne(fetch = FetchType.LAZY) // Many ProjectSql entities can belong to one Projects entity
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private Projects project;


}
