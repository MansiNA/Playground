package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "dbo", name = "Subscriber")
public class Subscriber {

    @Id
    private Integer row;
    @NotNull
    private Integer month;
    private String category;
    private String paymentType;
    private String segment;
    private String comment;
}