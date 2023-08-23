package de.dbuss.tefcontrol.views.knowledgeBase;

import com.vaadin.flow.component.button.Button;
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
import de.dbuss.tefcontrol.data.entity.CLTV_HW_Measures;
import de.dbuss.tefcontrol.data.entity.KnowledgeBase;
import de.dbuss.tefcontrol.data.service.KnowledgeBaseService;
import de.dbuss.tefcontrol.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@PageTitle("Knowledge Base")
@Route(value = "kb", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class KnowledgeBaseView  extends VerticalLayout {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseView(KnowledgeBaseService knowledgeBaseService) {

        super();
        this.knowledgeBaseService = knowledgeBaseService;

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

            //builder.editorData = "<p>This is a balloon editor example.</p>";


            builder.editorType = Constants.EditorType.BALLOON;
            builder.width = "70%";
            builder.config = config;
        }).createVaadinCKEditor();
        add(editor);

        Long id = 1L;
        Optional<KnowledgeBase> kb = knowledgeBaseService.findById(id);

        editor.setValue(kb.get().getRichText());

        Button save = new Button("save content text");
        save.addClickListener((event -> {
          //  System.out.println("Speicher den Inhalt: "+ editor.getValue());

            KnowledgeBase myKB = new KnowledgeBase();
            myKB.setId(1L);
            myKB.setRichText(editor.getValue());

            knowledgeBaseService.update(myKB);

        }));
        add(save);



    }


}



