package com.example.sqlserverquery;

import android.util.Log;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class sqlDB {

    public float[] query(String name, Connection m_con) throws Exception {
        //PreparedStatement pt = null;
        Statement pt = m_con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
        String q = "SELECT accx,accy,accz FROM " + name;
        //pt = m_con.prepareStatement(q);
        ResultSet rs = pt.executeQuery(q);
        rs.last();
        //rs.previous();
        float PositionData[]=new float[3];
        PositionData[0] = rs.getFloat("accx");
        PositionData[1] = rs.getFloat("accy");
        PositionData[2] = rs.getFloat("accz");

        rs.close();
        pt.close();
        Log.d("数据表数据", "获取成功");
        return PositionData;
    }

    public List<String> update(Connection m_con)throws Exception{
        List<String> fList = new ArrayList<String>();
        Statement pt = m_con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
        String q = "SELECT name FROM sysobjects where xtype='U'";
        ResultSet rs = pt.executeQuery(q);
        while(rs.next()){
            fList.add(rs.getString("name"));
        }

        rs.close();
        pt.close();
        Log.d("数据表名称", "获取成功");
        return fList;
    }
}
