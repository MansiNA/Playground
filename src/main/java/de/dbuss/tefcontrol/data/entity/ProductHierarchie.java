package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(schema = "dbo", name = "products")
public class ProductHierarchie {
    @Id
    private Long id;

    @NotEmpty
    private String pfg_Type = "";

    @NotEmpty
    private String node = "";

    @NotEmpty
    private String product_name = "";

    @NotEmpty
    private String exportTime_id = "";


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getpfg_Type() {
        return pfg_Type;
    }

    public void setpfg_Type(String pfg_Type) {
        this.pfg_Type = pfg_Type;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getProduct_name() {
        return product_name;
    }

    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    public String getExportTime_id() {
        return exportTime_id;
    }

    public void setExportTime_id(String exportTime_id) {
        this.exportTime_id = exportTime_id;
    }




}
