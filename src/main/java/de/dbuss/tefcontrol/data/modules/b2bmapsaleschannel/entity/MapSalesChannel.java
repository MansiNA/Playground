package de.dbuss.tefcontrol.data.modules.b2bmapsaleschannel.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class MapSalesChannel {
    private static int nextId = 0;
    private int id;
    private String salesChannel;
    private String channel;
    private Date loadDate;

    public MapSalesChannel() {
        this.id = nextId++;
    }
}
