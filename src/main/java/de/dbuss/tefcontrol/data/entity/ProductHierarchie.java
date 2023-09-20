package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
