package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "project_uploads", schema = "dbo")
public class ProjectUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Upload_ID")
    private Long uploadId;

    @Column(name = "Upload_Date")
    private Date uploadDate;

    @Column(name = "File_Name")
    private String fileName;

    @Column(name = "User_Name")
    private String userName;
}
