package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "project_qs", schema = "dbo")
public class ProjectQSEntity {

    @Id
    private int id;
    private Date create_date;
    private String description;
    private String name;
    private String sql;
    private int qs_id;
    private int qs_group;
    private String result;

}
