package com.ming.inclination.service;

import com.ming.inclination.entity.TblDataOffset;
import com.ming.inclination.entity.TblDataOffsetToOrcl;
import com.ming.inclination.entity.TblFilterOffset;
import com.ming.inclination.entity.TblOriginOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Service
public class DataService {

    @Value("${oracleUrl}")
    private String oracleUrl;//oracle数据库地址
    @Value("${oracleUsername}")
    private String oracleUsername;//用户名
    @Value("${oraclePassword}")
    private String oraclePassword;//密码

    private final String mySqlUrl = "jdbc:mysql://localhost:3306/inclination?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false";
    private final String mySqlUsername = "root";
    private final String mySqlPassword = "root";

    public void insertAll(List<TblDataOffsetToOrcl> offsetList) {
        try {
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            Class.forName("oracle.jdbc.driver.OracleDriver");
            String sql = "INSERT INTO TBL_ORIGIN_OFFSET(ID,BRIDGE_ID,MEASURE_POINT,OFFSET,AC_TIME) VALUES(?,?,?,?,?)";

            try (Connection connection = DriverManager.getConnection(oracleUrl, oracleUsername, oraclePassword);
                 PreparedStatement preStat = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);

                for (int i = 0; i < offsetList.size(); i++) {
                    TblDataOffsetToOrcl offset = offsetList.get(i);
                    preStat.setString(1, offset.getId());
                    preStat.setString(2, offset.getBridgeId());
                    preStat.setString(3, offset.getMeasurePoint());
                    preStat.setDouble(4, offset.getOffset());
                    preStat.setTimestamp(5, offset.getAcTime());
                    preStat.addBatch();
                    if (i % 1000 == 0) {
                        preStat.executeBatch();
                        preStat.clearBatch();
                    }
                }
                preStat.executeBatch();
                connection.commit();
                System.out.println("存入ORACLE数据库: " + offsetList.size());
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertAllOrigin(List<TblOriginOffset> originList) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String sql = "INSERT INTO TBL_ORIGIN_OFFSET(ID,CAN_ID,DATA,VALUEX,VALUEY,DATA_TIME) VALUES(?,?,?,?,?,?)";

            try (Connection connection = DriverManager.getConnection(mySqlUrl, mySqlUsername, mySqlPassword);
                 PreparedStatement preStat = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);

                for (int i = 0; i < originList.size(); i++) {
                    TblOriginOffset offset = originList.get(i);
                    preStat.setString(1, offset.getId());
                    preStat.setString(2, offset.getCanId());
                    preStat.setString(3, offset.getData());
                    preStat.setDouble(4, offset.getValueX());
                    preStat.setDouble(5, offset.getValueY());
                    preStat.setString(6, offset.getDataTime());
                    preStat.addBatch();
                    if (i % 1000 == 0) {
                        preStat.executeBatch();
                        preStat.clearBatch();
                    }
                }
                preStat.executeBatch();
                connection.commit();
                System.out.println("存入TBL_ORIGIN_OFFSET: " + originList.size());
            }

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertAllFilter(List<TblFilterOffset> offsetList) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String sql = "INSERT INTO TBL_FILTER_OFFSET(ID,CAN_ID,DATA_TIME,ORI_VALUEX,ORI_VALUEY,VALUEX,VALUEY) " +
                    "VALUES(?,?,?,?,?,?,?)";

            try (Connection connection = DriverManager.getConnection(mySqlUrl, mySqlUsername, mySqlPassword);
                 PreparedStatement preStat = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);

                for (int i = 0; i < offsetList.size(); i++) {
                    TblFilterOffset offset = offsetList.get(i);
                    preStat.setString(1, offset.getId());
                    preStat.setString(2, offset.getCanId());
                    preStat.setString(3, offset.getDataTime());
                    preStat.setDouble(4, offset.getOriValueX());
                    preStat.setDouble(5, offset.getOriValueY());
                    preStat.setDouble(6, offset.getValueX());
                    preStat.setDouble(7, offset.getValueY());
                    preStat.addBatch();
                    if (i % 1000 == 0) {
                        preStat.executeBatch();
                        preStat.clearBatch();
                    }
                }
                preStat.executeBatch();
                connection.commit();
                System.out.println("存TBL_FILTER_OFFSET: " + offsetList.size());
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertAllData(List<TblDataOffset> offsetList) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String sql = "INSERT INTO TBL_DATA_OFFSET(ID,AC_TIME,BRIDGE_ID,MEASURE_POINT,MIN_ZONE,OFFSET,UPLOADED) " +
                    "VALUES(?,?,?,?,?,?,?)";

            try (Connection connection = DriverManager.getConnection(mySqlUrl, mySqlUsername, mySqlPassword);
                 PreparedStatement preStat = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);

                for (int i = 0; i < offsetList.size(); i++) {
                    TblDataOffset offset = offsetList.get(i);
                    preStat.setString(1, offset.getId());
                    preStat.setString(2, offset.getAcTime());
                    preStat.setString(3, offset.getBridgeId());
                    preStat.setString(4, offset.getMeasurePoint());
                    preStat.setString(5, offset.getMinZone());
                    preStat.setDouble(6, offset.getOffset());
                    preStat.setString(7, offset.getUploaded());
                    preStat.addBatch();
                    if (i % 1000 == 0) {
                        preStat.executeBatch();
                        preStat.clearBatch();
                    }
                }
                preStat.executeBatch();
                connection.commit();
                System.out.println("存TBL_DATA_OFFSET: " + offsetList.size());
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

}
