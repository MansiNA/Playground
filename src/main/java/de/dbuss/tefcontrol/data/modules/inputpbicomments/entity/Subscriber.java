package de.dbuss.tefcontrol.data.modules.inputpbicomments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "Stage_CC_Comment", name = "Comments_Subscriber")
public class Subscriber {

    @Id
    private Integer row;
    @NotNull
    private Integer month;
    private String category;
    @Column(name = "payment_type")
    private String paymentType;
    private String segment;
    private String comment;
    private String Scenario;
    private String xtd;
}