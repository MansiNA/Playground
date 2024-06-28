package de.dbuss.tefcontrol.data.modules.rosettamapping.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RosettaProductTLN {
    private int lfdNr;
    private String rosettaProduct;
    private String coOneContractType;
    private String user;
}
