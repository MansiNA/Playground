package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "dbo", name = "IN_FRONT_CLTV_Product_Hier_PFG")
public class ProductHierarchie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Column(name="PFG_PO/PP")
    private String pfg_Type = "";

    @NotEmpty
    @Column(name="Knoten")
    private String node = "";

    @NotEmpty
    @Column(name="Product")
    private String product_name = "";

    @NotEmpty
    @Column(name="Export_Time")
    private String exportTime_id = "";
}
