package de.dbuss.tefcontrol.data.modules.inputpbicomments.view;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import de.dbuss.tefcontrol.data.entity.Constants;
import de.dbuss.tefcontrol.data.entity.ProjectParameter;
import de.dbuss.tefcontrol.data.modules.b2pOutlook.entity.OutlookMGSR;
import de.dbuss.tefcontrol.data.modules.cltv_Inflow.entity.CLTVInflow;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.*;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.ProjectParameterService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.format.CellFormatType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Route(value = "TechComments", layout = MainLayout.class)
@RolesAllowed({"ADMIN", "MAPPING"})
public class TechCommentView extends VerticalLayout {
    private final ProjectConnectionService projectConnectionService;
    private MemoryBuffer memoryBuffer = new MemoryBuffer();
    private Upload singleFileUpload = new Upload(memoryBuffer);

    private List<XPexComment> listOfXPexComment = new ArrayList<XPexComment>();
    private List<ITOnlyComment> listOfITOnlyComment = new ArrayList<ITOnlyComment>();
    private List<KPIsComment> listOfKPIsComment = new ArrayList<KPIsComment>();
    private Crud<XPexComment> crudXPexComment;
    private Grid<XPexComment> gridXPexComment = new Grid<>(XPexComment.class);
    private Crud<ITOnlyComment> crudITOnlyComment;
    private Grid<ITOnlyComment> gridITOnlyComment = new Grid<>(ITOnlyComment.class);
    private Crud<KPIsComment> crudKPIsComment;
    private Grid<KPIsComment> gridKPIsComment = new Grid<>(KPIsComment.class);
    private String selectedDbName;
    private String tableName;
    private long contentLength = 0;
    private String mimeType = "";
    private Div textArea = new Div();
    private Button saveButton;
    private String idKey = Constants.ZEILE;

