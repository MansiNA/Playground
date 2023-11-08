package de.dbuss.tefcontrol.data.service;

import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CLTVInflow;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.*;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.CltvAllProduct;
import de.dbuss.tefcontrol.data.modules.pfgproductmapping.entity.ProductHierarchie;
import de.dbuss.tefcontrol.data.repository.ProjectConnectionRepository;
import de.dbuss.tefcontrol.data.modules.techkpi.view.Tech_KPIView;
import lombok.Getter;
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

    @Getter
    private String errorMessage = "";

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
            return "ok";
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
            return "ok";
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
            return "ok";
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

    public String saveKPIFact(List<Tech_KPIView.KPI_Fact> data) {

        try {

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

    public void deleteKPIPlan(String selectedDatabase) {
        getJdbcConnection(selectedDatabase);
        try {
            String sqlDelete = "DELETE FROM [Stage_Tech_KPI].[KPI_Plan]";
            jdbcTemplate.update(sqlDelete);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = handleDatabaseError(e);
        }
    }
    public String saveKPIPlan(List<Tech_KPIView.KPI_Plan> data) {

        try {
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
    public String saveKPIActuals(List<Tech_KPIView.KPI_Actuals> data) {

        try {
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

    public List<CltvAllProduct> getCltvAllProducts(String productDb) {

        String dbConnection="";
        String dataBase="";

        try {
            //String[] dbName = productDb.split("\\.");
            String[] parts = productDb.split(":");

            if (parts.length == 2) {
                dbConnection = parts[0];
                dataBase = parts[1];
            } else
            {
                System.out.println("ERROR: No Connection/Table for CltvAllProducts!");
            }

            getJdbcConnection(dbConnection);

            //String sql = "SELECT [all_products], [all_products_gen_number], [all_products_gen2], [verarb_datum] FROM " + dataBase;
            String sql = "SELECT distinct [All_Products] FROM " + dataBase;

            List<CltvAllProduct> clatvAllProductList = jdbcTemplate.query(sql, (rs, rowNum) -> {
                CltvAllProduct cltvAllProduct = new CltvAllProduct();
                cltvAllProduct.setAllProducts(rs.getString("All_Products"));
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

    public List<ProductHierarchie> fetchProductHierarchie(String selectedDatabase, String targetTable, String filterValue) {

        try {
            getJdbcConnection(selectedDatabase);

            // Construct the dynamic SQL query with multiple OR conditions for filtering
            String sqlQuery = "SELECT * FROM " + targetTable + " WHERE Product LIKE ?" +
                    " OR [PFG_PO/PP] LIKE ?" +
                    " OR Knoten LIKE ?";

            // Create a RowMapper to map the query result to a ProductHierarchie object
            RowMapper<ProductHierarchie> rowMapper = (rs, rowNum) -> {
                ProductHierarchie productHierarchie = new ProductHierarchie();
                productHierarchie.setId(rs.getLong("id"));
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

    public String saveProductHierarchie(ProductHierarchie data, String selectedDatabase, String targetTable) {
        try {
            getJdbcConnection(selectedDatabase);

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

            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }

    }

    public List<String> getAllMissingProducts(String selectedDatabase, String targetView) {
        try {
            getJdbcConnection(selectedDatabase);
            String sqlQuery = "SELECT * FROM " + targetView;

            RowMapper<String> rowMapper = (rs, rowNum) -> {
                return rs.getString("All_Products");
            };

            List<String> fetchedData = jdbcTemplate.query(sqlQuery, rowMapper);

            return fetchedData;
        } catch (Exception ex) {
            ex.printStackTrace();
            errorMessage = handleDatabaseError(ex);
            return Collections.emptyList();
        }

    }

    public String saveListOfProductHierarchie(List<ProductHierarchie> data, String selectedDatabase, String targetTable) {
        try {
            getJdbcConnection(selectedDatabase);
            String sql = "INSERT INTO " + targetTable + " (Product, [PFG_PO/PP], Knoten) VALUES (?, ?, ?)";

            jdbcTemplate.batchUpdate(sql, data, data.size(), (ps, productHierarchie) -> {
                ps.setString(1, productHierarchie.getProduct_name());
                ps.setString(2, productHierarchie.getPfg_Type());
                ps.setString(3, productHierarchie.getNode());
            });

            return "ok";
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

            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }


    public String saveOutlookMGSR(List<OutlookMGSR> data, String tableName, String dbUrl, String dbUser, String dbPassword) {

        try {

            DataSource dataSource = getDataSourceUsingParameter(dbUrl, dbUser, dbPassword);
            jdbcTemplate = new JdbcTemplate(dataSource);

            String sqlDelete = "DELETE FROM " + tableName;

            jdbcTemplate.update(sqlDelete);

            String sqlInsert = "INSERT INTO "+ tableName +" (Zeile, Blatt, month, PL_Line, profit_center, scenario, block, segment, payment_type, type_of_data, value) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                ps.setDouble(11, entity.getValue());
              //  java.sql.Date sqlDate = (entity.getLoadDate() != null) ? new java.sql.Date(entity.getLoadDate().getTime()) : null;
              //  ps.setDate(12, sqlDate);
            });

            return "ok";
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

            return "ok";
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

            return "ok";
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

            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return handleDatabaseError(e);
        }
    }
}
