package de.dbuss.tefcontrol.data.modules.HUBFlowMapping.entity;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Getter
@Setter
public class HUBFlowMapping {

    private int zeile;

    private int hubMovementTypeDetailId;

    private String hubMovementTypeDetailName;

    private int flowL1Id;

    private String flowL1Name;

    private int flowL2Id;

    private String flowL2Name;

    private int flowL3Id;

    private String flowL3Name;

    private int hubFlowId;

    private String hubFlowName;

    private int sortHubFlowId;

    private int sortHubFlowName;

}
