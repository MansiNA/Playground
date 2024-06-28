package de.dbuss.tefcontrol.data.modules.rosettamapping.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RosettaPaymentType {

    private int lfdNr;
    private String rosettaPaymentType;
    private String coOnePaymentType;
    private String user;

    // Optional: toString, equals, hashCode methods can be added if needed
    @Override
    public String toString() {
        return "RosettaPaymentType{" +
                "lfdNr=" + lfdNr +
                ", rosettaPaymentType='" + rosettaPaymentType + '\'' +
                ", coOnePaymentType='" + coOnePaymentType + '\'' +
                ", user='" + user + '\'' +
                '}';
    }
}
