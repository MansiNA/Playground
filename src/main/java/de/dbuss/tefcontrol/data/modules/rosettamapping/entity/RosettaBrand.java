package de.dbuss.tefcontrol.data.modules.rosettamapping.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RosettaBrand {

    private int lfdNr;
    private String rosettaBrand;
    private String coOneSPS;
    private String user;

    @Override
    public String toString() {
        return "RosettaBrand{" +
                "lfdNr=" + lfdNr +
                ", rosettaBrand='" + rosettaBrand + '\'' +
                ", coOneSPS='" + coOneSPS + '\'' +
                ", user='" + user + '\'' +
                '}';
    }
}
