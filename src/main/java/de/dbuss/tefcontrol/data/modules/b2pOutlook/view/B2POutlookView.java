package de.dbuss.tefcontrol.data.modules.b2pOutlook.view;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.crud.CrudVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.entity.User;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Financials;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.Subscriber;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.UnitsDeepDive;
import de.dbuss.tefcontrol.data.service.BackendService;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Route(value = "B2P_Outlook_Excel", layout = MainLayout.class)
@RolesAllowed({"MAPPING", "ADMIN"})
public class B2POutlookView extends VerticalLayout {

    private ProjectConnectionService projectConnectionService;
    private MemoryBuffer memoryBuffer = new MemoryBuffer();
    private Upload singleFileUpload = new Upload(memoryBuffer);
    private List<List<OutlookMGSR>> listOfAllSheets = new ArrayList<>();
    private Crud<OutlookMGSR> crudMGSR;
    private Grid<OutlookMGSR> gridMGSR = new Grid<>(OutlookMGSR.class, false);
    private Button saveButton;
    private String selectedDbName;
    private String tableName;
    private long contentLength = 0;
    private String mimeType = "";
    private Div textArea = new Div();

    ListenableFuture<String> future;

    BackendService backendService;

    public B2POutlookView (ProjectParameterService projectParameterService, ProjectConnectionService projectConnectionService, BackendService backendService) {

        this.backendService=backendService;
        this.projectConnectionService = projectConnectionService;
        saveButton = new Button(Constants.SAVE);
        saveButton.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;

        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.B2P_OUTLOOK)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    selectedDbName = projectParameter.getValue();
                } else if (Constants.TABLE.equals(projectParameter.getName())) {
                    tableName = projectParameter.getValue();
                }
            }
        }

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        hl.add(singleFileUpload,saveButton);
        add(hl);

        saveButton.addClickListener(clickEvent -> {

            Notification notification = Notification.show(" Rows Uploaded start",2000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            List<OutlookMGSR> listOfAllData = new ArrayList<>();

            for (List<OutlookMGSR> sheetData : listOfAllSheets) {
                listOfAllData.addAll(sheetData);
            }
            String resultFinancial = projectConnectionService.saveOutlookMGSR(listOfAllData, selectedDbName, tableName);
            if (resultFinancial.contains("ok")){
                notification = Notification.show(listOfAllData.size() + " B2P_Outlook Rows Uploaded successfully",5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                notification = Notification.show("Error during B2P_Outlook upload: " + resultFinancial ,5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }


        });

        setupUploader();
        add(getMGSRGrid());
        setSizeFull();
        setHeightFull();

        UI.getCurrent().addShortcutListener(
                () ->  start_thread(),
                Key.KEY_V, KeyModifier.ALT);

        UI.getCurrent().addShortcutListener(
                () ->  future.cancel(true),
                Key.KEY_S, KeyModifier.ALT);


    }

    private void start_thread() {

        Notification.show("starte Thread");

        UI ui = getUI().orElseThrow();

        future = backendService.longRunningTask();
        future.addCallback(
                successResult -> updateUi(ui, "Task finished: " + successResult),
                failureException -> updateUi(ui, "Task failed: " + failureException.getMessage())

        );



    }

    private void updateUi(UI ui, String result) {


        ui.access(() -> {
            Notification.show(result,6000, Notification.Position.MIDDLE);
        });
    }

    private Component getMGSRGrid() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setHeightFull();
        crudMGSR = new Crud<>(OutlookMGSR.class, createMGSREditor());
        crudMGSR.setToolbarVisible(false);
        crudMGSR.setHeightFull();
        crudMGSR.setSizeFull();
        setupMGSRGrid();
        content.add(crudMGSR);

        return content;
    }

    private CrudEditor<OutlookMGSR> createMGSREditor() {

        FormLayout editForm = new FormLayout();
        Binder<OutlookMGSR> binder = new Binder<>(OutlookMGSR.class);
        return new BinderCrudEditor<>(binder, editForm);
    }

    private void setupMGSRGrid() {

        String ZEILE = "zeile";
        String MONTH = "month";
        String PLLINE = "pl_Line";
        String PROFITCENTER = "profitCenter";
        String SCENARIO = "scenario";
        String BLOCK = "block";
        String SEGMENT = "segment";
        String PAYMENTTYPE = "paymentType";
        String TYPEOFDATA = "typeOfData";
        String VALUE = "value";
        String BLATT = "blatt";
        String LOADDATE = "loadDate";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridMGSR = crudMGSR.getGrid();

        gridMGSR.getColumnByKey(BLATT).setHeader("Blatt").setWidth("120px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("60px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(MONTH).setHeader("Month").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(PLLINE).setHeader("PL_Line").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(PROFITCENTER).setHeader("ProfitCenter").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(BLOCK).setHeader("Block").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(SEGMENT).setHeader("Segment").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(PAYMENTTYPE).setHeader("PaymentType").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(TYPEOFDATA).setHeader("TypeOfData").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridMGSR.getColumnByKey(VALUE).setHeader("Value").setWidth("80px").setFlexGrow(0).setResizable(true);

        gridMGSR.getColumnByKey(LOADDATE).setHeader("LoadDate").setWidth("80px").setFlexGrow(0).setResizable(true);

        gridMGSR.getColumns().forEach(col -> col.setAutoWidth(true));
        // gridMGSR.setHeightFull();
        // gridMGSR.setSizeFull();

        gridMGSR.removeColumn(gridMGSR.getColumnByKey(EDIT_COLUMN));
        gridMGSR.removeColumn(gridMGSR.getColumnByKey(LOADDATE));

        // Reorder the columns (alphabetical by default)
        gridMGSR.setColumnOrder(gridMGSR.getColumnByKey(BLATT)
                , gridMGSR.getColumnByKey(ZEILE)
                , gridMGSR.getColumnByKey(MONTH)
                , gridMGSR.getColumnByKey(PLLINE)
                , gridMGSR.getColumnByKey(PROFITCENTER)
                , gridMGSR.getColumnByKey(SCENARIO)
                , gridMGSR.getColumnByKey(BLOCK)
                , gridMGSR.getColumnByKey(SEGMENT)
                , gridMGSR.getColumnByKey(PAYMENTTYPE)
                , gridMGSR.getColumnByKey(TYPEOFDATA)
                , gridMGSR.getColumnByKey(VALUE)
                );
        //    , gridFinancials.getColumnByKey(EDIT_COLUMN));



        gridMGSR.addThemeVariants(GridVariant.LUMO_COMPACT);

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
            List<OutlookMGSR> listOfAllData = new ArrayList<>();

            for (List<OutlookMGSR> sheetData : listOfAllSheets) {
                listOfAllData.addAll(sheetData);
            }
            GenericDataProvider dataFinancialsProvider = new GenericDataProvider(listOfAllData, "Zeile");
            gridMGSR.setDataProvider(dataFinancialsProvider);
            singleFileUpload.clearFileList();
            saveButton.setEnabled(true);
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
                    List<OutlookMGSR> sheetData = parseSheet(sheet, OutlookMGSR.class);
                    listOfAllSheets.add(sheetData);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> List<T>  parseSheet(XSSFSheet sheet, Class<T> targetType) {
        List<T> resultList = new ArrayList<>();
        try {
            // List<T> resultList = new ArrayList<>();
            Iterator<Row> rowIterator = sheet.iterator();

            int rowNumber=0;
            Integer Error_count=0;
            System.out.println(sheet.getPhysicalNumberOfRows()+"$$$$$$$$$");

            while (rowIterator.hasNext() ) {
                Row row = rowIterator.next();
                T entity = targetType.newInstance();
                rowNumber++;

                if (rowNumber == 1) {
                    continue;
                }
                if (row.getCell(0) != null && row.getCell(0).toString().isEmpty()) {
                    break;
                }

                Field[] fields = targetType.getDeclaredFields();
                for (int index = 0; index < fields.length; index++) {
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
                                field.set(entity, (int) cell.getNumericCellValue());
                            } else if (field.getType() == long.class || field.getType() == Long.class) {
                                field.set(entity, (long) cell.getNumericCellValue());
                            } else if (field.getType() == double.class || field.getType() == Double.class) {
                                field.set(entity, cell.getNumericCellValue());
                            } else if (field.getType() == String.class) {
                                field.set(entity, cell.getStringCellValue());
                            }
                        }
                    }
                }

                Field zeilField = fields[0];// entity.getClass().getDeclaredField("zeile");
                zeilField.setAccessible(true);
                int zeilValue = zeilField.getInt(entity);

                if(zeilValue != 0 ) {
                    Field blattField = fields[fields.length - 2];
                    blattField.setAccessible(true);
                    blattField.set(entity, sheet.getSheetName());
                    resultList.add(entity);
                } else {
                    break;
                }
            }
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show(sheet.getSheetName() +" sheet having a parsing problem", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return resultList;
        }

    }
}
