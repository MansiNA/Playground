package de.dbuss.tefcontrol.data.modules.inputpbicomments.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlashFinancials {
    private int zeile;
    private int month;
    private String category;
    private String comment;
    private String scenario;
    private String xtd;
}
