package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "dbo", name = "CLTV_HW_MEASURES")
public class CLTV_HW_Measures {

    @Id
    private Integer id;

    @NotNull
    private Integer monat_ID ;

    @NotEmpty
    private String device = "";

    @NotEmpty
    private String measure_Name = "";

    @NotEmpty
    private String channel = "";
    @NotEmpty
    private String value;
}
