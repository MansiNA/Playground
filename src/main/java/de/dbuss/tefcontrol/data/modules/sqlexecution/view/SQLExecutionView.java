package de.dbuss.tefcontrol.data.modules.sqlexecution.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.zaxxer.hikari.HikariDataSource;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.modules.sqlexecution.entity.Configuration;
import de.dbuss.tefcontrol.data.modules.sqlexecution.entity.SqlDefinition;
import de.dbuss.tefcontrol.data.modules.sqlexecution.entity.TableInfo;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("SQL EXECUTION")
@Route(value = "SQL_Execution/:project_Id", layout= MainLayout.class)
@RolesAllowed({"ADMIN"})
public class SQLExecutionView extends VerticalLayout {

    private String exportPath;
    private ProjectConnectionService projectConnectionService;
    private ProjectParameterService projectParameterService;
  //  private ConfigurationService service;
    private JdbcTemplate jdbcTemplate;
    private static ComboBox<Configuration> comboBox;
    private TreeGrid<SqlDefinition> treeGrid;

    //private Article descriptionTextField;
    private TextArea sqlTextField;
    private Details queryDetails;
    public static Connection conn;
    private ResultSet resultset;
    private Button exportButton = new Button("Export");
    private Button runButton = new Button("Run");
    private String aktuelle_SQL="";

    //private String aktuelle_Tabelle="";
    Grid<Map<String, Object>> grid2 = new Grid<>();
    List<Map<String,Object>> rows;

    // PaginatedGrid<String, Object> grid = new PaginatedGrid<>();
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private String configTable;
    private String sqlTable;
    private List<SqlDefinition> sqlDefinitionList;

