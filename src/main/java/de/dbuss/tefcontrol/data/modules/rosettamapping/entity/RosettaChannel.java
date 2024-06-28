package de.dbuss.tefcontrol.data.modules.rosettamapping.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RosettaChannel {

    private int lfdNr;
    private String rosettaChannel;
    private String coOneChannel;
    private String user;

    // Optional: toString, equals, hashCode methods can be added if needed
    @Override
    public String toString() {
        return "RosettaChannel{" +
                "lfdNr=" + lfdNr +
                ", rosettaChannel='" + rosettaChannel + '\'' +
                ", coOneChannel='" + coOneChannel + '\'' +
                ", user='" + user + '\'' +
                '}';
    }
}
