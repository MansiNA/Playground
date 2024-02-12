package de.dbuss.tefcontrol.data.modules.administration.entity;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurrentScenarios {
    @Id
    private Integer upload_ID;

    private String current_Plan;
    private String current_Outlook;
    private String prior_Outlook;
    private String current_QFC;

}