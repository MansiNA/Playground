package de.dbuss.tefcontrol.data.modules.rosettamapping.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class RosettaProductUSG {
    private int lfdNr;
    private String rosettaProduct;
    private String coOneMeasure;
    private String user;
}
