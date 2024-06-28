package de.dbuss.tefcontrol.data.modules.rosettamapping.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RosettaUsageDirection {
    private int lfdNr;
    private String rosettaUsageDirection;
    private String coOneUsageDirection;
    private String user;
}