    public TechCommentView(ProjectConnectionService projectConnectionService, ProjectParameterService projectParameterService) {

        this.projectConnectionService = projectConnectionService;

        saveButton = new Button(Constants.SAVE);
        saveButton.setEnabled(false);

        List<ProjectParameter> listOfProjectParameters = projectParameterService.findAll();
        String dbServer = null;
        String xPexTableName = null;
        String iTOnlyTableName = null;
        String kPIsTableName = null;


        for (ProjectParameter projectParameter : listOfProjectParameters) {
            if(projectParameter.getNamespace().equals(Constants.PBI_TECH_COMMENTS)) {
                if (Constants.DB_SERVER.equals(projectParameter.getName())) {
                    dbServer = projectParameter.getValue();
                } else if (Constants.DB_NAME.equals(projectParameter.getName())) {
                    selectedDbName = projectParameter.getValue();
                } else if (Constants.TABLE_xPEX.equals(projectParameter.getName())) {
                    xPexTableName = projectParameter.getValue();
                } else if (Constants.TABLE_ITONLY.equals(projectParameter.getName())) {
                    iTOnlyTableName = projectParameter.getValue();
                } else if (Constants.TABLE_KPIS.equals(projectParameter.getName())) {
                    kPIsTableName = projectParameter.getValue();
                }
            }
        }

        HorizontalLayout hl = new HorizontalLayout();
        hl.setAlignItems(Alignment.BASELINE);
        hl.add(singleFileUpload,saveButton);
        add(hl);

        String finalXPexTableName = xPexTableName;
        String finalITOnlyTableName = iTOnlyTableName;
        String finalKPIsTableName = kPIsTableName;

        saveButton.addClickListener(clickEvent -> {

            Notification notification = Notification.show(" Rows Uploaded start",2000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            String resultFinancial = projectConnectionService.saveXPexComments(listOfXPexComment, selectedDbName, finalXPexTableName);
            if (resultFinancial.contains("ok")){
                notification = Notification.show(listOfXPexComment.size() + " Financials Rows Uploaded successfully",5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                notification = Notification.show("Error during Financials upload: " + resultFinancial ,5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }

            String resultSubscriber = projectConnectionService.saveITOnlyComments(listOfITOnlyComment, selectedDbName, finalITOnlyTableName);
            if (resultSubscriber.contains("ok")){
                notification = Notification.show(listOfITOnlyComment.size() + " Subscriber Rows Uploaded successfully",5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                notification = Notification.show("Error during Subscriber upload: " + resultSubscriber,5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }

            String resultUnits = projectConnectionService.saveKPIsComments(listOfKPIsComment, selectedDbName, finalKPIsTableName);
            if (resultUnits.contains("ok")){
                notification = Notification.show(listOfKPIsComment.size() + " UnitsDeepDive Rows Uploaded successfully",5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                notification = Notification.show("Error during UnitsDeepDive upload: " + resultUnits,5000, Notification.Position.MIDDLE);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }

        });

        setupUploader();
        add(getTabsheet());
        setHeightFull();
    }

    private TabSheet getTabsheet() {

        TabSheet tabSheet = new TabSheet();
        tabSheet.add(Constants.XPEX,getXPexComment());
        tabSheet.add(Constants.ITONLY, getITOnlyComment());
        tabSheet.add(Constants.KPIS, getKPIsComment());

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();
        tabSheet.addThemeVariants(TabSheetVariant.MATERIAL_BORDERED);
        return tabSheet;
    }

    private Component getKPIsComment() {
        VerticalLayout content = new VerticalLayout();
        crudKPIsComment = new Crud<>(KPIsComment.class, createKPIsCommentEditor());
        setupKPIsCommentGrid();
        content.add(crudKPIsComment);

        crudKPIsComment.setToolbarVisible(false);

        gridKPIsComment.addItemDoubleClickListener(event -> {
            KPIsComment selectedEntity = event.getItem();
            crudKPIsComment.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudKPIsComment.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudKPIsComment.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private void setupKPIsCommentGrid() {

        String ZEILE = "zeile";

        String DATE = "date";

        String TOPIC = "topic";

        String CATEGORY1 = "category1";

        String CATEGORY2 = "category2";
        String COMMENT = "comment";
        String PLANSCENARIO = "planScenario";
        String LOADDATE = "loadDate";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridKPIsComment = crudKPIsComment.getGrid();

        gridKPIsComment.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridKPIsComment.getColumnByKey(DATE).setHeader("Date").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridKPIsComment.getColumnByKey(TOPIC).setHeader("Topic").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridKPIsComment.getColumnByKey(COMMENT).setHeader("Comment").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridKPIsComment.getColumnByKey(CATEGORY1).setHeader("Category_1").setWidth("150px").setFlexGrow(0).setResizable(true);
        gridKPIsComment.getColumnByKey(CATEGORY2).setHeader("Category_2").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridKPIsComment.getColumnByKey(PLANSCENARIO).setHeader("PlanScenario").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridKPIsComment.getColumnByKey(LOADDATE).setHeader("LoadDate").setWidth("80px").setFlexGrow(0).setResizable(true);

        gridKPIsComment.removeColumn(gridKPIsComment.getColumnByKey(EDIT_COLUMN));
        gridKPIsComment.removeColumn(gridKPIsComment.getColumnByKey(LOADDATE));
        gridKPIsComment.getColumns().forEach(col -> col.setAutoWidth(true));
        // Reorder the columns (alphabetical by default)
        gridKPIsComment.setColumnOrder( gridKPIsComment.getColumnByKey(ZEILE)
                , gridKPIsComment.getColumnByKey(DATE)
                , gridKPIsComment.getColumnByKey(TOPIC)
                , gridKPIsComment.getColumnByKey(COMMENT)
                , gridKPIsComment.getColumnByKey(CATEGORY1)
                , gridKPIsComment.getColumnByKey(CATEGORY2)
                , gridKPIsComment.getColumnByKey(PLANSCENARIO));
        //        , gridKPIsComment.getColumnByKey(LOADDATE));
        //    , gridUnitsDeepDive.getColumnByKey(EDIT_COLUMN));

    }

    private CrudEditor<KPIsComment> createKPIsCommentEditor() {
        FormLayout editForm = new FormLayout();
        Binder<KPIsComment> binder = new Binder<>(KPIsComment.class);
        return new BinderCrudEditor<>(binder, editForm);
    }

    private Component getITOnlyComment() {
        VerticalLayout content = new VerticalLayout();
        crudITOnlyComment = new Crud<>(ITOnlyComment.class, createITOnlyCommentEditor());
        setupITOnlyCommentGrid();
        content.add(crudITOnlyComment);

        crudITOnlyComment.setToolbarVisible(false);

        gridITOnlyComment.addItemDoubleClickListener(event -> {
            ITOnlyComment selectedEntity = event.getItem();
            crudITOnlyComment.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudITOnlyComment.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudITOnlyComment.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private void setupITOnlyCommentGrid() {

        String ZEILE = "zeile";

        String DATE = "date";

        String TOPIC = "topic";

        String CATEGORY1 = "category1";

        String CATEGORY2 = "category2";
        String COMMENT = "comment";
        String SCENARIO = "scenario";
        String XTD = "xtd";
        String LOADDATE = "loadDate";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridITOnlyComment = crudITOnlyComment.getGrid();

        gridITOnlyComment.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridITOnlyComment.getColumnByKey(DATE).setHeader("Date").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridITOnlyComment.getColumnByKey(TOPIC).setHeader("Topic").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridITOnlyComment.getColumnByKey(COMMENT).setHeader("Comment").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridITOnlyComment.getColumnByKey(CATEGORY1).setHeader("Category_1").setWidth("150px").setFlexGrow(0).setResizable(true);
        gridITOnlyComment.getColumnByKey(CATEGORY2).setHeader("Category_2").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridITOnlyComment.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridITOnlyComment.getColumnByKey(XTD).setHeader("XTD").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridITOnlyComment.getColumnByKey(LOADDATE).setHeader("LoadDate").setWidth("80px").setFlexGrow(0).setResizable(true);

        gridITOnlyComment.removeColumn(gridITOnlyComment.getColumnByKey(EDIT_COLUMN));
        gridITOnlyComment.removeColumn(gridITOnlyComment.getColumnByKey(LOADDATE));
        gridITOnlyComment.getColumns().forEach(col -> col.setAutoWidth(true));
        // Reorder the columns (alphabetical by default)
        gridITOnlyComment.setColumnOrder( gridITOnlyComment.getColumnByKey(ZEILE)
                , gridITOnlyComment.getColumnByKey(DATE)
                , gridITOnlyComment.getColumnByKey(TOPIC)
                , gridITOnlyComment.getColumnByKey(COMMENT)
                , gridITOnlyComment.getColumnByKey(CATEGORY1)
                , gridITOnlyComment.getColumnByKey(CATEGORY2)
                , gridITOnlyComment.getColumnByKey(SCENARIO)
                , gridITOnlyComment.getColumnByKey(XTD));

        //        , gridITOnlyComment.getColumnByKey(LOADDATE));
        //    , gridUnitsDeepDive.getColumnByKey(EDIT_COLUMN));

    }

    private CrudEditor<ITOnlyComment> createITOnlyCommentEditor() {
        FormLayout editForm = new FormLayout();
        Binder<ITOnlyComment> binder = new Binder<>(ITOnlyComment.class);
        return new BinderCrudEditor<>(binder, editForm);
    }

    private Component getXPexComment() {
        VerticalLayout content = new VerticalLayout();
        crudXPexComment = new Crud<>(XPexComment.class, createXPexCommentEditor());
        setupXPexCommentGrid();
        content.add(crudXPexComment);

        crudXPexComment.setToolbarVisible(false);

        gridXPexComment.addItemDoubleClickListener(event -> {
            XPexComment selectedEntity = event.getItem();
            crudXPexComment.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            crudXPexComment.getDeleteButton().getElement().getStyle().set("display", "none");
        });
        crudXPexComment.setHeightFull();
        content.setHeightFull();
        return content;
    }

    private void setupXPexCommentGrid() {

        String ZEILE = "zeile";

        String DATE = "date";

        String TOPIC = "topic";

        String CATEGORY1 = "category1";

        String CATEGORY2 = "category2";
        String COMMENT = "comment";
        String SCENARIO = "scenario";
        String XTD = "xtd";
        String LOADDATE = "loadDate";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        gridXPexComment = crudXPexComment.getGrid();

        gridXPexComment.getColumnByKey(ZEILE).setHeader("Zeile").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridXPexComment.getColumnByKey(DATE).setHeader("Date").setWidth("100px").setFlexGrow(0).setResizable(true);
        gridXPexComment.getColumnByKey(TOPIC).setHeader("Topic").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridXPexComment.getColumnByKey(COMMENT).setHeader("Comment").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridXPexComment.getColumnByKey(CATEGORY1).setHeader("Category_1").setWidth("150px").setFlexGrow(0).setResizable(true);
        gridXPexComment.getColumnByKey(CATEGORY2).setHeader("Category_2").setWidth("200px").setFlexGrow(0).setResizable(true);
        gridXPexComment.getColumnByKey(SCENARIO).setHeader("Scenario").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridXPexComment.getColumnByKey(XTD).setHeader("XTD").setWidth("80px").setFlexGrow(0).setResizable(true);
        gridXPexComment.getColumnByKey(LOADDATE).setHeader("LoadDate").setWidth("80px").setFlexGrow(0).setResizable(true);

        gridXPexComment.removeColumn(gridXPexComment.getColumnByKey(EDIT_COLUMN));
        gridXPexComment.removeColumn(gridXPexComment.getColumnByKey(LOADDATE));
        gridXPexComment.getColumns().forEach(col -> col.setAutoWidth(true));
        // Reorder the columns (alphabetical by default)
        gridXPexComment.setColumnOrder( gridXPexComment.getColumnByKey(ZEILE)
                , gridXPexComment.getColumnByKey(DATE)
                , gridXPexComment.getColumnByKey(TOPIC)
                , gridXPexComment.getColumnByKey(COMMENT)
                , gridXPexComment.getColumnByKey(CATEGORY1)
                , gridXPexComment.getColumnByKey(CATEGORY2)
                , gridXPexComment.getColumnByKey(SCENARIO)
                , gridXPexComment.getColumnByKey(XTD));

        //        , gridXPexComment.getColumnByKey(LOADDATE));
        //    , gridUnitsDeepDive.getColumnByKey(EDIT_COLUMN));

    }

    private CrudEditor<XPexComment> createXPexCommentEditor() {
        FormLayout editForm = new FormLayout();
        Binder<XPexComment> binder = new Binder<>(XPexComment.class);
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

            GenericDataProvider dataFinancialsProvider = new GenericDataProvider(listOfXPexComment, idKey);
            gridXPexComment.setDataProvider(dataFinancialsProvider);
            GenericDataProvider dataSubscriberProvider = new GenericDataProvider(listOfITOnlyComment, idKey);
            gridITOnlyComment.setDataProvider(dataSubscriberProvider);
            GenericDataProvider dataUnitsDeepDiveProvider = new GenericDataProvider(listOfKPIsComment, idKey);
            gridKPIsComment.setDataProvider(dataUnitsDeepDiveProvider);

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

            XSSFSheet sheet1 = my_xls_workbook.getSheet(Constants.XPEX);
            listOfXPexComment = parseSheet(sheet1, XPexComment.class);

            XSSFSheet sheet2 = my_xls_workbook.getSheet(Constants.ITONLY);
            listOfITOnlyComment = parseSheet(sheet2, ITOnlyComment.class);

            XSSFSheet sheet3 = my_xls_workbook.getSheet(Constants.KPIS);
            listOfKPIsComment = parseSheet(sheet3, KPIsComment.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public <T> List<T>  parseSheet(XSSFSheet my_worksheet, Class<T> targetType) {

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
}