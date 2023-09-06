package de.dbuss.tefcontrol.views.pfgproductmapping;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import de.dbuss.tefcontrol.data.entity.ProductHierarchie;

public class PFGProductForm extends FormLayout {
    ComboBox<String> pfg_Type = new ComboBox("PFG Type");

    TextField product_name = new TextField("Produkt");
    TextField node = new TextField("Knoten");

    TextField exportTime_id = new TextField("Export Zeitpunkt");

    Button save = new Button("Save");
    Button delete = new Button("Delete");
    Button close = new Button("Cancel");
    private ProductHierarchie productHierarchie;

    Binder<ProductHierarchie> binder = new BeanValidationBinder<>(ProductHierarchie.class);
    public PFGProductForm() {
        addClassName("product-form");
        binder.bindInstanceFields(this);

        pfg_Type.getItemLabelGenerator();
        pfg_Type.setItems("PFG Post", "PFG PRE");

        // Optional: Setze einen Standardwert
        pfg_Type.setValue("PFG Post");

        add(pfg_Type,node,product_name, exportTime_id, createButtonsLayout());
    }

    public void setProduct(ProductHierarchie productHierarchie){ binder.setBean(productHierarchie);}

    private Component createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, binder.getBean())));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid())); // <4>
        return new HorizontalLayout(save, delete, close);
    }

    private void validateAndSave() {
        if(binder.isValid()) {
            System.out.println("Save-Button gedrückt!");

            fireEvent(new SaveEvent(this, binder.getBean()));



        }
    }

    // Events
    public static abstract class ProductFormEvent extends ComponentEvent<PFGProductForm> {
        private ProductHierarchie productHierarchie;

        protected ProductFormEvent(PFGProductForm source, ProductHierarchie product) {
            super(source, false);
            this.productHierarchie = product;
        }

        public ProductHierarchie getProduct() {
            return productHierarchie;
        }
    }

    public static class SaveEvent extends ProductFormEvent {
        SaveEvent(PFGProductForm source, ProductHierarchie productHierarchie) {super(source, productHierarchie);
        }
    }

    public static class DeleteEvent extends ProductFormEvent {
        DeleteEvent(PFGProductForm source, ProductHierarchie productHierarchie) {super(source, productHierarchie);
        }

    }

    public static class CloseEvent extends ProductFormEvent {
        CloseEvent(PFGProductForm source) {
            super(source, null);
        }
    }

/*    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {

        return addListener(DeleteEvent.class, listener);
    }

    public void addSaveListener(ComponentEventListener<SaveEvent> listener) {
       // System.out.println("Save Event Listener wurde registriert");
        addListener(SaveEvent.class, listener);
    }
    public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
        return addListener(CloseEvent.class, listener);
    }*/


    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType, ComponentEventListener<T> listener){
        return getEventBus().addListener(eventType, listener);
    }
}
