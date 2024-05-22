package de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class CasaTerm {
    @Id
    @Column(name = "ContractFeature_id")
    private Long contractFeatureId;

    @Column(name = "AttributeClasses_ID")
    private Long attributeClassesId;

    @Column(name = "AttributeClasses_NAME")
    private String attributeClassesName;

    @Column(name = "Connect_Type")
    private String connectType;

    @Column(name = "CF_TYPE_CLASS_NAME")
    private String cfTypeClassName;

    @Column(name = "Term_Name")
    private String termName;


    @Column(name = "CLTV_Category_Name")
    private String cltvCategoryName;

    @Column(name = "Controlling_Branding")
    private String controllingBranding;

    @Column(name = "CLTV_Charge_Name")
    private String cltvChargeName;
}
