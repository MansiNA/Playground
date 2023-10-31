package de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

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
    private String node = "PFG_";

   // @NotEmpty
    @Column(name="Product")
    private String product_name = "";

   // @NotEmpty
   // @Column(name="Export_Time")
   // private Date exportTime_id;

/*    public void setNode(String node) {
        if (node != null && !node.startsWith("PFG_")) {
            this.node = "PFG_" + node;
        } else {
            this.node = node;
        }
    }*/
}
