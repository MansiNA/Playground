package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.repository.ProjectConnectionRepository;
import de.dbuss.tefcontrol.views.Tech_KPIView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.*;

@Slf4j
@Service
public class ProjectConnectionService {
    private final ProjectConnectionRepository repository;

    private JdbcTemplate jdbcTemplate;

    public ProjectConnectionService(ProjectConnectionRepository repository) {
        this.repository = repository;
    }

    public Optional<ProjectConnection> findByName(String name) {
        return repository.findByName(name);
    }

    public List<ProjectConnection> findAll() {
        return repository.findAll();
    }

    @Primary
    public DataSource getDataSource(String selectedDatabase) {

        // Load database connection details from the ProjectConnection entity
        Optional<ProjectConnection> projectConnection = repository.findByName(selectedDatabase);

        if (projectConnection.isPresent()) {
            System.out.println("jdbc:sqlserver://"+projectConnection.get().getHostname() + ";databaseName="+projectConnection.get().getDbName()+";encrypt=true;trustServerCertificate=true");
            DataSource dataSource = DataSourceBuilder
                    .create()
                    .url("jdbc:sqlserver://"+projectConnection.get().getHostname() + ";databaseName="+projectConnection.get().getDbName()+";encrypt=true;trustServerCertificate=true")
                    .username(projectConnection.get().getUsername())
                    .password(projectConnection.get().getPassword())
                    .build();
            return dataSource;
        }

        throw new RuntimeException("Database connection not found: " + selectedDatabase);
    }

    public List<CLTV_HW_Measures> fetchDataFromDatabase(String selectedDatabase) {
        DataSource dataSource = getDataSource(selectedDatabase);
        jdbcTemplate = new JdbcTemplate(dataSource);

        String sqlQuery = "SELECT * FROM CLTV_HW_Measures";

        // Create a RowMapper to map the query result to a CLTV_HW_Measures object
        RowMapper<CLTV_HW_Measures> rowMapper = (rs, rowNum) -> {
            CLTV_HW_Measures measure = new CLTV_HW_Measures();
            measure.setId(rs.getInt("id"));
            measure.setMonat_ID(rs.getInt("monat_id"));
            measure.setDevice(rs.getString("device"));
            measure.setMeasure_Name(rs.getString("measure_name"));
            measure.setChannel(rs.getString("channel"));
            measure.setValue(rs.getString("value"));
            return measure;
        };

        List<CLTV_HW_Measures> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper);

