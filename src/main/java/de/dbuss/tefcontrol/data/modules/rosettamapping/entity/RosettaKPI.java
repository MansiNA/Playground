package de.dbuss.tefcontrol.data.modules.rosettamapping.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RosettaKPI {

    private int lfdNr;
    private String rosettaKPI;
    private String coOneMeasure;
    private String user;

    // Optional: toString, equals, hashCode methods can be added if needed
    @Override
    public String toString() {
        return "RosettaKPI{" +
                "lfdNr=" + lfdNr +
                ", rosettaKPI='" + rosettaKPI + '\'' +
                ", coOneMeasure='" + coOneMeasure + '\'' +
                ", user='" + user + '\'' +
                '}';
    }
}
