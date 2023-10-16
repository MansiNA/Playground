package de.dbuss.tefcontrol.views.pfgproductmapping;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.shared.Registration;
import de.dbuss.tefcontrol.data.entity.CltvAllProduct;
import de.dbuss.tefcontrol.data.entity.ProductHierarchie;
import de.dbuss.tefcontrol.data.entity.ProjectConnection;

import java.util.List;
import java.util.stream.Collectors;

public class PFGProductForm extends FormLayout {
    ComboBox<String> pfg_Type = new ComboBox("PFG-Type");
    TextField node = new TextField("Node");
  //  Select<String> product_name = new Select<>();
      ComboBox<String> product_name = new ComboBox<>();


    private ProductHierarchie productHierarchie;

    Binder<ProductHierarchie> binder = new BeanValidationBinder<>(ProductHierarchie.class);
    public PFGProductForm(List<CltvAllProduct> cltvAllProducts) {
        addClassName("product-form");
        binder.bindInstanceFields(this);

        pfg_Type.getItemLabelGenerator();
        pfg_Type.setItems("PFG (PO)", "PFG (PP)");
        pfg_Type.setValue("PFG (PO)");
        pfg_Type.setAllowCustomValue(false);

        product_name.setLabel("Product");
        product_name.setWidthFull();

        product_name.setPageSize(5);

        //product_name.addThemeVariants(
             //   ComboBoxVariant.LUMO_SMALL,
            //    ComboBoxVariant.LUMO_ALIGN_RIGHT,
            //    ComboBoxVariant.LUMO_HELPER_ABOVE_FIELD
        //);
      //  product_name.getStyle().set("--vaadin-input-field-border-width", "1px");
        product_name.setAllowCustomValue(true);

        if (cltvAllProducts != null && !cltvAllProducts.isEmpty()) {
            List<String> productNames = cltvAllProducts.stream()
                    .map(CltvAllProduct::getAllProducts)
                    .collect(Collectors.toList());

            if(productNames.isEmpty() || productNames==null)
            {
                productNames.add("no values found");
            }

            product_name.setItems(productNames);
            product_name.setValue(productNames.get(0));


            //product_name.setHelperText("Helper text");
            product_name.setPlaceholder("Choose Product");
            product_name.setTooltipText("Choose Product from List");
            product_name.setClearButtonVisible(true);
            product_name.setPrefixComponent(VaadinIcon.SEARCH.create());

        }

        product_name.addValueChangeListener(event -> {
            if(event.getValue() != null) {
                product_name.setValue(event.getValue());
            }
        });


        add(product_name, pfg_Type,node, createButtonsLayout());
    }

    public void setProduct(ProductHierarchie productHierarchie){
        if (productHierarchie != null && productHierarchie.getId() != null) {
            pfg_Type.setEnabled(false);
            node.setEnabled(false);
        } else {
            pfg_Type.setEnabled(true);
            node.setEnabled(true);
        }
        binder.setBean(productHierarchie);
    }

    private Component createButtonsLayout() {

        Button save = new Button("Save");
     //   Button delete = new Button("Delete");
        Button close = new Button("Cancel");

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
     //   delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
    //    delete.addClickListener(event -> fireEvent(new DeleteEvent(this, binder.getBean())));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid())); // <4>
      //  return new HorizontalLayout(save, delete, close);
        return new HorizontalLayout(save, close);
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
        SaveEvent(PFGProductForm source, ProductHierarchie productHierarchie)
        {
            super(source, productHierarchie);
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
