package de.dbuss.tefcontrol.data.modules.adjustmentrefx.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;


@Getter
@Setter
public class AdjustmentsREFX {
//    @Id
//    @Column(name = "ID")
//    private int id;

    private String scenario;

    private String date;

    private String adjustmentType;

    private String authorizationGroup;

    private String companyCode;

    private String assetClass;

    private String vendor;

    private String profitCenter;

    private String leasePayments;

    private String leaseLiability;

    private String interest;

    private String rouCapex;

    private String rouDepreciation;

    private String comment;

    private Date loadDate;
}
