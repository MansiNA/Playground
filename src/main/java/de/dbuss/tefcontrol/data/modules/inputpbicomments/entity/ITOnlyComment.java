package de.dbuss.tefcontrol.data.modules.inputpbicomments.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ITOnlyComment {
    private int zeile;
    private String date;
    private String topic;
    private String category1;
    private String category2;
    private String comment;
    private String scenario;
    private String xtd;
    private Date loadDate;
}
