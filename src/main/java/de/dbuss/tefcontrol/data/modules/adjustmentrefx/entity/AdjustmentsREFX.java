package de.dbuss.tefcontrol.data.modules.adjustmentrefx.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;


@Entity
@Table(name = "adjustments_refx")
@Getter
@Setter
public class AdjustmentsREFX {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "Scenario")
    private String scenario;

    @Column(name = "Date")
    private String date;

    @Column(name = "Adjustment_Type")
    private String adjustmentType;

    @Column(name = "Authorization_Group")
    private String authorizationGroup;

    @Column(name = "Company_Code")
    private String companyCode;

    @Column(name = "Asset_Class")
    private String assetClass;

    @Column(name = "Vendor")
    private String vendor;

    @Column(name = "Profit_Center")
    private String profitCenter;

    @Column(name = "Lease_Payments")
    private String leasePayments;

    @Column(name = "Lease_Liability")
    private String leaseLiability;

    @Column(name = "Interest")
    private String interest;

    @Column(name = "ROU_Capex")
    private String rouCapex;

    @Column(name = "ROU_Depreciation")
    private String rouDepreciation;

    @Column(name = "Comment")
    private String comment;

    @Column(name = "Load_Date")
    private Date loadDate;
}
