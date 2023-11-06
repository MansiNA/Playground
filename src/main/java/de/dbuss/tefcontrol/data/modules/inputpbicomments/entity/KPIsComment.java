package de.dbuss.tefcontrol.data.modules.inputpbicomments.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class KPIsComment {
    private int zeile;
    private String date;
    private String topic;
    private String category1;
    private String category2;
    private String comment;
    private String planScenario;
    private Date loadDate;
}