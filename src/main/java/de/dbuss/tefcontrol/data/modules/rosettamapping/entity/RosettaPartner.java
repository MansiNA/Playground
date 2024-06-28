package de.dbuss.tefcontrol.data.modules.rosettamapping.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RosettaPartner {

    private int lfdNr;
    private String rosettaPartner;
    private String coOneSPS;
    private String coOnePaymentType;
    private String user;

    // Optional: toString, equals, hashCode methods can be added if needed
    @Override
    public String toString() {
        return "RosettaPartner{" +
                "lfdNr=" + lfdNr +
                ", rosettaPartner='" + rosettaPartner + '\'' +
                ", coOneSPS='" + coOneSPS + '\'' +
                ", coOnePaymentType='" + coOnePaymentType + '\'' +
                ", user='" + user + '\'' +
                '}';
    }
}
