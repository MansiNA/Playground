package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(schema = "dbo", name = "CLTV_HW_MEASURES")
public class CLTV_HW_Measures {

    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    private Integer monat_ID ;

    @NotEmpty
    private String device = "";

    @NotEmpty
    private String measure_Name = "";

    @NotEmpty
    private String channel = "";
    @NotEmpty
    private String value;

    public CLTV_HW_Measures(Integer id, Integer monat_id, String device, String measure_Name, String channel, String value) {
        this.id = id;
        this.monat_ID = monat_id;
        this.device = device;
        this.measure_Name = measure_Name;
        this.channel = channel;
        this.value = value;
    }

    public CLTV_HW_Measures() {

    }

    public Integer getMonat_ID() {
        return monat_ID;
    }

    public void setMonat_ID(Integer monat_id) {
        this.monat_ID = monat_id;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getMeasure_Name() {
        return measure_Name;
    }

    public void setMeasure_Name(String measure_Name) {
        this.measure_Name = measure_Name;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getValue() {
        //return value.toString();
        return value;
    }

    public void setValue(String value) {
        //      value = String.valueOf(Long.valueOf(value));
        this.value=value;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
