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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.router.*;
import de.dbuss.tefcontrol.components.QS_Callback;
import de.dbuss.tefcontrol.components.QS_Grid;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.entity.*;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Financials;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.GenericComments;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Subscriber;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.UnitsDeepDive;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.security.AuthenticatedUser;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@PageTitle("Generic Comments")
@Route(value = "Generic_Comments/:project_Id", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING"})
public class GenericCommentsView extends VerticalLayout implements BeforeEnterObserver {
    private final ProjectConnectionService projectConnectionService;
    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);
    private List<List<GenericComments>> listOfAllSheets = new ArrayList<>();
    private Crud<GenericComments> crudGenericComments;
    private Grid<GenericComments> gridGenericComments = new Grid<>(GenericComments.class, false);
    private String tableName;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    long contentLength = 0;
    String mimeType = "";
    Div textArea = new Div();
    private int projectId;
    private QS_Grid qsGrid;
    private Button uploadBtn;
    private Button qsBtn;
    private int id;

    public GenericCommentsView(AuthenticatedUser authenticatedUser, ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService) {

        this.projectConnectionService = projectConnectionService;

        uploadBtn = new Button("Upload");
        uploadBtn.setEnabled(false);

        qsBtn = new Button("QS and Start Job");
        qsBtn.setEnabled(false);

        Div htmlDiv = new Div();
        htmlDiv.getElement().setProperty("innerHTML", "<h2>Input Frontend for Generic Comments");
        add(htmlDiv);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String dbName = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.GENERIC_COMMENTS)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    dbName = projectParameter.getValue();
                } else if (Constants.DB_USER.equals(projectParameter.getName())) {
                    dbUser = projectParameter.getValue();
                } else if (Constants.DB_PASSWORD.equals(projectParameter.getName())) {
                    dbPassword = projectParameter.getValue();
                }else if (Constants.TABLE.equals(projectParameter.getName())) {
                    tableName = projectParameter.getValue();
                }
            }
        }
        dbUrl = "jdbc:sqlserver://" + dbServer + ";databaseName=" + dbName + ";encrypt=true;trustServerCertificate=true";
        Text databaseDetail = new Text("Connected to: "+ dbServer+ ", Database: " + dbName);

        //Componente QS-Grid:
        qsGrid = new QS_Grid(projectConnectionService);

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        // hl.add(singleFileUpload, saveButton, databaseDetail, progressBar);
        hl.add(singleFileUpload, uploadBtn, qsBtn, databaseDetail, qsGrid);
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

        add(textArea);
        setupUploader();
        add(getGenericCommentsGrid());
        setHeightFull();
    }
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        projectId = Integer.parseInt(parameters.get("project_Id").orElse(null));
    }

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
    private void save2db() {
        List<GenericComments> allGenericCommentsItems = getGenericCommentsDataProviderAllItems();

        List<String> allFileNames = allGenericCommentsItems.stream()
                .map(GenericComments::getFileName)
                .distinct()
                .collect(Collectors.toList());

        for (String fileName : allFileNames) {
            ProjectUpload projectUpload = new ProjectUpload();
            projectUpload.setFileName(fileName);
            projectUpload.setUserName(MainLayout.userName);
            projectConnectionService.saveUploadedGenericFileData(projectUpload);
        }

        Notification notification;
        String resultComments = projectConnectionService.saveGenericComments(allGenericCommentsItems, tableName, dbUrl, dbUser, dbPassword);
        if (resultComments.equals(Constants.OK)){
            notification = Notification.show(allGenericCommentsItems.size() + "X Comments Rows uploaded successfully",5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            notification = Notification.show("Error during Financials upload: " + resultComments ,5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

    }

    private Component getGenericCommentsGrid() {
        VerticalLayout content = new VerticalLayout();
        crudGenericComments = new Crud<>(GenericComments.class, createGenericCommentsEditor());
        setupGenericCommentsGrid();
        content.add(crudGenericComments);

        crudGenericComments.setToolbarVisible(false);

        gridGenericComments.addItemDoubleClickListener(event -> {
            GenericComments selectedEntity = event.getItem();
            crudGenericComments.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudGenericComments.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudGenericComments.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private CrudEditor<GenericComments> createGenericCommentsEditor() {

        TextArea comment = new TextArea("Comment");

        comment.setHeight("250px");
        comment.setWidth("1200px");
        FormLayout editForm = new FormLayout(comment);
        editForm.setColspan(comment, 2);

        editForm.setHeight("250px");
        editForm.setWidth("1200px");

        Binder<GenericComments> binder = new Binder<>(GenericComments.class);
        binder.forField(comment).asRequired().bind(GenericComments:: getComment, GenericComments::setComment);

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

            boolean isFileAlredyUploaded = isFileNameAvailable(fileName);
            if(!isFileAlredyUploaded) {
                parseExcelFile(fileData, fileName);
            } else {
                Notification.show("this file alredy uploaded " ,5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            List<GenericComments> listOfAllData = new ArrayList<>();

            for (List<GenericComments> sheetData : listOfAllSheets) {
                listOfAllData.addAll(sheetData);
            }
            GenericDataProvider dataGenericCommentsProvider = new GenericDataProvider(listOfAllData);
            gridGenericComments.setDataProvider(dataGenericCommentsProvider);
            setupDataProviderEvent();

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

            my_xls_workbook.forEach(sheet -> {
                String sheetName = sheet.getSheetName();
                if (sheetName != null) {
                    List<GenericComments> sheetData = parseSheet(fileName, sheet, GenericComments.class);
                    listOfAllSheets.add(sheetData);
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isFileNameAvailable(String fileName) {
        // Use stream to check if the fileName is present in any list within listOfAllSheets
        return listOfAllSheets.stream()
                .anyMatch(sheet -> sheet.stream()
                        .anyMatch(comment -> comment.getFileName().equals(fileName)));
    }
    private void setupGenericCommentsGrid() {

        String FILENAME = "fileName";
        String REGISTERNAME = "registerName";
        String LINENUMBER = "lineNumber";
        String RESPONSIBLE = "responsible";
        String TOPIC = "topic";
        String MONTH = "month";
        String CATEGORY_1 = "category1";
        String CATEGORY_2 = "category2";
        String SCENARIO = "scenario";
        String XTD = "xtd";
        String SEGMENT = "segment";
        String PAYMENTTYPE = "paymentType";
        String COMMENT = "comment";
        String ID = "id";
        String UPLOADID = "uploadId";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridGenericComments = crudGenericComments.getGrid();

        gridGenericComments.removeColumn(gridGenericComments.getColumnByKey(EDIT_COLUMN));
        gridGenericComments.removeColumn(gridGenericComments.getColumnByKey(ID));
        gridGenericComments.removeColumn(gridGenericComments.getColumnByKey(UPLOADID));

        gridGenericComments.getColumnByKey(FILENAME).setHeader("File_Name").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(REGISTERNAME).setHeader("Register_Name").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(LINENUMBER).setHeader("Line_Number").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(RESPONSIBLE).setHeader("Responsible").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(TOPIC).setHeader("Topic").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(MONTH).setHeader("Month").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(CATEGORY_1).setHeader("Category_1").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(CATEGORY_2).setHeader("Category_2").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(XTD).setHeader("XTD").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(SEGMENT).setHeader("Segment").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(PAYMENTTYPE).setHeader("Payment_Type").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridGenericComments.getColumnByKey(COMMENT).setHeader("Comment").setWidth("200px").setFlexGrow(0).setResizable(true);

        gridGenericComments.getColumns().forEach(col -> col.setAutoWidth(true));
        // Reorder the columns (alphabetical by default)
        gridGenericComments.setColumnOrder( gridGenericComments.getColumnByKey(FILENAME)
                , gridGenericComments.getColumnByKey(REGISTERNAME)
                , gridGenericComments.getColumnByKey(LINENUMBER)
                , gridGenericComments.getColumnByKey(RESPONSIBLE)
                , gridGenericComments.getColumnByKey(TOPIC)
                , gridGenericComments.getColumnByKey(MONTH)
                , gridGenericComments.getColumnByKey(CATEGORY_1)
                , gridGenericComments.getColumnByKey(CATEGORY_2)
                , gridGenericComments.getColumnByKey(SCENARIO)
                , gridGenericComments.getColumnByKey(XTD)
                , gridGenericComments.getColumnByKey(SEGMENT)
                , gridGenericComments.getColumnByKey(PAYMENTTYPE)
                , gridGenericComments.getColumnByKey(COMMENT));
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));
    }

    public <T> List<T>  parseSheet(String fileName, XSSFSheet my_worksheet, Class<T> targetType) {

        try {
            List<T> resultList = new ArrayList<>();
            Iterator<Row> rowIterator = my_worksheet.iterator();

            int rowNumber=0;
            Integer Error_count=0;
            System.out.println(my_worksheet.getPhysicalNumberOfRows()+"$$$$$$$$$");

            while (rowIterator.hasNext() ) {
                Row row = rowIterator.next();
                T entity = targetType.newInstance();
                rowNumber++;

                if (rowNumber == 1 || row.getCell(0) == null ) {
                  //  System.out.println(row.getCell(0).getCellType()+"..............................type");
                    continue;
                }
                System.out.println(row.getRowNum() +"............."+ my_worksheet.getSheetName());
                if(row.getCell(0) != null && row.getCell(0).toString().isEmpty()) {
                    break;
                }

                Field[] fields = targetType.getDeclaredFields();
                for (int index = 0; index < fields.length; index++) {
                    Cell cell = null;
                    if(index != 0) {
                        cell = row.getCell(index -1);
                    } else {
                        cell = row.getCell(index);
                    }

                    Field field = fields[index];
                    field.setAccessible(true);
                    if (cell != null && !cell.toString().isEmpty()) {
                        if (index == 0) {
                            field.set(entity, rowNumber);
                        } else {
                            if (field.getType() == int.class || field.getType() == Integer.class) {
                                if (cell.getCellType() == cell.CELL_TYPE_NUMERIC) {
                                    field.set(entity, (int) cell.getNumericCellValue());
                                } else if (cell.getCellType() == cell.CELL_TYPE_STRING) {
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
                    if (field.getName().equals("fileName")) {
                        field.set(entity, fileName);
                    } else if (field.getName().equals("registerName")) {
                        field.set(entity, my_worksheet.getSheetName());
                    } else if (field.getName().equals("id")) {
                        field.set(entity, id++);
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

    private void setupDataProviderEvent() {
        GenericDataProvider financialsdataProvider = new GenericDataProvider(getGenericCommentsDataProviderAllItems());

        crudGenericComments.addDeleteListener(
                deleteEvent -> {financialsdataProvider.delete(deleteEvent.getItem());
                    crudGenericComments.setDataProvider(financialsdataProvider);

                });
        crudGenericComments.addSaveListener(
                saveEvent -> {
                    financialsdataProvider.persist(saveEvent.getItem());
                    crudGenericComments.setDataProvider(financialsdataProvider);
                });
    }

    private List<GenericComments> getGenericCommentsDataProviderAllItems() {
        DataProvider<GenericComments, Void> existDataProvider = (DataProvider<GenericComments, Void>) gridGenericComments.getDataProvider();
        List<GenericComments> listOfGenericComments = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfGenericComments;
    }

}
