package de.dbuss.tefcontrol.views.knowledgeBase;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.Constants;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Knowledge Base")
@Route(value = "kb", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class KnowledgeBaseView  extends VerticalLayout {

    public KnowledgeBaseView() {

        super();
        Config config = new Config();
        config.setBalloonToolBar(Constants.Toolbar.values());
        config.setImage(new String[][]{},
                "", new String[]{"full", "alignLeft", "alignCenter", "alignRight"},
                new String[]{"imageTextAlternative", "|",
                        "imageStyle:alignLeft",
                        "imageStyle:full",
                        "imageStyle:alignCenter",
                        "imageStyle:alignRight"}, new String[]{});
        VaadinCKEditor editor = new VaadinCKEditorBuilder().with(builder -> {
            builder.editorData = "<p>This is a balloon editor example.</p>";
            builder.editorType = Constants.EditorType.BALLOON;
            builder.width = "70%";
            builder.config = config;
        }).createVaadinCKEditor();
        add(editor);

        add(new Label("--------------Preview---------------"));
//        Label label = new Label();
//        label.setWidth(editor.getWidth());
//        label.getElement().setProperty("innerHTML", editor.getValue());
//        editor.addValueChangeListener(e-> label.getElement().setProperty("innerHTML", e.getValue()));
//        add(label);
        VaadinCKEditor preview = new VaadinCKEditorBuilder().with(builder -> {
            builder.editorData = editor.getValue();
            builder.editorType = Constants.EditorType.BALLOON;
            builder.width = "70%";
            builder.config = new Config();
            config.setImage(new String[][]{}, "", new String[]{}, new String[]{}, new String[]{});
            builder.readOnly = true;
        }).createVaadinCKEditor();
        add(preview);
        editor.addValueChangeListener(e->preview.setValue(editor.getValue()));
        add(new Label("--------------Preview---------------"));

        setAlignItems(Alignment.CENTER);

    }


}