        return fetchedData;
    }

    public String write2DB(List<CLTV_HW_Measures> data, String selectedDatabase) {

        try {
            DataSource dataSource = getDataSource(selectedDatabase);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM CLTV_HW_Measures";
            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO CLTV_HW_Measures (id, monat_id, device, measure_name, channel, value) VALUES (?, ?, ?, ?, ?, ?)";

            // Loop through the data and insert new records
            for (CLTV_HW_Measures item : data) {
                jdbcTemplate.update(
                        sqlInsert,
                        item.getId(),
                        item.getMonat_ID(),
                        item.getDevice(),
                        item.getMeasure_Name(),
                        item.getChannel(),
                        item.getValue()
                );
            }
            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during update: " + e.getMessage();
        }
    }

    public List<LinkedHashMap<String, Object>> getDataFromDatabase(String selectedDatabase, String query) {
        log.info("Starting getDataFromDatabase() for SQL: {}", query);
        List<LinkedHashMap<String, Object>> rows = new LinkedList<>();
        DataSource dataSource = getDataSource(selectedDatabase);
        jdbcTemplate = new JdbcTemplate(dataSource);

        ResultSetExtractor<List<LinkedHashMap<String, Object>>> resultSetExtractor = resultSet -> {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();
            List<String> columnNames = new ArrayList<>();

            for (int index = 1; index <= columnCount; index++) {
                columnNames.add(resultSetMetaData.getColumnName(index));
            }

            while (resultSet.next()) {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                for (String columnName : columnNames) {
                    Object columnValue = resultSet.getObject(columnName) == null ? "" : String.valueOf(resultSet.getObject(columnName));
                    row.put(columnName, columnValue);
                }
                rows.add(row);
            }
            return rows;
        };

        List<LinkedHashMap<String, Object>> result = jdbcTemplate.query(query, resultSetExtractor);

        log.info("Ending getDataFromDatabase() for SQL "+ rows.size());
        return result;
    }

    public String saveFinancials(List<Financials> data, String selectedDatabase) {

        try {
            DataSource dataSource = getDataSource(selectedDatabase);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM Stage_CC_Comment.Comments_Financials";

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO Stage_CC_Comment.Comments_Financials (zeile, month, category, comment, scenario, xtd) VALUES (?, ?, ?, ?, ?, ?)";

            // Loop through the data and insert new records
            for (Financials item : data) {
                jdbcTemplate.update(
                        sqlInsert,
                        item.getRow(),
                        item.getMonth(),
                        item.getCategory(),
                        item.getComment(),
                        item.getScenario(),
                        item.getXtd()
                );
            }
            return "ok";
        }
           catch (CannotGetJdbcConnectionException connectionException) {
            return connectionException.getMessage();
        }
           catch (Exception e) {
                e.printStackTrace();
            return e.getMessage();
        }
    }

    public String saveSubscriber(List<Subscriber> data, String selectedDatabase) {

        try {

            DataSource dataSource = getDataSource(selectedDatabase);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM Stage_CC_Comment.Comments_Subscriber";

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO Stage_CC_Comment.Comments_Subscriber (zeile, month, category, payment_type, segment, comment, scenario, xtd) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            // Loop through the data and insert new records
            for (Subscriber item : data) {
                jdbcTemplate.update(
                        sqlInsert,
                        item.getRow(),
                        item.getMonth(),
                        item.getCategory(),
                        item.getPaymentType(),
                        item.getSegment(),
                        item.getComment(),
                        item.getScenario(),
                        item.getXtd()
                );
            }
            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during update: " + e.getMessage();
        }
    }

    public String saveUnitsDeepDive(List<UnitsDeepDive> data, String selectedDatabase) {

        try {

            DataSource dataSource = getDataSource(selectedDatabase);
            jdbcTemplate = new JdbcTemplate(dataSource);
          
            String sqlDelete = "DELETE FROM Stage_CC_Comment.Comments_UnitsDeepDive";

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO Stage_CC_Comment.Comments_UnitsDeepDive (zeile, month,segment, category, comment, scenario, xtd) VALUES (?, ?, ?, ?, ?, ?, ?)";

            // Loop through the data and insert new records
            for (UnitsDeepDive item : data) {
                jdbcTemplate.update(
                        sqlInsert,
                        item.getRow(),
                        item.getMonth(),
                        item.getSegment(),
                        item.getCategory(),
                        item.getComment(),
                        item.getScenario(),
                        item.getXtd()
                );
            }
            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error during update: " + e.getMessage();
        }
    }

    public String saveKPIFact(List<Tech_KPIView.KPI_Fact> data, String selectedDatabase) {

        try {
            DataSource dataSource = getDataSource(selectedDatabase);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM [Stage_Tech_KPI].[KPI_Fact]";
            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO [Stage_Tech_KPI].[KPI_Fact] (Zeile, NT_ID, Runrate, Scenario,[Date],Wert) VALUES (?, ?, ?, ?, ?, ?)";


            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, entity.getRow());
                ps.setString(2, entity.getNT_ID());
                ps.setString(3, entity.getRunrate());
                ps.setString(4, entity.getScenario());
                //  ps.setDate(3, new java.sql.Date(2023,01,01));
                java.sql.Date sqlDate = (entity.getDate() != null) ? new java.sql.Date(entity.getDate().getTime()) : null;
                ps.setDate(5, sqlDate);
                //  ps.setDate(5, new java.sql.Date(entity.getDate().getTime() ));
                ps.setDouble (6, entity.getWert());
            });
            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveKPIPlan(List<Tech_KPIView.KPI_Plan> data, String selectedDatabase) {

        try {
            DataSource dataSource = getDataSource(selectedDatabase);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM [Stage_Tech_KPI].[KPI_Plan]";
            jdbcTemplate.update(sqlDelete);

            String sql = "INSERT INTO [Stage_Tech_KPI].[KPI_Plan] (Zeile, NT_ID, Spalte1, Scenario, VersionDate, VersionComment, Runrate) VALUES (?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sql, data, data.size(), (ps, entity) -> {

                java.sql.Date versionDate = null;
                if(entity.getVersionDate() != null)
                {
                    versionDate=new java.sql.Date(entity.getVersionDate().getTime());
                }

                ps.setInt(1,entity.getRow());
                ps.setString(2, entity.getNT_ID());
                ps.setString(3, entity.getSpalte1());
                ps.setString(4, entity.getScenario());
                ps.setDate(5, versionDate);
                ps.setString(6, entity.getVersionComment());
                ps.setString(7, entity.getRunrate());
                //  ps.setDate(3, new java.sql.Date(2023,01,01));
                //ps.setDate(3, new java.sql.Date(entity.getDate().getTime() ));
                //ps.setDouble (4, entity.getWert());
            });

            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveKPIActuals(List<Tech_KPIView.KPI_Actuals> data, String selectedDatabase) {

        try {
            DataSource dataSource = getDataSource(selectedDatabase);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM [Stage_Tech_KPI].[KPI_Actuals]";
            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO [Stage_Tech_KPI].[KPI_Actuals] (Zeile, [NT_ID],[WTAC_ID],[sort],[M2_Area],[M1_Network],[M3_Service],[M4_Dimension],[M5_Tech],[M6_Detail],[KPI_long],[Runrate],[Unit],[Description],[SourceReport],[SourceInput],[SourceComment] ,[SourceContact] ,[SourceLink] ) VALUES (?, ?, ?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {


                ps.setInt(1, entity.getRow());
                ps.setString(2, entity.getNT_ID());
                ps.setString(3, entity.getWTAC_ID());
                ps.setInt(4, entity.getSort());
                ps.setString(5, entity.getM2_Area());
                ps.setString(6, entity.getM1_Network());
                ps.setString(7, entity.getM3_Service());
                ps.setString(8, entity.getM4_Dimension());
                ps.setString(9, entity.getM5_Tech());
                ps.setString(10, entity.getM6_Detail());
                ps.setString(11, entity.getKPI_long());
                ps.setString(12, entity.getRunrate());
                ps.setString(13, entity.getUnit());
                ps.setString(14, entity.getDescription());
                ps.setString(15, entity.getSourceReport());
                ps.setString(16, entity.getSourceInput());
                ps.setString(17, entity.getSourceComment());
                ps.setString(18, entity.getSourceContact());
                ps.setString(19, entity.getSourceLink());
            });


            return "ok";
        } catch (Exception e) {
            return handleDatabaseError(e);
        }
    }

    private String handleDatabaseError(Exception e) {

        if (e instanceof DataAccessException) {
            Throwable rootCause = getRootCause(e);
            if (rootCause instanceof org.springframework.jdbc.CannotGetJdbcConnectionException) {
                return "Error: Cannot connect to the database. Check database configuration.";
            } else if (rootCause instanceof org.springframework.jdbc.BadSqlGrammarException) {
                return "Error: Table does not exist or SQL syntax error.";
            } else {
                e.printStackTrace();
                return "Database error: " + e.getMessage();
            }
        } else {
            e.printStackTrace();
            return "Unknown error: " + e.getMessage();
        }
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null) {
            return throwable;
        }
        return getRootCause(cause);
    }

}
