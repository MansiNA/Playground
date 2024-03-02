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

    @Column(name = "Connect_Type")
    private String connectType;
}
