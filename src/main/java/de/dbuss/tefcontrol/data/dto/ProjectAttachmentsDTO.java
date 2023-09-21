package de.dbuss.tefcontrol.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class ProjectAttachmentsDTO {
    private Long id;
    private String description;
    private String filename;
    private Date uploadDate;
    private Integer filesizeKb;
    private byte[] fileContent;
    private Long projectId;

    public ProjectAttachmentsDTO(Long id, String description, String filename, Date uploadDate, Integer filesizeKb) {
        this.id = id;
        this.description = description;
        this.filename = filename;
        this.uploadDate = uploadDate;
        this.filesizeKb = filesizeKb;
    }
}

