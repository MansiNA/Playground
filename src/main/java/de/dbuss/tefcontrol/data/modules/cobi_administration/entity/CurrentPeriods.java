package de.dbuss.tefcontrol.data.modules.cobi_administration.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurrentPeriods {
    @Id
    private Integer Upload_ID;
    @NotNull
    private String current_month;
  //  private String preliminary_month;

}