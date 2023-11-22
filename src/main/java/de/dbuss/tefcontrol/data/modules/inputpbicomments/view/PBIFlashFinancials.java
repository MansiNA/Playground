package de.dbuss.tefcontrol.data.modules.inputpbicomments.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.FlashFinancials;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.ITOnlyComment;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.KPIsComment;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.XPexComment;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.sql.DataSource;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "PBI_FlashFinancials/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING"})
public class PBIFlashFinancials extends VerticalLayout implements BeforeEnterObserver {

    private final ProjectConnectionService projectConnectionService;
    private MemoryBuffer memoryBuffer = new MemoryBuffer();
    private Upload singleFileUpload = new Upload(memoryBuffer);
    private int projectId;
    private List<FlashFinancials> listOfFlashFinancials = new ArrayList<>();
    private Crud<FlashFinancials> crudFlashFinancials;
    private Grid<FlashFinancials> gridFlashFinancials = new Grid<>(FlashFinancials.class);
    private long contentLength = 0;
    private String mimeType = "";
    private Div textArea = new Div();
    private Button login;
    private Button qsBtn;
    private Button uploadBtn;
    private QS_Grid qsGrid;
    private String dbUser;
    private String dbPassword;
    private String dbJob = null;
    private String tableName = null;
    private String dbUrl;
    private String idKey = Constants.ZEILE;

    public PBIFlashFinancials(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService) {

        this.projectConnectionService = projectConnectionService;

        Div htmlDiv = new Div();
        htmlDiv.getElement().setProperty("innerHTML", "<h2>Input Frontend for Flash Financials Comments");
        add(htmlDiv);

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);

