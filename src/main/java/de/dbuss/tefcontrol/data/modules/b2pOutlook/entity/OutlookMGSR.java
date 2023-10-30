package de.dbuss.tefcontrol.data.modules.b2pOutlook.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class OutlookMGSR {
        private int zeile;
        private int month;
        private String pl_Line;
        private long profitCenter;
        private String scenario;
        private String block;
        private String segment;
        private String paymentType;
        private String typeOfData;
        private long value;
        private String blatt;
        private Date loadDate;

        // Constructors, getters, and setters here
}

