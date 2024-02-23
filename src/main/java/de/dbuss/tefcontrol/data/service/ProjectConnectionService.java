package de.dbuss.tefcontrol.data.service;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.B2pOutlookSub;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.modules.b2bmapsaleschannel.entity.MapSalesChannel;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CLTVInflow;
import de.dbuss.tefcontrol.data.modules.administration.entity.CurrentPeriods;
import de.dbuss.tefcontrol.data.modules.administration.entity.CurrentScenarios;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.*;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.view.GenericCommentsView;
import de.dbuss.tefcontrol.data.modules.kpi.Strategic_KPIView;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.CltvAllProduct;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.ProductHierarchie;
import de.dbuss.tefcontrol.data.modules.tarifmapping.entity.CLTVProduct;
import de.dbuss.tefcontrol.data.modules.tarifmapping.entity.MissingCLTVProduct;
import de.dbuss.tefcontrol.data.modules.userimport.ImportDimLineTapete;
import de.dbuss.tefcontrol.data.repository.ProjectConnectionRepository;
import de.dbuss.tefcontrol.data.modules.kpi.Tech_KPIView;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class ProjectConnectionService {
    private final ProjectConnectionRepository repository;
    private JdbcTemplate jdbcTemplate;

    @Getter
    private String errorMessage = "";

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    HashMap<String, String> defaultConnectionParams;

    public ProjectConnectionService(ProjectConnectionRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    private void init() {
        defaultConnectionParams = new HashMap<>();
        defaultConnectionParams.put("dbUrl", dbUrl);
        defaultConnectionParams.put("dbUser", dbUser);
        defaultConnectionParams.put("dbPassword", dbPassword);
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
            System.out.println("Username = "+projectConnection.get().getUsername()+ " Password = "+projectConnection.get().getPassword());
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

    @Primary
    public DataSource getDataSourceUsingParameter(String dbUrl, String dbUser, String dbPassword) {

        if(dbUser != null) {
            System.out.println(dbUrl);
            System.out.println("Username = " + dbUser + " Password = " + dbPassword);
            DataSource dataSource = DataSourceBuilder
                    .create()
                    .url(dbUrl)
                    .username(dbUser)
                    .password(dbPassword)
                    .build();
            return dataSource;
        }

        throw new RuntimeException("Database connection not found: " + dbUser);
    }

    public JdbcTemplate getJdbcConnection(String selectedDatabase) {
        DataSource dataSource = getDataSource(selectedDatabase);
        jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate;
    }
    public JdbcTemplate getJdbcConnection(String dbUrl, String dbUser, String dbPassword) {
        DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
        jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate;
    }

    public JdbcTemplate getJdbcDefaultConnection () {
        String dbUrl = defaultConnectionParams.get("dbUrl");
        String dbUser = defaultConnectionParams.get("dbUser");
        String dbPassword = defaultConnectionParams.get("dbPassword");
        DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
        jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate;
    }

    public List<CLTV_HW_Measures> getCLTVHWMeasuresData(String selectedDatabase) {
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

    public List<CLTV_HW_Measures> getCLTVHWMeasuresData(String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlQuery = "SELECT * FROM " + tableName;

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
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }

    }
    public String write2DB(List<CLTV_HW_Measures> data, String dbUrl, String dbUser, String dbPassword, String tableName) {

        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM " + tableName;
            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO "+tableName+" ( [Monat_ID], [Device], [Measure_Name], [Channel], [Value]) VALUES (?, ?, ?, ?, ?)";

            // Loop through the data and insert new records
            for (CLTV_HW_Measures item : data) {
                jdbcTemplate.update(
                        sqlInsert,
                        item.getMonat_ID(),
                        item.getDevice(),
                        item.getMeasure_Name(),
                        item.getChannel(),
                        item.getValue()
                );
            }
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
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
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
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

    public String saveFinancials(List<Financials> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM " + tableName;

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO " + tableName + " (zeile, month, category, comment, scenario, xtd) VALUES (?, ?, ?, ?, ?, ?)";

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
      //      String sqlUpdate ="delete from " + tableName + " where comment is null";
      //      System.out.println(sqlUpdate);
      //      jdbcTemplate.update(sqlUpdate);
            return Constants.OK;
        }
           catch (CannotGetJdbcConnectionException connectionException) {
            return connectionException.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveSubscriber(List<Subscriber> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM " + tableName;

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO " + tableName + " (zeile, month, category, payment_type, segment, comment, scenario, xtd) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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
        //    jdbcTemplate.update("delete from " + tableName + " where comment is null");
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveUnitsDeepDive(List<UnitsDeepDive> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);
          
            String sqlDelete = "DELETE FROM " + tableName;

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO " + tableName + " (zeile, month,segment, category, comment, scenario, xtd) VALUES (?, ?, ?, ?, ?, ?, ?)";

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
         //   jdbcTemplate.update("delete from " + tableName + " where comment is null");
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public void deleteKPIFact(String selectedDatabase) {
        getJdbcConnection(selectedDatabase);
        try {
            String sqlDelete = "DELETE FROM [Stage_Tech_KPI].[KPI_Fact]";
            jdbcTemplate.update(sqlDelete);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = handleDatabaseError(e);
        }
    }

    public String saveStrategic_KPIFact(List<Strategic_KPIView.Fact_CC_KPI> data, String tableName, int upload_id) {

        try {

            String sqlInsert = "INSERT INTO "+ tableName +" (Upload_ID, Zeile, Month, Scenario, Segment,CC_KPI,Amount ) VALUES (?, ?, ?, ?, ?, ?, ?)";


            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, upload_id);
                ps.setInt(2, entity.getRow());
                ps.setInt(3, entity.getPeriod());
                ps.setString(4, entity.getScenario());
                ps.setString(5, entity.getSegment());
                ps.setString(6, entity.getCC_KPI());

                if (entity.getAmount() == null){
                    ps.setString(7,null);
                }
                else
                {
                    ps.setDouble (7, entity.getAmount());
                }

            });
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }

    }

    public String saveStrategic_KPIDim(List<Strategic_KPIView.Dim_CC_KPI> data, String tableName, int upload_id) {

        try {

            String sqlInsert = "INSERT INTO "+ tableName +" (Upload_ID, Zeile, CC_KPI, CC_KPI_Gen01, CC_KPI_Gen02 ) VALUES (?, ?, ?, ?, ?)";


            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, upload_id);
                ps.setInt(2, entity.getRow());
                ps.setString(3, entity.getCC_KPI());
                ps.setString(4,entity.getCC_KPI_Gen01());
                ps.setString(5, entity.getCC_KPI_Gen02());

            });
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }

    }

    public String saveKPIFact(List<Tech_KPIView.KPI_Fact> data, String tableName, int upload_id) {

        try {

            String sqlInsert = "INSERT INTO "+ tableName +" (Zeile, NT_ID, XTD, Scenario_Name,[Date],Amount, Upload_ID) VALUES (?, ?, ?, ?, ?, ?, ?)";


            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, entity.getRow());
                ps.setString(2, entity.getNT_ID());
                ps.setString(3, entity.getRunrate());
                ps.setString(4, entity.getScenario());
                //  ps.setDate(3, new java.sql.Date(2023,01,01));
                java.sql.Date sqlDate = (entity.getDate() != null) ? new java.sql.Date(entity.getDate().getTime()) : null;
                ps.setDate(5, sqlDate);
                //  ps.setDate(5, new java.sql.Date(entity.getDate().getTime() ));
                //ps.setDouble (6, entity.getWert());
                if (entity.getWert() == null){
                    ps.setString(6,null);
                }
                else
                {
                    ps.setDouble (6, entity.getWert());
                }
                ps.setInt(7, upload_id);
            });
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }

    }

    public void deleteTableData(String dbUrl, String dbUser, String dbPassword, String tableName) {
        DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
        jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            String sqlDelete = "DELETE FROM "+tableName;
            jdbcTemplate.update(sqlDelete);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = handleDatabaseError(e);
        }
    }
    public String saveKPIPlan(List<Tech_KPIView.KPI_Plan> data, String tableName, int upload_id) {

        try {
            String sql = "INSERT INTO "+ tableName +" (Zeile, NT_ID, Spalte1, Scenario, VersionDate, VersionComment, Runrate, Upload_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sql, data, data.size(), (ps, entity) -> {

                java.sql.Date versionDate = null;
                if(entity.getVersionDate() != null)
                {
                    versionDate = new java.sql.Date(entity.getVersionDate().getTime());
                }
                ps.setInt(1,entity.getRow());
                ps.setString(2, entity.getNT_ID());
                ps.setString(3, entity.getSpalte1());
                ps.setString(4, entity.getScenario());
                ps.setDate(5, versionDate);
                ps.setString(6, entity.getVersionComment());
                ps.setString(7, entity.getRunrate());
                ps.setInt(8, upload_id);
                //  ps.setDate(3, new java.sql.Date(2023,01,01));
                //ps.setDate(3, new java.sql.Date(entity.getDate().getTime() ));
                //ps.setDouble (4, entity.getWert());
            });

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public void deleteKPIActuals(String selectedDatabase) {
        getJdbcConnection(selectedDatabase);
        try {
            String sqlDelete = "DELETE FROM [Stage_Tech_KPI].[KPI_Actuals]";
            jdbcTemplate.update(sqlDelete);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = handleDatabaseError(e);
        }
    }
    public String saveKPIActuals(List<Tech_KPIView.KPI_Actuals> data, String tableName, int upload_id) {

        try {
            String sqlInsert = "INSERT INTO "+ tableName +" (Zeile, [NT_ID],[WTAC_ID],[sort],[M2_Area],[M1_Network],[M3_Service],[M4_Dimension],[M5_Tech],[M6_Detail],[KPI_long],[Runrate],[Unit],[Description],[SourceReport],[SourceInput],[SourceComment] ,[SourceContact] ,[SourceLink], [Upload_ID] ) VALUES (?, ?, ?, ?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

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
                ps.setInt(20, upload_id);
            });

            return Constants.OK;
        } catch (Exception e) {
            return handleDatabaseError(e);
        }
    }

    public String handleDatabaseError(Exception e) {

        if (e instanceof DataAccessException) {
            Throwable rootCause = getRootCause(e);
            if (rootCause instanceof org.springframework.jdbc.CannotGetJdbcConnectionException) {
                return "Error: Cannot connect to the database. Check database configuration.";
            } else if (rootCause instanceof org.springframework.jdbc.BadSqlGrammarException) {
                return "Error: Table does not exist or SQL syntax error.";
            } else {
                e.printStackTrace();
                if(e.getMessage().contains(";")) {
                    String [] errorMessage = e.getMessage().split(";");
                    return errorMessage[errorMessage.length - 1];
                }
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

    public List<CltvAllProduct> getCltvAllProducts(String dbUrl, String dbUser, String dbPassword, String tableName) {

        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            //String sql = "SELECT [all_products], [all_products_gen_number], [all_products_gen2], [verarb_datum] FROM " + dataBase;
            String sql = "SELECT distinct [All Products] FROM " + tableName;

            List<CltvAllProduct> clatvAllProductList = jdbcTemplate.query(sql, (rs, rowNum) -> {
                CltvAllProduct cltvAllProduct = new CltvAllProduct();
                cltvAllProduct.setAllProducts(rs.getString("All Products"));
              //  cltvAllProduct.setAllProductGenNumber(rs.getString("all_products_gen_number"));
              //  cltvAllProduct.setAllProductGen2(rs.getString("all_products_gen2"));
              //  cltvAllProduct.setVerarb_datum(null);
                return cltvAllProduct;
            });

            return clatvAllProductList;
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = handleDatabaseError(e);
            return Collections.emptyList();
        }
    }

    public List<ProductHierarchie> fetchProductHierarchie(String dbUrl, String dbUser, String dbPassword, String targetTable, String filterValue) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            // Construct the dynamic SQL query with multiple OR conditions for filtering
            String sqlQuery = "SELECT * FROM " + targetTable + " WHERE Product LIKE ?" +
                    " OR [PFG_PO/PP] LIKE ?" +
                    " OR Knoten LIKE ?";

            // Create a RowMapper to map the query result to a ProductHierarchie object
            RowMapper<ProductHierarchie> rowMapper = (rs, rowNum) -> {
                ProductHierarchie productHierarchie = new ProductHierarchie();
           //     productHierarchie.setId(rs.getLong("id"));
                productHierarchie.setPfg_Type(rs.getString("PFG_PO/PP"));
                productHierarchie.setNode(rs.getString("Knoten"));
                productHierarchie.setProduct_name(rs.getString("Product"));
                return productHierarchie;
            };

            // Use wildcard characters to search for partial matches in all columns
            String filteredValue = "%" + filterValue + "%";

            // Use the same filteredValue for all columns
            List<ProductHierarchie> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper, filteredValue, filteredValue, filteredValue);

            return fetchedData;
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }

    }

    public String saveProductHierarchie(ProductHierarchie data, String dbUrl, String dbUser, String dbPassword, String targetTable) {
        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            // Check if the data already exists based on a case-insensitive condition
            String sqlSelect = "SELECT COUNT(*) FROM "+ targetTable +" WHERE Id = ?";
            int count = jdbcTemplate.queryForObject(sqlSelect, Integer.class, data.getId());

            if (count > 0) {
                // Data exists, so update it
                String sqlUpdate = "UPDATE "+ targetTable + " SET [PFG_PO/PP] = ?, Knoten = ?, Product = ? WHERE Id = ?";
                jdbcTemplate.update(
                        sqlUpdate,
                        data.getPfg_Type(),
                        data.getNode(),
                        data.getProduct_name(),
                        data.getId()
                );
            } else {
                // Data doesn't exist, so insert it
                String sqlInsert = "INSERT INTO "+ targetTable +" (Product, [PFG_PO/PP], Knoten) VALUES (?, ?, ?)";
                jdbcTemplate.update(
                        sqlInsert,
                        data.getProduct_name(),
                        data.getPfg_Type(),
                        data.getNode()
                );
            }

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }

    }

    public List<String> getAllMissingProducts(String dbUrl, String dbUser, String dbPassword, String targetView) {
        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlQuery = "SELECT * FROM " + targetView;

            RowMapper<String> rowMapper = (rs, rowNum) -> {
                return rs.getString("All Products");
            };

            List<String> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper);

            return fetchedData;
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }

    }

    public String saveListOfProductHierarchie(List<ProductHierarchie> data, String dbUrl, String dbUser, String dbPassword, String targetTable) {
        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sql = "INSERT INTO " + targetTable + " (Product, [PFG_PO/PP], Knoten) VALUES (?, ?, ?)";

            jdbcTemplate.batchUpdate(sql, data, data.size(), (ps, productHierarchie) -> {
                ps.setString(1, productHierarchie.getProduct_name());
                ps.setString(2, productHierarchie.getPfg_Type());
                ps.setString(3, productHierarchie.getNode());
            });

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public List<CLTVInflow> getAllCLTVInflow(String tableName, String dbUrl, String dbUser, String dbPassword) {
        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlQuery = "SELECT * FROM " +tableName;

            // Create a RowMapper to map the query result to a CLTVInflow object
            RowMapper<CLTVInflow> rowMapper = (rs, rowNum) -> {
                CLTVInflow cltvInflow = new CLTVInflow();
                cltvInflow.setContractFeatureId(rs.getLong("ContractFeature_id"));
                cltvInflow.setAttributeClassesId(rs.getLong("AttributeClasses_ID"));
                cltvInflow.setCfTypeClassName(rs.getString("CF_TYPE_CLASS_NAME"));
                cltvInflow.setAttributeClassesName(rs.getString("AttributeClasses_NAME"));
                cltvInflow.setContractFeatureSubCategoryName(rs.getString("ContractFeatureSubCategory_Name"));
                cltvInflow.setContractFeatureName(rs.getString("ContractFeature_Name"));
                cltvInflow.setCfTypeName(rs.getString("CF_TYPE_NAME"));
                cltvInflow.setCfDurationInMonth(rs.getString("CF_Duration_in_Month"));
                cltvInflow.setConnectType(rs.getString("Connect_Type"));
                cltvInflow.setCltvCategoryName(rs.getString("CLTV_Category_Name"));
                cltvInflow.setControllingBrandingDetailed(rs.getString("Controlling_Branding_Detailed"));
                cltvInflow.setControllingBranding(rs.getString("Controlling_Branding"));
                cltvInflow.setUser(rs.getString("User"));
                cltvInflow.setCltvChargeName(rs.getString("CLTV_Charge_Name"));

                return cltvInflow;
            };

            List<CLTVInflow> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper);

            return fetchedData;
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }
    }


    public String updateListOfCLTVInflow(List<CLTVInflow> modifiedCLTVInflow, String tableName, String dbUrl, String dbUser, String dbPassword) {
        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            // Assuming that you have a unique identifier to match the records to update, e.g., contractFeatureId
            String sql = "UPDATE " + tableName + " SET CLTV_Category_Name = ?, Controlling_Branding_Detailed = ?, " +
                    "Controlling_Branding = ?, CLTV_Charge_Name = ? WHERE ContractFeature_id = ?";

            jdbcTemplate.batchUpdate(sql, modifiedCLTVInflow, modifiedCLTVInflow.size(), (ps, cltvInflow) -> {
                ps.setString(1, cltvInflow.getCltvCategoryName());
                ps.setString(2, cltvInflow.getControllingBrandingDetailed());
                ps.setString(3, cltvInflow.getControllingBranding());
                ps.setString(4, cltvInflow.getCltvChargeName());
                ps.setLong(5, cltvInflow.getContractFeatureId());
            });

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }


    public String saveOutlookMGSR(List<OutlookMGSR> data, String tableName, String dbUrl, String dbUser, String dbPassword, int upload_id) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

       //     String sqlDelete = "DELETE FROM " + tableName;
       //     jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO "+ tableName +" (Zeile, Blatt, month, PL_Line, profit_center, scenario, block, segment, payment_type, type_of_data, value, Upload_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, entity.getZeile());
                ps.setString(2, entity.getBlatt());
                ps.setInt(3, entity.getMonth());
                ps.setString(4, entity.getPl_Line());
                ps.setLong(5, entity.getProfitCenter());
                ps.setString(6, entity.getScenario());
                ps.setString(7, entity.getBlock());
                ps.setString(8, entity.getSegment());
                ps.setString(9, entity.getPaymentType());
                ps.setString(10, entity.getTypeOfData());
              //  ps.setDouble(11, Double.parseDouble((entity.getValue())));
                if (entity.getValue() != null) {
                    ps.setDouble(11, Double.parseDouble(entity.getValue()));
                } else {
                    ps.setNull(11, Types.DOUBLE);
                }
           //     ps.setString(11, (entity.getValue()));
                ps.setInt(12, upload_id);
              //  java.sql.Date sqlDate = (entity.getLoadDate() != null) ? new java.sql.Date(entity.getLoadDate().getTime()) : null;
              //  ps.setDate(12, sqlDate);
            });

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveB2POutlookSub(List<B2pOutlookSub> data, String tableName, String dbUrl, String dbUser, String dbPassword, int upload_id) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

       //     String sqlDelete = "DELETE FROM " + tableName;
       //     jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO "+ tableName +" (Zeile, Blatt, Month, Scenario, Measure, PHY_Line, Segment, Payment_Type, Contract_Type, Value, Upload_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, entity.getZeile());
                ps.setString(2, entity.getBlatt());
                ps.setInt(3, entity.getMonth());
                ps.setString(4, entity.getScenario());
                ps.setString(5, entity.getMeasure());
                ps.setString(6, entity.getPhysicalsLine());
                ps.setString(7, entity.getSegment());
                ps.setString(8, entity.getPaymentType());
                ps.setString(9, entity.getContractType());
                if (entity.getValue() != null) {
                    ps.setDouble(10, Double.parseDouble(entity.getValue()));
                } else {
                    ps.setNull(10, Types.DOUBLE);
                }
            //    ps.setDouble(10, entity.getValue());
                ps.setInt(11, upload_id);
                //  java.sql.Date sqlDate = (entity.getLoadDate() != null) ? new java.sql.Date(entity.getLoadDate().getTime()) : null;
                //  ps.setDate(12, sqlDate);
            });

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveXPexComments(List<XPexComment> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM " + tableName;

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO "+ tableName +" (Zeile, Date, Topic, Comment, Category_1, Category_2, Scenario, XTD) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, entity.getZeile());
                ps.setInt(2, entity.getDate());
                ps.setString(3, entity.getTopic());
                ps.setString(4, entity.getComment());
                ps.setString(5, entity.getCategory1());
                ps.setString(6, entity.getCategory2());
                ps.setString(7, entity.getScenario());
                ps.setString(8, entity.getXtd());
            });
            //delete rows with Empty Comments
        //    String sqlUpdate ="delete from " + tableName + " where comment is null";
        //    System.out.println(sqlUpdate);
        //    jdbcTemplate.update(sqlUpdate);
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveITOnlyComments(List<ITOnlyComment> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM " + tableName;

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO "+ tableName +" (Zeile, Date, Topic, Comment, Category_1, Category_2, Scenario, XTD) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, entity.getZeile());
                ps.setInt(2, entity.getDate());
                ps.setString(3, entity.getTopic());
                ps.setString(4, entity.getComment());
                ps.setString(5, entity.getCategory1());
                ps.setString(6, entity.getCategory2());
                ps.setString(7, entity.getScenario());
                ps.setString(8, entity.getXtd());
            });
            //delete rows with Empty Comments
        //    String sqlUpdate ="delete from " + tableName + " where comment is null";
        //    System.out.println(sqlUpdate);
        //    jdbcTemplate.update(sqlUpdate);
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveKPIsComments(List<KPIsComment> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM " + tableName;

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO "+ tableName +" (Zeile, Date, Topic, Comment, Category_1, Category_2, Plan_Scenario) VALUES (?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, entity.getZeile());
                ps.setInt(2, entity.getDate());
                ps.setString(3, entity.getTopic());
                ps.setString(4, entity.getComment());
                ps.setString(5, entity.getCategory1());
                ps.setString(6, entity.getCategory2());
                ps.setString(7, entity.getPlanScenario());
            });

            //delete rows with Empty Comments
      //      String sqlUpdate ="delete from " + tableName + " where comment is null";
      //      System.out.println(sqlUpdate);
      //      jdbcTemplate.update(sqlUpdate);
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveFlashFinancials(List<FlashFinancials> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM " + tableName;

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO "+ tableName +" (Zeile, Month, Category, Comment,Scenario, XTD) VALUES (?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, entity.getZeile());
                ps.setInt(2, entity.getMonth());
                ps.setString(3, entity.getCategory());
                ps.setString(4, entity.getComment());
                ps.setString(5, entity.getScenario());
                ps.setString(6, entity.getXtd());
            });

            //delete rows with Empty Comments
     //       String sqlUpdate ="delete from " + tableName + " where comment is null";
     //       System.out.println(sqlUpdate);
     //       jdbcTemplate.update(sqlUpdate);
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String startAgent(int projectId) {
        System.out.println("Start Agent-Jobs...");
        String result = "";
        jdbcTemplate = getJdbcDefaultConnection();

        String sql = "select pp.value from project_parameter pp, projects p\n" +
                "  where pp.namespace=p.page_url\n" +
                "  and pp.name in ('DBJobs')\n" +
                "  and p.id=?";


        String agents = null;

        try{
            agents=jdbcTemplate.queryForObject(sql, new Object[]{projectId},String.class);
        }
        catch(Exception e)
        {
            result = "Problem to find relevant Jobs in project_parameter for job_id " + projectId+ "!";
            Notification.show("Problem to find relevant Jobs in project_parameter for job_id " + projectId+ "!",10000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return result;
        }


        sql = "select pp.name, pp.value from project_parameter pp, projects p\n" +
                "  where pp.namespace=p.page_url\n" +
                "  and pp.name in ('DB_Server','DB_Name', 'DB_User','DB_Password')\n" +
                "  and p.id="+projectId ;

        List<ProjectParameter> resultList = jdbcTemplate.query(sql, (rs, rowNum) -> {
            ProjectParameter projectParameter = new ProjectParameter();
            projectParameter.setName(rs.getString("name"));
            projectParameter.setValue(rs.getString("value"));
            return projectParameter;
        });
        String dbName = null;
        String dbServer = null;
        for (ProjectParameter projectParameter : resultList) {
            if (Constants.DB_NAME.equals(projectParameter.getName())) {
                dbName = projectParameter.getValue();
            } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                dbUser = projectParameter.getValue();
            } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                dbPassword = projectParameter.getValue();
            } else if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                dbServer = projectParameter.getValue();
            }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);

        jdbcTemplate.setDataSource(dataSource);

        if (agents != null) {
            String[] jobs = agents.split(";");
            for (String job : jobs) {
                System.out.println("Start job: " + job);

                try {
                    sql = "msdb.dbo.sp_start_job @job_name='" + job + "'";
                    jdbcTemplate.execute(sql);
                    result = job + " startet...";
                   // Notification.show(job + " startet..." ,5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception e) {
                    // Handle other exceptions
                    result = handleDatabaseError(e);
                  //  Notification.show("Error: " + errorMessage, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

            }
        }
        return result;
    }

    public List<JobDetails> getJobDetails(String job_id, String agent_db) {
        try {

            DataSource dataSource = getDataSource(agent_db);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlQuery = "select run_date, run_time, message from msdb.dbo.sysjobhistory AS jh where job_id='"+job_id+"' order by run_date desc, run_time desc";

            // Create a RowMapper to map the query result to a CLTVInflow object
            RowMapper<JobDetails> rowMapper = (rs, rowNum) -> {
                JobDetails jobDetails = new JobDetails();
                jobDetails.setRun_date(rs.getInt("run_date"));
                jobDetails.setRun_time(rs.getString("run_time"));
                jobDetails.setMessage(rs.getString("message"));

                return jobDetails;
            };

            List<JobDetails> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper);

            return fetchedData;
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }
    }

    public List<AgentJobs> getAgentJobListFindbyName(String agentNames, String agent_db) {

        try {
            if (agentNames != null) {
                String[] jobs = agentNames.split(";");
                DataSource dataSource = getDataSource(agent_db);
                jdbcTemplate = new JdbcTemplate(dataSource);

                // String sqlQuery = "SELECT * FROM job_status WHERE name IN (" + String.join(",", "?".repeat(jobs.length)) + ")";
                   String sqlQuery = "SELECT * FROM job_status WHERE name IN (" + String.join(",", Collections.nCopies(jobs.length, "?")) + ")";

                // Create a RowMapper to map the query result to an AgentJobs object
                RowMapper<AgentJobs> rowMapper = (rs, rowNum) -> {
                    AgentJobs agentJobs = new AgentJobs();
                    agentJobs.setJob_id(rs.getString("job_id")); // Assuming job_id is the correct column name
                    agentJobs.setName(rs.getString("Name"));
                    agentJobs.setJobEnabled(rs.getString("Job_Enabled"));
                    agentJobs.setJobDescription(rs.getString("Job_Description"));
                    agentJobs.setJob_activity(rs.getString("Job_Activity"));
                    agentJobs.setDuration_Min(rs.getString("Duration_Min"));
                    agentJobs.setJobStartDate(rs.getString("Job_Start_Date"));
                    agentJobs.setJobLastExecutedStep(rs.getString("Job_Last_Executed_Step"));
                    agentJobs.setJobExecutedStepDate(rs.getString("Job_Executed_Step_Date"));
                    agentJobs.setJobStopDate(rs.getString("Job_Stop_Date"));
                    agentJobs.setJobNextRunDate(rs.getString("Job_Next_Run_Date"));
                    agentJobs.setResult(rs.getString("result"));
                    return agentJobs;
                };

                List<AgentJobs> fetchedData = jdbcTemplate.query(sqlQuery, jobs, rowMapper);

                return fetchedData;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            System.out.println(errorMessage);
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public String saveGenericComments(List<GenericComments> data, String tableName, String dbUrl, String dbUser, String dbPassword, int upload_ID) {

        try {

            //  DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            //  jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlInsert = "INSERT INTO " + tableName + " ([Upload_ID], [File_Name], [Register_Name], [Line_Number], [Responsible], [Topic], [Month], " +
                    "[Category_1], [Category_2], [Scenario], [XTD], [Segment], [Payment_Type], [Comment]) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {
                ps.setInt(1,upload_ID);
                ps.setString(2,entity.getFileName());
                ps.setString(3,entity.getRegisterName());
                ps.setInt(4,entity.getLineNumber());
                ps.setString(5,entity.getResponsible());
                ps.setString(6,entity.getTopic());
                ps.setInt(7,entity.getMonth());
                ps.setString(8,entity.getCategory1());
                ps.setString(9,entity.getCategory2());
                ps.setString(10,entity.getScenario());
                ps.setString(11,entity.getXtd());
                ps.setString(12,entity.getSegment());
                ps.setString(13,entity.getPaymentType());
                ps.setString(14,entity.getComment());
            });

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveGenericComments(List<GenericComments> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {

            Map<String, Integer> uploadIdMap = GenericCommentsView.projectUploadIdMap;
          //  DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
          //  jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlInsert = "INSERT INTO " + tableName + " ([Upload_ID], [File_Name], [Register_Name], [Line_Number], [Responsible], [Topic], [Month], " +
                    "[Category_1], [Category_2], [Scenario], [XTD], [Segment], [Payment_Type], [Comment]) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {
                ps.setInt(1,uploadIdMap.get(entity.getFileName()));
                ps.setString(2,entity.getFileName());
                ps.setString(3,entity.getRegisterName());
                ps.setInt(4,entity.getLineNumber());
                ps.setString(5,entity.getResponsible());
                ps.setString(6,entity.getTopic());
                ps.setInt(7,entity.getMonth());
                ps.setString(8,entity.getCategory1());
                ps.setString(9,entity.getCategory2());
                ps.setString(10,entity.getScenario());
                ps.setString(11,entity.getXtd());
                ps.setString(12,entity.getSegment());
                ps.setString(13,entity.getPaymentType());
                ps.setString(14,entity.getComment());
              });

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }





 /*   public String saveGenericComments(List<GenericComments> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {

            Map<String, Integer> uploadIdMap = GenericCommentsView.projectUploadIdMap;
          //  DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
          //  jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlInsert = "INSERT INTO " + tableName + " ([Upload_ID], [File_Name], [Register_Name], [Line_Number], [Responsible], [Topic], [Month], " +
                    "[Category_1], [Category_2], [Scenario], [XTD], [Segment], [Payment_Type], [Comment]) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            // Loop through the data and insert new records
            for (GenericComments item : data) {
                jdbcTemplate.update(
                        sqlInsert,
                        uploadIdMap.get(item.getFileName()),
                        item.getFileName(),
                        item.getRegisterName(),
                        item.getLineNumber(),
                        item.getResponsible(),
                        item.getTopic(),
                        item.getMonth(),
                        item.getCategory1(),
                        item.getCategory2(),
                        item.getScenario(),
                        item.getXtd(),
                        item.getSegment(),
                        item.getPaymentType(),
                        item.getComment()
                );
            }
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }*/


 /*   public String saveUploadedGenericFileData(ProjectUpload entity) {

        try {
            String tableName = "Log.User_uploads";
          //  jdbcTemplate = getJdbcDefaultConnection();
            String sql = "INSERT INTO " + tableName + " ([File_Name], [User_Name], [Modul_Name]) VALUES (?, ?, ?)";

            System.out.println("Execute SQL:" + sql);
            System.out.println("1. Parameter: " + entity.getFileName());
            System.out.println("2. Parameter: " + entity.getUserName());
            System.out.println("3. Parameter: " + entity.getModulName());

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sql, new String[]{"Upload_ID"});
                    ps.setString(1, entity.getFileName());
                    ps.setString(2, entity.getUserName());
                    ps.setString(3, entity.getModulName());
                    return ps;
                }
            }, keyHolder);

            // Check if the insertion was successful
            if (affectedRows > 0) {
                // Retrieve the generated ID from the KeyHolder
                long generatedId = keyHolder.getKey().longValue();

                if (entity.getModulName().equals("Tech_KPI")){
                    Tech_KPIView.projectUploadIdMap.put(entity.getFileName(), (int) generatedId);
                } else if (entity.getModulName().equals("GenericComments")) {
                    GenericCommentsView.projectUploadIdMap.put(entity.getFileName(), (int) generatedId);
                }


                return Constants.OK;
            } else {
                return "Failed to insert data into " + tableName ;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }*/

    public Integer saveUploadedGenericFileData(ProjectUpload entity) {

        try {
            String tableName = "Log.User_uploads";
            //  jdbcTemplate = getJdbcDefaultConnection();
            String sql = "INSERT INTO " + tableName + " ([File_Name], [User_Name], [Modul_Name]) VALUES (?, ?, ?)";

            System.out.println("Execute SQL:" + sql);
            System.out.println("1. Parameter: " + entity.getFileName());
            System.out.println("2. Parameter: " + entity.getUserName());
            System.out.println("3. Parameter: " + entity.getModulName());

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int affectedRows = jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(sql, new String[]{"Upload_ID"});
                    ps.setString(1, entity.getFileName());
                    ps.setString(2, entity.getUserName());
                    ps.setString(3, entity.getModulName());
                    return ps;
                }
            }, keyHolder);

            // Check if the insertion was successful
            if (affectedRows > 0) {
                // Retrieve the generated ID from the KeyHolder
                Integer generatedId = keyHolder.getKey().intValue();
                return generatedId;
            } else {
                return -1 ;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /*
    public Map<String, Integer> getUploadIdMap() {
        try {

            jdbcTemplate = getJdbcDefaultConnection();
            String sql = "SELECT [Upload_ID], [File_Name] FROM PIT2.Log.user_uploads"; //ToDo: Get DB of target table, this DB should used instead PIT2-DB


            // Execute the query and get a list of maps
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql);

            // Create a Map with file name as key and upload ID as value
            Map<String, Integer> uploadIdMap = new HashMap<>();
            for (Map<String, Object> row : resultList) {
                int uploadId = (int) row.get("Upload_ID");
                String fileName = (String) row.get("File_Name");
                uploadIdMap.put(fileName, uploadId);
            }

            return uploadIdMap;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }



    public Map<String, Integer> getUploadIdMap(String modulName, String userName, String dbUrl, String dbUser, String dbPassword) {
        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);
            String sql = "SELECT [Upload_ID] FROM Log.user_uploads where Modul_Name= '" + modulName + "' and User_Name = '" + userName+ "' order by Upload_ID desc";

            System.out.println("Execute SQL: " + sql);
            // Execute the query and get a list of maps
            List<Map<String, Object>> resultList = jdbcTemplate.queryForList(sql);

            // Create a Map with file name as key and upload ID as value
            Map<String, Integer> uploadIdMap = new HashMap<>();
            for (Map<String, Object> row : resultList) {
                int uploadId = (int) row.get("Upload_ID");
                String fileName = (String) row.get("File_Name");
                uploadIdMap.put(fileName, uploadId);
            }

            return uploadIdMap;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
 */

    public List<String> getCobiAdminQFCPlanOutlook(String dbUrl, String dbUser, String dbPassword, String sql) {
        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);
// Execute SQL query and retrieve the result as a list of maps
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            List<String> resultList = new ArrayList<>();
            for (Map<String, Object> row : result) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    resultList.add(entry.getValue().toString());
                }
            }
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    public String saveCobiAdminCurrentPeriods(CurrentPeriods data, String dbUrl, String dbUser, String dbPassword, String targetTable) {
        try {
            jdbcTemplate = getJdbcConnection(dbUrl, dbUser, dbPassword);

          //  String sqlDelete = "DELETE FROM " + targetTable;
          //  jdbcTemplate.update(sqlDelete);

            String sql = "INSERT INTO " + targetTable + " (Upload_ID, Current_Month) VALUES (?, ?)";
            jdbcTemplate.update(sql, data.getUpload_ID(), data.getCurrent_month());

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveCobiAdminCurrentScenarios(CurrentScenarios data, String dbUrl, String dbUser, String dbPassword, String targetTable) {
        try {

            jdbcTemplate = getJdbcConnection(dbUrl, dbUser, dbPassword);

          //  String sqlDelete = "DELETE FROM " + targetTable;
          //  jdbcTemplate.update(sqlDelete);

            String sql = "INSERT INTO " + targetTable + " (Upload_ID, Current_Plan, Current_Outlook, Prior_Outlook, Current_QFC) VALUES (?,?, ?, ?, ?)";
            jdbcTemplate.update(sql, data.getUpload_ID(), data.getCurrent_Plan(), data.getCurrent_Outlook(), data.getPrior_Outlook(), data.getCurrent_QFC());

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveReportAdmintPeriods(CurrentPeriods data, String dbUrl, String dbUser, String dbPassword, String targetTable) {
        try {
            jdbcTemplate = getJdbcConnection(dbUrl, dbUser, dbPassword);

            // Check if the record exists
            String sqlSelect = "SELECT COUNT(*) FROM " + targetTable + " WHERE ReportName = ?";
            int count = jdbcTemplate.queryForObject(sqlSelect, Integer.class, "Rohdatenreport");

            if (count > 0) {
                // Record exists, perform update
                String sqlUpdate = "UPDATE " + targetTable + " SET Load_Monat = ? WHERE ReportName = ?";
                jdbcTemplate.update(sqlUpdate, data.getCurrent_month(), "Rohdatenreport");
            } else {
                // Record does not exist, perform insert
                String sqlInsert = "INSERT INTO " + targetTable + " (ReportName, Load_Monat) VALUES (?, ?)";
                jdbcTemplate.update(sqlInsert, "Rohdatenreport", data.getCurrent_month());
            }

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String executeReportAdminPeriods(CurrentPeriods data, String dbUrl, String dbUser, String dbPassword) {
        try {
            System.out.println("Method: executeReportAdminPeriods =>");
            System.out.println("CurrentPeriods: " + data.getCurrent_month());
            System.out.println("dbUrl: " + dbUrl);
            System.out.println("dbUser: " + dbUser);
            System.out.println("dbPassword: " + dbPassword);

            jdbcTemplate = getJdbcConnection(dbUrl, dbUser, dbPassword);

            // Check if the record exists
            String sql = "exec Admin_DB.dbo.setParameter @JobName='ESS_IF_Report_01', @Parameter1="+data.getCurrent_month();
            System.out.println("sql: " + sql);
            int result = jdbcTemplate.queryForObject(sql, Integer.class);
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }


    public String addMonthsInCLTVHWMeasure(String dbUrl, String dbUser, String dbPassword, String sql) {
        try {

            jdbcTemplate = getJdbcConnection(dbUrl, dbUser, dbPassword);

            jdbcTemplate.update(sql);

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }


    public List<CLTVProduct> getCLTVProducts(String dbUrl, String dbUser, String dbPassword, String tableName) {

        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlQuery = "SELECT * FROM " + tableName;

            // Create a RowMapper to map the query result to a CLTV_HW_Measures object
            RowMapper<CLTVProduct> rowMapper = (rs, rowNum) -> {
                CLTVProduct cltvProduct = new CLTVProduct();
             //   cltvProduct.setId(rs.getInt("ID"));
                cltvProduct.setNode(rs.getString("Node"));
                cltvProduct.setChild(rs.getString("Child"));
                cltvProduct.setCltvTarif(rs.getString("CLTV_Tarif"));
                cltvProduct.setProductType(rs.getString("Product_Type"));
                cltvProduct.setUser(rs.getString("User"));
                cltvProduct.setVerarbDatum(rs.getDate("verarb_datum"));
                return cltvProduct;
            };

            List<CLTVProduct> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper);

            return fetchedData;
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }

    }

    public List<MissingCLTVProduct> getMissingCLTVProducts(String dbUrl, String dbUser, String dbPassword, String tableName) {

        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlQuery = "SELECT * FROM " + tableName;

            // Create a RowMapper to map the query result to a CLTV_HW_Measures object
            RowMapper<MissingCLTVProduct> rowMapper = (rs, rowNum) -> {
                MissingCLTVProduct missingCLTVProduct = new MissingCLTVProduct();
                missingCLTVProduct.setTariffGroupId(rs.getString("TariffGroupID"));
                missingCLTVProduct.setTariffGroupName(rs.getString("TariffGroupName"));
                missingCLTVProduct.setTariffGroupL4Name(rs.getString("TariffGroupL4Name"));
                return missingCLTVProduct;
            };

            List<MissingCLTVProduct> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper);

            return fetchedData;
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }

    }

    public String saveCLTVProduct(CLTVProduct item, String dbUrl, String dbUser, String dbPassword, String tableName) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlInsert = "INSERT INTO " + tableName + " (Node, Child, CLTV_Tarif, Product_Type, [User]) VALUES (?, ?, ?, ?, ?)";

            jdbcTemplate.update(
                    sqlInsert,
                    item.getNode(),
                    item.getChild(),
                    item.getCltvTarif(),
                    item.getProductType(),
                    item.getUser()
            );

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String updateCLTVProducts(String dbUrl, String dbUser, String dbPassword, String tableName, List<CLTVProduct> updatedCltvProductsList) {
        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlUpdate = "UPDATE " + tableName +
                    " SET CLTV_Tarif = ?, Product_Type = ? " +
                    "WHERE Node = ?";

            String sqlUpdateChild = "UPDATE " + tableName +
                    " SET CLTV_Tarif = ?, Product_Type = ? " +
                    "WHERE Child = ?";

            for (CLTVProduct item : updatedCltvProductsList) {
                if (item.getNode() != null) {
                    jdbcTemplate.update(
                            sqlUpdate,
                            item.getCltvTarif(),
                            item.getProductType(),
                            item.getNode()
                    );
                } else if (item.getChild() != null) {
                    jdbcTemplate.update(
                            sqlUpdateChild,
                            item.getCltvTarif(),
                            item.getProductType(),
                            item.getChild()
                    );
                }
            }

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String deleteMissingCLTVProduct(String dbUrl, String dbUser, String dbPassword, String tableName, String tariffGroupId) {
        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM " + tableName + " WHERE TariffGroupID = ?";

            jdbcTemplate.update(sqlDelete, tariffGroupId);

            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public List<MapSalesChannel> getMapSalesChannelList(String dbUrl, String dbUser, String dbPassword, String tableName) {

        try {
            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlQuery = "SELECT * FROM " + tableName;

            // Create a RowMapper to map the query result to a CLTV_HW_Measures object
            RowMapper<MapSalesChannel> rowMapper = (rs, rowNum) -> {
                MapSalesChannel mapSalesChannel = new MapSalesChannel();
              //  mapSalesChannel.setUploadId(rs.getLong("Upload_ID"));
                mapSalesChannel.setSalesChannel(rs.getString("Sales_Channel"));
                mapSalesChannel.setChannel(rs.getString("Channel"));
                return mapSalesChannel;
            };

            List<MapSalesChannel> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper);

            return fetchedData;
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }

    }

    public String saveMapSalesChannel(String dbUrl, String dbUser, String dbPassword, String tableName, int uploadId, List<MapSalesChannel> data) {
        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlInsert = "INSERT INTO " + tableName + " ([Upload_ID], [Sales_Channel], [Channel]) VALUES (?, ?, ?)";

            // Loop through the data and insert new records
            for (MapSalesChannel item : data) {
                jdbcTemplate.update(
                        sqlInsert,
                        uploadId,
                        item.getSalesChannel(),
                        item.getChannel()
                );
            }
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }

    public String saveDimLineTapete(List<ImportDimLineTapete.Dim_Line_Tapete> data, String tableName, int upload_id) {


        try {

            String sqlInsert = "INSERT INTO "+ tableName +" ([Upload_ID],[File_Name],[Register_Name],[Line_Number],[PL_Line],[PL_Line_Name],[PL_Line_Gen01],[PL_Line_Gen01_Name],[PL_Line_Gen02],[PL_Line_Gen02_Name],[PL_Line_Gen03],[PL_Line_Gen03_Name],[PL_Line_Gen03_Sortierung],[PL_Line_Gen04],[PL_Line_Gen04_Name],[PL_Line_Gen04_Sortierung],[PL_Line_Gen05],[PL_Line_Gen05_Name],[PL_Line_Gen05_Sortierung],[PL_Line_Gen06],[PL_Line_Gen06_Name] ,[PL_Line_Gen06_Sortierung],[PL_Line_Gen07],[PL_Line_Gen07_Name],[PL_Line_Gen07_Sortierung],[PL_Line_Gen08],[PL_Line_Gen08_Name],[PL_Line_Gen08_Sortierung],[PL_Line_Gen09],[PL_Line_Gen09_Name],[PL_Line_Gen09_Sortierung],[PL_Line_Gen10],[PL_Line_Gen10_Name],[PL_Line_Gen10_Sortierung],[PL_Line_Gen11],[PL_Line_Gen11_Name],[PL_Line_Gen11_Sortierung],[PL_Line_Gen12],[PL_Line_Gen12_Name],[PL_Line_Gen12_Sortierung],[PL_Line_Gen13],[PL_Line_Gen13_Name],[PL_Line_Gen13_Sortierung],[PL_Line_Gen14],[PL_Line_Gen14_Name],[PL_Line_Gen14_Sortierung],[PL_Line_Gen15],[PL_Line_Gen15_Name],[PL_Line_Gen15_Sortierung] ) VALUES (?, ?, ?, ?, ?, ?, ?,?,?,?,?, ?, ?, ?, ?, ?, ?,?,?,?,?, ?, ?, ?, ?, ?, ?,?,?,?,?, ?, ?, ?, ?, ?, ?,?,?,?,?,?,?,?,?,?,?,?,?)";


            jdbcTemplate.batchUpdate(sqlInsert, data, data.size(), (ps, entity) -> {

                ps.setInt(1, upload_id);
                ps.setString(2, "FileName");
                ps.setString(3, "RegisterName");
                ps.setInt(4, entity.getRow());
                ps.setString(5, entity.getPL_Line());
                ps.setString(6, entity.getPL_Line_Name());
                ps.setString(7, entity.getPL_Line_Gen01());
                ps.setString(8, entity.getPL_Line_Gen01_Name());
                ps.setString(9, entity.getPL_Line_Gen02());
                ps.setString(10, entity.getPL_Line_Gen02_Name());
                ps.setString(11, entity.getPL_Line_Gen03());
                ps.setString(12, entity.getPL_Line_Gen03_Name());
                ps.setInt(13, entity.getPL_Line_Gen03_Sortierung());
                ps.setString(14, entity.getPL_Line_Gen04());
                ps.setString(15, entity.getPL_Line_Gen04_Name());
                ps.setInt(16, entity.getPL_Line_Gen04_Sortierung());
                ps.setString(17, entity.getPL_Line_Gen05());
                ps.setString(18, entity.getPL_Line_Gen05_Name());
                ps.setInt(19, entity.getPL_Line_Gen05_Sortierung());
                ps.setString(20, entity.getPL_Line_Gen06());
                ps.setString(21, entity.getPL_Line_Gen06_Name());
                ps.setInt(22, entity.getPL_Line_Gen06_Sortierung());
                ps.setString(23, entity.getPL_Line_Gen07());
                ps.setString(24, entity.getPL_Line_Gen07_Name());
                ps.setInt(25, entity.getPL_Line_Gen07_Sortierung());
                ps.setString(26, entity.getPL_Line_Gen08());
                ps.setString(27, entity.getPL_Line_Gen08_Name());
                ps.setInt(28, entity.getPL_Line_Gen08_Sortierung());
                ps.setString(29, entity.getPL_Line_Gen09());
                ps.setString(30, entity.getPL_Line_Gen09_Name());
                ps.setInt(31, entity.getPL_Line_Gen09_Sortierung());
                ps.setString(32, entity.getPL_Line_Gen10());
                ps.setString(33, entity.getPL_Line_Gen10_Name());
                ps.setInt(34, entity.getPL_Line_Gen10_Sortierung());
                ps.setString(35, entity.getPL_Line_Gen11());
                ps.setString(36, entity.getPL_Line_Gen11_Name());
                ps.setInt(37, entity.getPL_Line_Gen11_Sortierung());
                ps.setString(38, entity.getPL_Line_Gen12());
                ps.setString(39, entity.getPL_Line_Gen12_Name());
                ps.setInt(40, entity.getPL_Line_Gen12_Sortierung());
                ps.setString(41, entity.getPL_Line_Gen13());
                ps.setString(42, entity.getPL_Line_Gen13_Name());
                ps.setInt(43, entity.getPL_Line_Gen13_Sortierung());
                ps.setString(44, entity.getPL_Line_Gen14());
                ps.setString(45, entity.getPL_Line_Gen14_Name());
                ps.setInt(46, entity.getPL_Line_Gen14_Sortierung());
                ps.setString(47, entity.getPL_Line_Gen15());
                ps.setString(48, entity.getPL_Line_Gen15_Name());
                ps.setInt(49, entity.getPL_Line_Gen15_Sortierung());


            });
            return Constants.OK;
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }


    }
}
