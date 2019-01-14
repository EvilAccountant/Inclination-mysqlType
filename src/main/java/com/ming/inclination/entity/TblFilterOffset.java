package com.ming.inclination.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
public final class TblFilterOffset {

    @Id
    @Column(length = 32)
    private String id;

    private String canId;

    private String dataTime;

    private double oriValueX;

    private double oriValueY;

    private double valueX;

    private double valueY;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCanId() {
        return canId;
    }

    public void setCanId(String canId) {
        this.canId = canId;
    }

    public String getDataTime() {
        return dataTime;
    }

    public void setDataTime(String dataTime) {
        this.dataTime = dataTime;
    }

    public double getOriValueX() {
        return oriValueX;
    }

    public void setOriValueX(double oriValueX) {
        this.oriValueX = oriValueX;
    }

    public double getOriValueY() {
        return oriValueY;
    }

    public void setOriValueY(double oriValueY) {
        this.oriValueY = oriValueY;
    }

    public double getValueX() {
        return valueX;
    }

    public void setValueX(double valueX) {
        this.valueX = valueX;
    }

    public double getValueY() {
        return valueY;
    }

    public void setValueY(double valueY) {
        this.valueY = valueY;
    }

    @Override
    public String toString() {
        return canId+" "+dataTime+" "+oriValueX+" "+valueX+" "+oriValueY+" "+valueY+"\n";
    }
}
