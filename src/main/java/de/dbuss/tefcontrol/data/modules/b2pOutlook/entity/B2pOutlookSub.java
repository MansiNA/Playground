package de.dbuss.tefcontrol.data.modules.b2pOutlook.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class B2pOutlookSub {
        private int zeile;
        private int month;
        private String scenario;
        private String measure;
        private String physicalsLine;
        private String segment;
        private String paymentType;
        private String contractType;
        private double value;
        private String blatt;

}

