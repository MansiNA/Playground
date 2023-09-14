package de.dbuss.tefcontrol.data.service;


import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.repository.CLTV_HW_MeasuresRepository;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Service
public class CLTV_HW_MeasureService {
    private final CLTV_HW_MeasuresRepository repository;

    public CLTV_HW_MeasureService(CLTV_HW_MeasuresRepository repository) {
        this.repository = repository;
    }

    public Optional<CLTV_HW_Measures> get(Long id) {
        return repository.findById(id);
    }

    public CLTV_HW_Measures update(CLTV_HW_Measures entity) { return repository.save(entity);}
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<CLTV_HW_Measures> findAll() {
        return repository.findAll();
    }

    public Optional<CLTV_HW_Measures> findById(long id) {
        return repository.findById(id);
    }

    public void saveAll(List<CLTV_HW_Measures> elaFavoritenListe) {
        repository.saveAll(elaFavoritenListe);
    }

    public List<CLTV_HW_Measures> findAllProducts(String stringFilter) {
        if (stringFilter == null || stringFilter.isEmpty()) {
            return repository.findAll();
        } else {
            return repository.search(stringFilter);
        }
    }

   /* public List<String> getMonate() {
        final List<String> Monate = new ArrayList<>();
        Monate.add("kein Filter");

        try {
            // Verbindung zur Datenbank herstellen
            Connection connection = DriverManager.getConnection("jdbc:sqlserver://128.140.47.43;databaseName=PIT;encrypt=true;trustServerCertificate=true", "PIT", "PIT!20230904");
            Statement statement = connection.createStatement();

            // SQL-Abfrage ausführen
            ResultSet resultSet = statement.executeQuery("select distinct monat_id from PIT.dbo.cltv_hw_measures");

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
    }*/


    public void update(CLTV_HW_Measures currow, String s) {

        //  cLTVHwMeasuresRepository.save(currow);

        try {
            // Verbindung zur Datenbank herstellen
            Connection connection = DriverManager.getConnection("jdbc:sqlserver://128.140.47.43;databaseName=PIT;encrypt=true;trustServerCertificate=true", "PIT", "PIT!20230904");
            Statement statement = connection.createStatement();

            // SQL-Abfrage ausführen
            statement.execute("update PIT.dbo.cltv_hw_measures set value =" + s + " where id=" + currow.getId());

            // Verbindung schließen

            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