        login = new Button("Login");
        login.addClickListener(e -> {
            System.out.println("Login clicked...");

            String lpadUrl="ldap://viaginterkom.de:389";
            String lpadUser="mquaschn@viaginterkom.de";
            String lpadPassword="Juniper_16";
            DirContext context = connectToLpad(lpadUrl, lpadUser, lpadPassword);
            if (context != null){
                System.out.println("User " + lpadUser + " connected");
            }
            else
            {
                System.out.println("Fehler beim Verbinden mit LPAD-Server "  + lpadUrl);
            }
        });


        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.PBI_FLASH_FINANCIALS)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    dbName = projectParameter.getValue();
                } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                    dbUser = projectParameter.getValue();
                } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                    dbPassword = projectParameter.getValue();
                } else if (Constants.DB_JOBS.equals(projectParameter.getName())) {
                    dbJob = projectParameter.getValue();
                } else if (Constants.TABLE.equals(projectParameter.getName())) {
                    tableName = projectParameter.getValue();
                }
            }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";

        //    Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName+ ", Table xPEX: " + xPexTableName + ", Table IT only: " + iTOnlyTableName+ ", Table KPIs: "+ kPIsTableName);
        Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName) ;

        //Componente QS-Grid:
        qsGrid = new QS_Grid(projectConnectionService);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(FlexComponent.Alignment.BASELINE);

        //  hl.add(singleFileUpload,qsBtn,saveButton, databaseDetail, qsDialog, login);
        hl.add(singleFileUpload,uploadBtn,qsBtn, databaseDetail, qsGrid, login);
        add(hl);

        uploadBtn.addClickListener(e ->{
            save2db();
            qsBtn.setEnabled(true);
        });

        qsBtn.addClickListener(e ->{

            if (qsGrid.projectId != projectId) {
                CallbackHandler callbackHandler = new CallbackHandler();
                qsGrid.createDialog(callbackHandler, projectId);
            }
            qsGrid.showDialog(true);
        });

        setupUploader();
        add(getXPexComment());
        setHeightFull();


    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        projectId = Integer.parseInt(parameters.get("project_Id").orElse(null));
    }

    // Die Klasse, die die Callback-Methode implementiert
    public class CallbackHandler implements QS_Callback {
        // Die Methode, die aufgerufen wird, wenn die externe Methode abgeschlossen ist
        @Override
        public void onComplete(String result) {

            if(!result.equals("Cancel")) {
                String message = projectConnectionService.startAgent(projectId);
                if (!message.contains("Error")) {
                    Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    Notification.show(message, 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }

        }
    }

    private void save2db(){
        List<FlashFinancials> flashFinancialsData = getFlashFinancialsProviderAllItems();

        Notification notification = new Notification();

        String resultFinancial = projectConnectionService.saveFlashFinancials(flashFinancialsData, tableName, dbUrl, dbUser, dbPassword);
        if (resultFinancial.contains("ok")) {
            notification = Notification.show(flashFinancialsData.size() + " Flash Financials Rows Uploaded successfully", 6000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            notification = Notification.show("Error during Flash Financials upload: " + resultFinancial, 15000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private DirContext connectToLpad(String ldapUrl, String ldapUser, String ldapPassword) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_PRINCIPAL, ldapUser);
        env.put(Context.SECURITY_CREDENTIALS, ldapPassword);


        try {
            return new InitialDirContext(env);
        } catch (NamingException e) {
            e.printStackTrace();
            return null;
        }

    }
    private Component getXPexComment() {
        VerticalLayout content = new VerticalLayout();
        crudFlashFinancials = new Crud<>(FlashFinancials.class, createFlashFinancialsEditor());
        setupFlashFinancialsGrid();
        content.add(crudFlashFinancials);

        crudFlashFinancials.setToolbarVisible(false);

        gridFlashFinancials.addItemDoubleClickListener(event -> {
            FlashFinancials selectedEntity = event.getItem();
            crudFlashFinancials.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudFlashFinancials.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudFlashFinancials.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private void setupFlashFinancialsGrid() {

        String ZEILE = "zeile";
        String MONTH = "month";
        String CATEGORY = "category";
        String COMMENT = "comment";
        String SCENARIO = "scenario";
        String XTD = "xtd";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridFlashFinancials = crudFlashFinancials.getGrid();

        gridFlashFinancials.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridFlashFinancials.getColumnByKey(MONTH).setHeader("Month").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridFlashFinancials.getColumnByKey(CATEGORY).setHeader("Category").setWidth("150px").setFlexGrow(0).setResizable(true);
        gridFlashFinancials.getColumnByKey(COMMENT).setHeader("Comment").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridFlashFinancials.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridFlashFinancials.getColumnByKey(XTD).setHeader("XTD").setWidth("80px").setFlexGrow(0).setResizable(true);

        gridFlashFinancials.removeColumn(gridFlashFinancials.getColumnByKey(EDIT_COLUMN));
        gridFlashFinancials.getColumns().forEach(col -> col.setAutoWidth(true));
        // Reorder the columns (alphabetical by default)
        gridFlashFinancials.setColumnOrder( gridFlashFinancials.getColumnByKey(ZEILE)
                , gridFlashFinancials.getColumnByKey(MONTH)
                , gridFlashFinancials.getColumnByKey(COMMENT)
                , gridFlashFinancials.getColumnByKey(CATEGORY)
                , gridFlashFinancials.getColumnByKey(SCENARIO)
                , gridFlashFinancials.getColumnByKey(XTD));

        //    , gridFlashFinancials.getColumnByKey(EDIT_COLUMN));

    }

    private CrudEditor<FlashFinancials> createFlashFinancialsEditor() {

        TextArea comment = new TextArea("Comment");

        comment.setHeight("250px");
        comment.setWidth("1200px");
        FormLayout editForm = new FormLayout(comment);
        editForm.setColspan(comment, 2);

        editForm.setHeight("250px");
        editForm.setWidth("1200px");

        Binder<FlashFinancials> binder = new Binder<>(FlashFinancials.class);
        binder.forField(comment).asRequired().bind(FlashFinancials::getComment, FlashFinancials::setComment);

        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupUploader() {
        singleFileUpload.setWidth("600px");

        singleFileUpload.addSucceededListener(event -> {
            // Get information about the uploaded file
            InputStream fileData = memoryBuffer.getInputStream();
            String fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();

            parseExcelFile(fileData,fileName);

            GenericDataProvider dataFinancialsProvider = new GenericDataProvider(listOfFlashFinancials, idKey);
            gridFlashFinancials.setDataProvider(dataFinancialsProvider);
            singleFileUpload.clearFileList();
            uploadBtn.setEnabled(true);
        });
    }

    private void parseExcelFile(InputStream fileData, String fileName) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        Article article = new Article();

        try {
            if(fileName.isEmpty() || fileName.length()==0)
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: Keine Datei angegeben!");
                textArea.add(article);
            }

            if(!mimeType.contains("openxmlformats-officedocument"))
            {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: ungültiges Dateiformat!");
                textArea.add(article);
            }
            textArea.setText(LocalDateTime.now().format(formatter) + ": Info: Verarbeite Datei: " + fileName + " (" + contentLength + " Byte)");

            XSSFWorkbook my_xls_workbook = new XSSFWorkbook(fileData);

            XSSFSheet sheet1 = my_xls_workbook.getSheet(Constants.B2P_FLASH_FINANCIALS);
            listOfFlashFinancials = parseSheet(sheet1, FlashFinancials.class, 6);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public <T> List<T>  parseSheet(XSSFSheet my_worksheet, Class<T> targetType, int cntColumns) {

        int column=1;
        try {
            List<T> resultList = new ArrayList<>();
            Iterator<Row> rowIterator = my_worksheet.iterator();

            int rowNumber = 0;
            Integer Error_count = 0;
            System.out.println(my_worksheet.getPhysicalNumberOfRows() + "$$$$$$$$$");

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                T entity = targetType.newInstance();
                rowNumber++;

                if (rowNumber == 1) {
                    continue;
                }
                if (row.getCell(0).toString().isEmpty()) {
                    break;
                }

                Field[] fields = targetType.getDeclaredFields();
                //for (int index = 0; index < fields.length; index++) {
                for (int index = 0; index < cntColumns +1 ; index++) {
                    Cell cell = null;
                    if (index != 0) {
                        cell = row.getCell(index - 1);
                    } else {
                        cell = row.getCell(index);
                    }

                    if (cell != null && !cell.toString().isEmpty()) {
                        Field field = fields[index];
                        field.setAccessible(true);

                        if (index == 0) {
                            field.set(entity, rowNumber);
                        } else {
                            if (field.getType() == int.class || field.getType() == Integer.class) {
                                if (cell.getCellType() == cell.CELL_TYPE_NUMERIC) {
                                    // If the cell type is numeric, set the integer value
                                    field.set(entity, (int) cell.getNumericCellValue());
                                } else if (cell.getCellType() == cell.CELL_TYPE_STRING) {
                                    // If the cell type is string, try to parse it as an integer
                                    String cellText = cell.getStringCellValue();
                                    field.set(entity, Integer.parseInt(cellText));
                                }
                            } else if (field.getType() == double.class || field.getType() == Double.class) {
                                field.set(entity, cell.getNumericCellValue());
                            } else if (field.getType() == String.class) {
                                field.set(entity, cell.getStringCellValue());
                            }
                        }
                    }
                }
                resultList.add(entity);
            }
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<FlashFinancials> getFlashFinancialsProviderAllItems() {
        DataProvider<FlashFinancials, Void> existDataProvider = (DataProvider<FlashFinancials, Void>) gridFlashFinancials.getDataProvider();
        List<FlashFinancials> listOfFinancials = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfFinancials;
    }

}