    public SQLExecutionView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService, @Value("${sqlcsv_exportPath}") String p_exportPath, JdbcTemplate jdbcTemplate) throws SQLException, IOException {
        //add(new H1("Table View"));
        this.projectConnectionService = projectConnectionService;
        this.projectParameterService = projectParameterService;
        this.exportPath = p_exportPath;
        this.jdbcTemplate = jdbcTemplate;

        System.out.println("Export Path: " + exportPath);
        exportButton.setVisible(false);
        runButton.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        List<ProjectParameter> filteredProjectParameters = listOfProjectParameters.stream()
                .filter(projectParameter -> Constants.SQL_EXECUTION.equals(projectParameter.getNamespace()))
                .collect(Collectors.toList());

        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : filteredProjectParameters) {
            if (Constants.DB_URL.equals(projectParameter.getName())) {
                dbUrl = projectParameter.getValue();
            } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                dbUser = projectParameter.getValue();
            } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                dbPassword = projectParameter.getValue();
            } else if (Constants.TABLE.equals(projectParameter.getName())) {
                configTable = projectParameter.getValue();
            } else if (Constants.SQL_TABLE.equals(projectParameter.getName())) {
                sqlTable = projectParameter.getValue();
            }
        }

     //   dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        comboBox = new ComboBox<>("Verbindung");
        
        try {
            List<Configuration> configList = projectConnectionService.getConfigurationFromEKPMonitor(dbUrl, dbUser, dbPassword, configTable);
            if (configList != null && !configList.isEmpty()) {
                comboBox.setItems(configList);
                comboBox.setItemLabelGenerator(Configuration::getName);
                comboBox.setValue(configList.get(0));
            }
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
        // Add value change listener to comboBox
        comboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                runButton.setEnabled(true);
            }
        });

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(comboBox);

        hl.setAlignItems(Alignment.BASELINE);

        setSizeFull();
        // add(hl);

        //Export Button

        exportButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        exportButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        exportButton.addClickListener(clickEvent -> {

            Notification.show("Exportiere Daten" );
            generateExcelFile(rows, exportPath + "query.xlsx");
        });

        runButton.addClickListener(clickEvent -> {
            try {
                show_grid(sqlTextField.getValue());
                exportButton.setVisible(true);
                runButton.setEnabled(false);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        HorizontalLayout treehl = new HorizontalLayout();


        TreeGrid tg= createTreeGrid();

        treehl.add(tg, createSQLTextField());
        treehl.setFlexGrow(1, tg);

        //  treehl.setWidthFull();
        treehl.setAlignItems(Alignment.BASELINE);

        queryDetails = new Details("SQL Auswahl",treehl);
        queryDetails.setOpened(true);
        queryDetails.setWidthFull();
        queryDetails.setSummaryText("Bitte Abfrage auswählen");


        add(hl, queryDetails);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(runButton, exportButton);
        horizontalLayout.setAlignItems(Alignment.BASELINE);
        add(horizontalLayout, grid2);
    }

    /*
    private Article createDescriptionTextField() {
        descriptionTextField = new Article();
      //  descriptionTextField.setReadOnly(true); // Set as read-only as per your requirement
        descriptionTextField.setWidthFull();
        descriptionTextField.setText("Bitte Abfrage auswählen.");
        return descriptionTextField;
    }

     */

    private TextArea createSQLTextField() {
        sqlTextField = new TextArea("Abfrage:");
        sqlTextField.setReadOnly(true); // Set as read-only as per your requirement
        //sqlTextField.setMaxLength(2000);
        sqlTextField.setWidth("900px");
        //  sqlTextField.setEnabled(false);
        sqlTextField.setHelperText("SQL");

        // sqlTextField.addClassName("no-boarder");
        sqlTextField.getStyle().set("--vaadin-input-field-readonly-border", "0px");

        //   sqlTextField.getStyle().set("--vaadin-input-field-label-font-weight", "800");

        sqlTextField.addClassName("my-special-classname");

        sqlTextField.addThemeVariants(
                TextAreaVariant.LUMO_SMALL,
                // TextAreaVariant.LUMO_ALIGN_RIGHT,
                TextAreaVariant.LUMO_HELPER_ABOVE_FIELD
        );


        return sqlTextField;
    }
    private TreeGrid createTreeGrid() {
        treeGrid = new TreeGrid<>();
        sqlDefinitionList = projectConnectionService.getSqlDefinitions(dbUrl,dbUser,dbPassword, sqlTable);
      //  treeGrid.setItems(sqlDefinitionService.getRootProjects(), sqlDefinitionService ::getChildProjects);
       // treeGrid.setItems(getRootProjects(), this::getChildProjects);
        treeGrid.setItems(projectConnectionService.getRootProjects(), projectConnectionService::getChildProjects);
        treeGrid.addHierarchyColumn(SqlDefinition::getName);
        treeGrid.getColumns().forEach(col -> col.setAutoWidth(true));

        treeGrid.setWidth("350px");
        treeGrid.addExpandListener(event->
                System.out.println(String.format("Expanded %s item(s)",event.getItems().size()))
        );
        treeGrid.addCollapseListener(event->
                System.out.println(String.format("Collapsed %s item(s)",event.getItems().size()))
        );

        treeGrid.setThemeName("dense");
        treeGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        treeGrid.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS);
        treeGrid.addThemeVariants(GridVariant.LUMO_COMPACT);

        treeGrid.asSingleSelect().addValueChangeListener(event->{

            SqlDefinition selectedItem=event.getValue();
            if(selectedItem != null){
                String sql = selectedItem.getSql();
                if(sql == null) {
                    sql = "";
                }

                queryDetails.setSummaryText(selectedItem.getName() + ": " + selectedItem.getBeschreibung());

                sqlTextField.setValue(sql);
                System.out.println("jetzt Ausführen: " + selectedItem.getSql());
                aktuelle_SQL = sql;
                // aktuelle_Tabelle = selectedItem.getName();

                runButton.setEnabled(true);
            }
        });

        if(MainLayout.isAdmin) {
            GridContextMenu<SqlDefinition> contextMenu = treeGrid.addContextMenu();
            GridMenuItem<SqlDefinition> editItem = contextMenu.addItem("Edit", event -> {
                showEditAndNewDialog(event.getItem().get(), "Edit");
            });
            GridMenuItem<SqlDefinition> newItem = contextMenu.addItem("New", event -> {
                showEditAndNewDialog(event.getItem().get(), "New");
            });
            GridMenuItem<SqlDefinition> deleteItem = contextMenu.addItem("Delete", event -> {
                deleteTreeGridItem(event.getItem().get());
            });
        }
        return treeGrid;
    }

    private VerticalLayout showEditAndNewDialog(SqlDefinition sqlDefinition, String context){
        VerticalLayout dialogLayout = new VerticalLayout();
        Dialog dialog = new Dialog();
        SqlDefinition newSqlDefinition = new SqlDefinition();

        if(context.equals("New")){
         //   List<SqlDefinition> sqlDefinitionList = sqlDefinitionService.getAllSqlDefinitions();
            newSqlDefinition.setId((long) (sqlDefinitionList.size() + 1));
            newSqlDefinition.setPid(sqlDefinition.getPid());
            dialog.add(editSqlDefination(newSqlDefinition, true)); // For adding new entry
        } else {
            dialog.add(editSqlDefination(sqlDefinition, false)); // For editing existing entry
        }

        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("1000px");
        dialog.setHeight("400px");
        Button cancelButton = new Button("Cancel");
        Button saveButton = new Button(context.equals("Edit") ? "Save" : "Add");
        dialog.getFooter().add(saveButton, cancelButton);

        cancelButton.addClickListener(cancelEvent -> {
            dialog.close(); // Close the confirmation dialog
        });

        saveButton.addClickListener(saveEvent -> {
            System.out.println("saved data....");
            if(context.equals("New")) {
                saveSqlDefinition(newSqlDefinition);
            } else {
                saveSqlDefinition(sqlDefinition);
            }
          //  sqlDefinitionService.getAllSqlDefinitions();
            treeGrid.setItems(getRootProjects(), this ::getChildProjects);
            //   rows = retrieveRows();
            // treeg.setItems(param_Liste);
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();

        return dialogLayout;

    }

    private Component editSqlDefination(SqlDefinition sqlDefinition, boolean isNew) {
        VerticalLayout content = new VerticalLayout();

        TextField name = new TextField("NAME");
        name.setValue(isNew ? "" : (sqlDefinition.getName() != null ? sqlDefinition.getName() : ""));
        name.setWidthFull();

        TextField sql = new TextField("SQL");
        sql.setValue(isNew ? "" : (sqlDefinition.getSql() != null ? sqlDefinition.getSql() : ""));
        sql.setWidthFull();

        TextField beschreibung = new TextField("BESCHREIBUNG");
        beschreibung.setValue(isNew ? "" : (sqlDefinition.getBeschreibung() != null ? sqlDefinition.getBeschreibung() : ""));
        beschreibung.setWidthFull();

        TextField pid = new TextField("PID");
        pid.setValue(sqlDefinition.getPid() != null ? sqlDefinition.getPid().toString() : "");
        pid.setWidthFull();

        MultiSelectComboBox<String> rolesComboBox = new MultiSelectComboBox<>("Roles");
       // List<SqlDefinition> sqlDefinitionList = sqlDefinitionService.getAllSqlDefinitions();
        rolesComboBox.setWidthFull();

        // Collect unique access roles from all SqlDefinition items
        Set<String> uniqueAccessRoles = sqlDefinitionList.stream()
                .map(SqlDefinition::getAccessRoles)
                .filter(Objects::nonNull)
                .flatMap(accessRoles -> Arrays.stream(accessRoles.split(",")))
                .map(String::trim)
                .collect(Collectors.toSet());
        rolesComboBox.setItems(uniqueAccessRoles);

        String accessRoles = sqlDefinition.getAccessRoles();
        if(accessRoles != null) {
            rolesComboBox.setValue(accessRoles.split(","));
        }

        // Add value change listeners to trigger binder updates
        name.addValueChangeListener(event -> sqlDefinition.setName(event.getValue()));
        sql.addValueChangeListener(event -> sqlDefinition.setSql(event.getValue()));
        beschreibung.addValueChangeListener(event -> sqlDefinition.setBeschreibung(event.getValue()));
        pid.addValueChangeListener(event -> {
            try {
                if (event.getValue() != null && !event.getValue().isEmpty()) {
                    Long pidValue = Long.parseLong(event.getValue());
                    sqlDefinition.setPid(pidValue);
                } else {
                    sqlDefinition.setPid(null);
                }
            } catch (NumberFormatException e) {
                Notification.show(e.getCause().getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });

        rolesComboBox.addValueChangeListener(event -> {
            String selectedRolesString = String.join(",", event.getValue());
            sqlDefinition.setAccessRoles(selectedRolesString);
        });

        content.add(name,sql,beschreibung, pid, rolesComboBox);
        return content;
    }

    private Component deleteTreeGridItem(SqlDefinition sqlDefinition) {

        VerticalLayout dialogLayout = new VerticalLayout();
        Dialog dialog = new Dialog();
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("500px");
        dialog.setHeight("150px");
        Button cancelButton = new Button("Cancel");
        Button deleteButton = new Button("Delete");
        Text deleteConfirmationText = new Text("Are you sure you want to delete?");
        dialog.add(deleteConfirmationText);
        dialog.getFooter().add(deleteButton, cancelButton);

        cancelButton.addClickListener(cancelEvent -> {
            dialog.close(); // Close the confirmation dialog
        });

        deleteButton.addClickListener(saveEvent -> {
       //     sqlDefinitionService.deleteSqlDefinitionById(sqlDefinition.getId());
        //    sqlDefinitionService.getAllSqlDefinitions();
        //    treeGrid.setItems(sqlDefinitionService.getRootProjects(), sqlDefinitionService ::getChildProjects);
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();

        return dialogLayout;
    }

  /*  private Tree createTree(){
        Tree<SqlDefinition> tree = new Tree<>(
                SqlDefinition::getName);
        System.out.println(sqlDefinitionService.getRootProjects().size()+"..............vvvvvvvvvvvvvvvvvvvvvvvvvv");

        tree.setAllRowsVisible(true);
        tree.setItems(sqlDefinitionService.getRootProjects(),
                sqlDefinitionService::getChildProjects);
    //    tree.setItemIconProvider(item -> getIcon(item));
     //   tree.setItemIconSrcProvider(item -> getImageIconSrc(item));
      //  tree.setItemTitleProvider(SqlDefinition::getManager);

        tree.addExpandListener(event->
                System.out.println(String.format("Expanded%sitem(s)",event.getItems().size()))
        );
        tree.addCollapseListener(event->
                System.out.println(String.format("Collapsed%sitem(s)",event.getItems().size()))
        );
        tree.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null)
                System.out.println(event.getValue().getName() + " selected");
        });
        tree.setHeight("350px"); //tree.addClassNames("text-l","m-m");
        tree.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.Margin.NONE);



        // add(tree);
        return tree;
    }
    private void createTreeOld(){

        Tree<SqlDefinition> tree=new Tree<>(SqlDefinition::getName);
        System.out.println(sqlDefinitionService.getAllSqlDefinitions()+"#######################");
        tree.setItems(sqlDefinitionService.getRootProjects(),sqlDefinitionService::getChildProjects);

        tree.addExpandListener(event->
                System.out.println(String.format("Expanded%sitem(s)",event.getItems().size()))
        );
        tree.addCollapseListener(event->
                System.out.println(String.format("Collapsed%sitem(s)",event.getItems().size()))
        );
        tree.asSingleSelect().addValueChangeListener(event->{

            SqlDefinition selectedItem=event.getValue();
            if(selectedItem!=null){
                System.out.println("where..........");
            }
        });
        tree.setAllRowsVisible(true);

        tree.addClassNames(LumoUtility.FontSize.XXSMALL,LumoUtility.Margin.NONE);

        add(tree);
    }
*/


    private InputStream getStream(File file) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return stream;
    }

    public StreamResource getStreamResource(String filename, String content) {
        return new StreamResource(filename,
                () -> new ByteArrayInputStream(content.getBytes()));
    }

    public static void fileOutputStreamByteSingle(String file, String data) throws IOException {
        byte[] bytes = data.getBytes();
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
    }


    private void show_grid(String sql) throws SQLException, IOException {

        System.out.println("Execute SQL: " + sql );

        // Create the grid and set its items
        //Grid<LinkedHashMap<String, Object>> grid2 = new Grid<>();
        grid2.removeAllColumns();

        //List<LinkedHashMap<String,Object>> rows = retrieveRows("select * from EKP.ELA_FAVORITEN where rownum<200");
        rows = retrieveRows(sql);

        if(!rows.isEmpty()){
            grid2.setItems( rows); // rows is the result of retrieveRows

            // Add the columns based on the first row
            Map<String, Object> s = rows.get(0);
            for (Map.Entry<String, Object> entry : s.entrySet()) {
                grid2.addColumn(h -> h.get(entry.getKey().toString())).setHeader(entry.getKey()).setAutoWidth(true).setResizable(true).setSortable(true);
            }

            grid2.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
            grid2.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            grid2.addThemeVariants(GridVariant.LUMO_COMPACT);
            //   grid2.setAllRowsVisible(true);
            grid2.setPageSize(50);
            grid2.setHeight("800px");

            grid2.getStyle().set("resize", "vertical");
            grid2.getStyle().set("overflow", "auto");
            grid2.setThemeName("dense");

            //grid2.setPaginatorSize(5);
            // Add the grid to the page


//            this.setPadding(false);
//            this.setSpacing(false);
//            this.setBoxSizing(BoxSizing.CONTENT_BOX);

        }
        else {
            //Text txt = new Text("Es konnten keine Daten  abgerufen werden!");
            //add(txt);
        }

    }

    public List<Map<String,Object>> retrieveRows(String queryString) {
        List<Map<String, Object>> rows;
        if (queryString != null) {
            try {
                Configuration conf = comboBox.getValue();
                String password = Configuration.decodePassword(conf.getPassword());
                DataSource dataSource = getDataSourceUsingParameter(conf.getDb_Url(), conf.getUserName(), password);
                jdbcTemplate = new JdbcTemplate(dataSource);
                rows = jdbcTemplate.queryForList(queryString);
                connectionClose(jdbcTemplate);
                return rows;
            } catch (Exception e) {
                e.printStackTrace();
                Notification.show(e.getCause().getMessage(), 5000, Notification.Position.MIDDLE);
            }
        }

        return Collections.emptyList();
    }

    public void saveSqlDefinition(SqlDefinition sqlDefinition) {
     //   sqlDefinitionService.saveSqlDefinition(sqlDefinition);
    }

    public List<LinkedHashMap<String,Object>> retrieveRows_old(String queryString) throws SQLException, IOException {

        List<LinkedHashMap<String, Object>> rows = new LinkedList<LinkedHashMap<String, Object>>();

        if(queryString != null) {

            PreparedStatement s = null;
            ResultSet rs = null;
            try {
                //    String url="jdbc:oracle:thin:@37.120.189.200:1521:xe";
                //    String user="SYSTEM";
                //    String password="Michael123";

                DriverManagerDataSource ds = new DriverManagerDataSource();
                Configuration conf;
                conf = comboBox.getValue();

                Class.forName("oracle.jdbc.driver.OracleDriver");
                String password = Configuration.decodePassword(conf.getPassword());

                //    Connection conn=DriverManager.getConnection(url, user, password);
                Connection conn = DriverManager.getConnection(conf.getDb_Url(), conf.getUserName(), password);


                s = conn.prepareStatement(queryString);

                int timeout = s.getQueryTimeout();
                if (timeout != 0)
                    s.setQueryTimeout(0);

                rs = s.executeQuery();


                List<String> columns = new LinkedList<>();
                ResultSetMetaData resultSetMetaData = rs.getMetaData();
                int colCount = resultSetMetaData.getColumnCount();
                for (int i = 1; i < colCount + 1; i++) {
                    columns.add(resultSetMetaData.getColumnLabel(i));
                }

                while (rs.next()) {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<String, Object>();
                    for (String col : columns) {
                        int colIndex = columns.indexOf(col) + 1;
                        String object = rs.getObject(colIndex) == null ? "" : String.valueOf(rs.getObject(colIndex));
                        row.put(col, object);
                    }

                    rows.add(row);
                }
            } catch (SQLException | IllegalArgumentException | SecurityException e) {
                // e.printStackTrace();
                // add(new Text(e.getMessage()));

                Notification notification = Notification.show(e.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

                return Collections.emptyList();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {

                try {
                    rs.close();
                } catch (Exception e) { /* Ignored */ }
                try {
                    s.close();
                } catch (Exception e) { /* Ignored */ }
                try {
                    conn.close();
                } catch (Exception e) { /* Ignored */ }


            }
        }
        return rows;
    }

    private void generateExcelFile(List<Map<String, Object>> rows, String fileName) {
        // Create a new Excel workbook and sheet
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");

        // Create a header row with column names
        Row headerRow = sheet.createRow(0);
        int cellIndex = 0;
        for (String columnName : rows.get(0).keySet()) {
            Cell cell = headerRow.createCell(cellIndex++);
            cell.setCellValue(columnName);
        }

        // Populate the data rows
        int rowIndex = 1;
        for (Map<String, Object> row : rows) {
            Row dataRow = sheet.createRow(rowIndex++);
            cellIndex = 0;
            for (Object value : row.values()) {
                Cell cell = dataRow.createCell(cellIndex++);
                if (value != null) {
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else {
                        // Handle other data types as needed
                        cell.setCellValue(value.toString());
                    }
                } else {
                    cell.setCellValue(""); // Handle null values as empty strings
                }
            }
        }

        try {

            // Write the workbook to the specified file
            try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                workbook.write(fileOut);
            } catch (Exception e) {
                // e.printStackTrace();
                Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            }
            // Close the workbook
            workbook.close();

            File file = new File(fileName);
            StreamResource streamResource = new StreamResource(file.getName(), () -> getStream(file));

            Anchor anchor = new Anchor(streamResource, "");
            anchor.getElement().setAttribute("download", true);
            anchor.getElement().getStyle().set("display", "none");
            add(anchor);
            UI.getCurrent().getPage().executeJs("arguments[0].click()", anchor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateExcel(String file, String query) throws IOException {
        Configuration conf;
        conf = comboBox.getValue();

        try {
            //String url="jdbc:oracle:thin:@37.120.189.200:1521:xe";
            //String user="SYSTEM";
            //String password="Michael123";




            Class.forName("oracle.jdbc.driver.OracleDriver");
            String password = Configuration.decodePassword(conf.getPassword());
            //    Connection conn=DriverManager.getConnection(url, user, password);
            Connection conn=DriverManager.getConnection(conf.getDb_Url(), conf.getUserName(), password);

            //   DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());


            PreparedStatement stmt=null;
            //Workbook
            HSSFWorkbook workBook=new HSSFWorkbook();
            HSSFSheet sheet1=null;

            //Cell
            Cell c=null;

            CellStyle cs=workBook.createCellStyle();
            HSSFFont f =workBook.createFont();
            f.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            f.setFontHeightInPoints((short) 12);
            cs.setFont(f);


            sheet1=workBook.createSheet("Sheet1");


            // String query="select  EMPNO, ENAME, JOB, MGR, HIREDATE, SAL, COMM, DEPTNO, WORK_CITY, WORK_COUNTRY from APEX_040000.WWV_DEMO_EMP";
            stmt=conn.prepareStatement(query);
            ResultSet rs=stmt.executeQuery();

            ResultSetMetaData metaData=rs.getMetaData();
            int colCount=metaData.getColumnCount();

            LinkedHashMap<Integer, TableInfo> hashMap=new LinkedHashMap<Integer, TableInfo>();


            for(int i=0;i<colCount;i++){
                TableInfo tableInfo=new TableInfo();
                tableInfo.setFieldName(metaData.getColumnName(i+1).trim());
                tableInfo.setFieldText(metaData.getColumnLabel(i+1));
                tableInfo.setFieldSize(metaData.getPrecision(i+1));
                tableInfo.setFieldDecimal(metaData.getScale(i+1));
                tableInfo.setFieldType(metaData.getColumnType(i+1));
                //     tableInfo.setCellStyle(getCellAttributes(workBook, c, tableInfo));

                hashMap.put(i, tableInfo);
            }

            //Row and Column Indexes
            int idx=0;
            int idy=0;

            HSSFRow row=sheet1.createRow(idx);
            TableInfo tableInfo=new TableInfo();

            Iterator<Integer> iterator=hashMap.keySet().iterator();

            while(iterator.hasNext()){
                Integer key=(Integer)iterator.next();

                tableInfo=hashMap.get(key);
                c=row.createCell(idy);
                c.setCellValue(tableInfo.getFieldText());
                c.setCellStyle(cs);
                if(tableInfo.getFieldSize() > tableInfo.getFieldText().trim().length()){
                    sheet1.setColumnWidth(idy, (tableInfo.getFieldSize()* 10));
                }
                else {
                    sheet1.setColumnWidth(idy, (tableInfo.getFieldText().trim().length() * 5));
                }
                idy++;
            }

            while (rs.next()) {

                idx++;
                row = sheet1.createRow(idx);
                //  System.out.println(idx);
                for (int i = 0; i < colCount; i++) {

                    c = row.createCell(i);
                    tableInfo = hashMap.get(i);

                    switch (tableInfo.getFieldType()) {
                        case 1:
                            c.setCellValue(rs.getString(i+1));
                            break;
                        case 2:
                            c.setCellValue(rs.getDouble(i+1));
                            break;
                        case 3:
                            c.setCellValue(rs.getDouble(i+1));
                            break;
                        default:
                            c.setCellValue(rs.getString(i+1));
                            break;
                    }
                    c.setCellStyle(tableInfo.getCellStyle());
                }

            }
            rs.close();
            stmt.close();
            conn.close();

            // String path="c:\\tmp\\test.xls";

            FileOutputStream fileOut = new FileOutputStream(file);

            workBook.write(fileOut);
            fileOut.close();


        } catch (SQLException | FileNotFoundException e) {
            System.out.println("Error in Method generateExcel(String file, String query) file: " + file + " query: "  + query);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
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

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        Connection connection = null;
        DataSource dataSource = null;
        try {
            // Retrieve the connection from the DataSource
            connection = jdbcTemplate.getDataSource().getConnection();
            dataSource = jdbcTemplate.getDataSource();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();

                    if (dataSource instanceof HikariDataSource) {
                        ((HikariDataSource) dataSource).close();
                    }

                } catch (SQLException e) {

                    e.printStackTrace();
                }
            }
        }
    }

    public List<SqlDefinition> getRootProjects() {

        List<SqlDefinition> rootProjects = sqlDefinitionList
                .stream()
                .filter(sqlDef -> {
                    System.out.println("pid: " + sqlDef.getPid());
                    System.out.println("accessRoles: " + sqlDef.getAccessRoles());
                    return sqlDef.getPid() == null;
                })
                .filter(projects -> {
                    System.out.println("accessRoles: " + projects.getAccessRoles());
                    return hasAccess(projects.getAccessRoles());
                })
                .collect(Collectors.toList());

        // Log the names of root projects
        rootProjects.forEach(project -> System.out.println("Root Project: " + project.getName()));

        return rootProjects;
    }

    public List<SqlDefinition> getChildProjects(SqlDefinition parent) {

        List<SqlDefinition> childProjects = sqlDefinitionList
                .stream()
                .filter(sqlDef -> {
                    System.out.println("Child Project - ID: " + sqlDef.getId() + ", PID: " + sqlDef.getPid());
                    return Objects.equals(sqlDef.getPid(), parent.getId());
                })
                .filter(projects -> {
                    System.out.println("child accessRoles: " + projects.getAccessRoles());
                    return hasAccess(projects.getAccessRoles());
                })
                .collect(Collectors.toList());

        // Log the names of child projects
        childProjects.forEach(project -> System.out.println("Child Project of " + parent.getName() + ": " + project.getName()));

        return childProjects;
    }

    private boolean hasAccess(String projectRoles) {
        System.out.println("before project role"+projectRoles);
        if (projectRoles != null) {
            System.out.println("project role"+projectRoles);
            String[] roleList = projectRoles.split(",");
            // here noted rolelist have ADMIN, USER like that
            // here noted userRoles have ROLE_ADMIN, ROLE_USER like that
            for (String role : roleList) {
                for (String userRole : MainLayout.userRoles) {
                    if (userRole.contains(role)){
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
