package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "project_parameter", schema = "dbo")
public class ProjectParameter {
    @Id
   // @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generated ID
    @Column(name = "id")
    private Integer id;

    @Column(name = "Namespace")
    private String namespace;

    @Column(name = "Name")
    private String name;

    @Column(name = "Value")
    private String value;

    @Column(name = "Description")
    private String description;


}
