package de.dbuss.tefcontrol.data.modules.inputpbicomments.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenericComments {
    private int lineNumber;
    private String responsible;
    private String topic;
    private int month;
    private String category1;
    private String category2;
    private String scenario;
    private String xtd;
    private String segment;
    private String paymentType;
    private String comment;
    private String fileName;
    private String registerName;
}

