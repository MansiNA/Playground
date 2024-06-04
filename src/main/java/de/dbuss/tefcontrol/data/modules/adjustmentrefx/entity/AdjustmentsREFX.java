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

    private Date date;

    private String adjustmentType;

    private String authorizationGroup;

    private long companyCode;

    private long assetClass;

    private String vendor;

    private long profitCenter;

    private String leasePayments;

    private String leaseLiability;

    private String interest;

    private String rouCapex;

    private String rouDepreciation;

    private String comment;

    private Date loadDate;
}
