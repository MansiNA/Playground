package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "user_uploads", schema = "log")
public class ProjectUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Upload_ID")
    private Long uploadId;

    @Column(name = "Upload_Date")
    private Date uploadDate;

    @Column(name = "Modul_Name")
    private String modulName;

    @Column(name = "File_Name")
    private String fileName;

    @Column(name = "User_Name")
    private String userName;
}
