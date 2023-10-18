package de.dbuss.tefcontrol.data.modules.inputPBIComments.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "Stage_CC_Comment", name = "Comments_UnitsDeepDive")
public class UnitsDeepDive {

    @Id
    private Integer row;
    @NotNull
    private Integer month;
    private String segment;
    private String category;
    private String comment;
    private String scenario;
    private String xtd;
}