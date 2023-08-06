package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(schema = "dbo", name = "CLTV_HW_MEASURES")
public class CLTV_HW_Measures {

    @Id
    private Long id;

    @NotEmpty
    private Integer monat_ID ;

    @NotEmpty
    private String device = "";

    @NotEmpty
    private String measure_Name = "";

    @NotEmpty
    private String channel = "";
    @NotEmpty
    private Long value;

    public Integer getMonat_ID() {
        return monat_ID;
    }

    public void setMonat_ID(Integer monat_ID) {
        monat_ID = monat_ID;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        device = device;
    }

    public String getMeasure_Name() {
        return measure_Name;
    }

    public void setMeasure_Name(String measure_Name) {
        measure_Name = measure_Name;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        channel = channel;
    }

    public String getValue() {
        return value.toString();
    }

    public void setValue(String value) {
        value = String.valueOf(Long.valueOf(value));
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }


}
