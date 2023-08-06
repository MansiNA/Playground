package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.repository.CLTV_HW_MeasuresRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class CLTV_HW_MeasureService {


    private final CLTV_HW_MeasuresRepository cLTVHwMeasuresRepository;
    @Autowired
    private JdbcTemplate template;

    public CLTV_HW_MeasureService(CLTV_HW_MeasuresRepository cLTV_HW_MeasuresRepository) {

        cLTVHwMeasuresRepository = cLTV_HW_MeasuresRepository;
    }

    public List<CLTV_HW_Measures> findAllProducts(String stringFilter) {
        if (stringFilter == null || stringFilter.isEmpty()) {


            return cLTVHwMeasuresRepository.findAll();
        } else {
            return cLTVHwMeasuresRepository.search(stringFilter);
        }
    }

    public List<CLTV_HW_Measures> findProductsbyMonat(String stringFilter) {

        if (stringFilter == null || stringFilter.isEmpty() || stringFilter.contains("kein Filter")) {
            return cLTVHwMeasuresRepository.findAll();
        }

        Integer intFilter=Integer.parseInt(stringFilter);

        if (intFilter == null || intFilter==0) {
            System.out.println("Filter ist null!!");
            return cLTVHwMeasuresRepository.findAll();
        } else {
            return cLTVHwMeasuresRepository.searchMonat(intFilter);
        }
    }


    public List<String> getMonate() {
        final List<String> Monate = new ArrayList<>();
        Monate.add("kein Filter");

        try {
            // Verbindung zur Datenbank herstellen
            Connection connection = DriverManager.getConnection("jdbc:sqlserver://192.168.58.130;databaseName=TEF;encrypt=true;trustServerCertificate=true", "dwhflex", "dwhflex");
            Statement statement = connection.createStatement();

            // SQL-Abfrage ausführen
            ResultSet resultSet = statement.executeQuery("select distinct monat_id from TEF.dbo.cltv_hw_measures");

            // Ergebnisse in die Liste einfügen
            while (resultSet.next()) {
                Monate.add(resultSet.getString("monat_id"));
            }

            // Verbindung schließen
            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Monate;
    }


    public void update(CLTV_HW_Measures currow, String s) {

        //  cLTVHwMeasuresRepository.save(currow);

        try {
            // Verbindung zur Datenbank herstellen
            Connection connection = DriverManager.getConnection("jdbc:sqlserver://192.168.58.130;databaseName=TEF;encrypt=true;trustServerCertificate=true", "dwhflex", "dwhflex");
            Statement statement = connection.createStatement();

            // SQL-Abfrage ausführen
            statement.execute("update TEF.dbo.cltv_hw_measures set value =" + s + " where id=" + currow.getId());

            // Verbindung schließen

            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }






}
