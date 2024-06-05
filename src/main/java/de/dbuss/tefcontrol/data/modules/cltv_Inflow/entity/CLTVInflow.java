package de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
//@Table(schema = "USR", name = "IN_FRONT_CLTV_Inflow")
public class CLTVInflow {

    @Id
    @Column(name = "ContractFeature_id")
    private Long contractFeatureId;

    @Column(name = "AttributeClasses_ID")
    private Long attributeClassesId;

    @Column(name = "CF_TYPE_CLASS_NAME")
    private String cfTypeClassName;

    @Column(name = "AttributeClasses_NAME")
    private String attributeClassesName;

    @Column(name = "ContractFeatureSubCategory_Name")
    private String contractFeatureSubCategoryName;

    @Column(name = "ContractFeature_Name")
    private String contractFeatureName;

    @Column(name = "CF_TYPE_NAME")
    private String cfTypeName;

    @Column(name = "CF_Duration_in_Month")
    private String cfDurationInMonth;

    @Column(name = "Connect_Type")
    private String connectType;

    @Column(name = "CLTV_Category_Name")
    private String cltvCategoryName;

  //  @Column(name = "Controlling_Branding_Detailed")
  //  private String controllingBrandingDetailed;

    @Column(name = "Controlling_Branding")
    private String controllingBranding;

    @Column(name = "CLTV_Charge_Name")
    private String cltvChargeName;

    @Column(name = "User")
    private String user;

    @Transient
    private String id;

    @PostLoad
    public void setIdFromFields() {
        // Concatenate values of contractFeatureId, attributeClassesId, and connectType
        this.id = contractFeatureId + "_" + attributeClassesId + "_" + connectType;
    }

}
