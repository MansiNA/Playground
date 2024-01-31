package de.dbuss.tefcontrol.data.modules.tarifmapping.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class CLTVProduct {

    // private int id;
    private String node;
    private String child;
    private String cltvTarif;
    private String productType;
    private String user;
    private Date verarbDatum;

}
