package de.dbuss.tefcontrol.data.modules.saleschannelmapping.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class CLTVSalesChannel {
    private Integer lfdNr;
    private String salesChannelsCoOneBkId;
    private String salesChannelsCoOneId;
    private String salesChannelsName;
    private String salesChannelCltv;
    private String user;
    private LocalDate gueltigVon;
    private LocalDate gueltigBis;
}
