package com.microjet.airqi2;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by chang on 2017/12/9.
 */

public class AsmDataModel extends RealmObject {
    @PrimaryKey
    private int id;
    private String TEMPValue;
    private String HUMIValue;
    private String TVOCValue;
    private String ECO2Value;
    private Long Created_time;

    public Integer getDataId() {
        return id;
    }

    public void setData_id(Integer Data_id) {
        this.id = Data_id;
    }

    public String getTEMPValue() {
        return TEMPValue;
    }

    public void setTEMPValue(String TEMPValue) {
        this.TEMPValue = TEMPValue;
    }

    public String getHUMIValue() {
        return HUMIValue;
    }

    public void setHUMIValue(String HUMIValue) {
        this.HUMIValue = HUMIValue;
    }

    public String getTVOCValue() {
        return TVOCValue;
    }

    public void setTVOCValue(String TVOCValue) {
        this.TVOCValue = TVOCValue;
    }

    public String getECO2Value() {
        return ECO2Value;
    }

    public void setECO2Value(String ECO2Value) {
        this.ECO2Value = ECO2Value;
    }

    public Long getCreated_time() {
        return Created_time;
    }

    public void setCreated_time(Long Created_time) {
        this.Created_time = Created_time;
    }

}
