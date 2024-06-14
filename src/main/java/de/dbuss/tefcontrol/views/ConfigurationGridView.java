package de.dbuss.tefcontrol.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.dbuss.tefcontrol.data.Role;
import de.dbuss.tefcontrol.data.entity.User;
import de.dbuss.tefcontrol.data.modules.inputpbicomments.entity.GenericComments;
import de.dbuss.tefcontrol.data.service.ProjectConnectionService;
import de.dbuss.tefcontrol.data.service.UserService;
import de.dbuss.tefcontrol.dataprovider.GenericDataProvider;
import com.vaadin.flow.data.converter.Converter;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("User Configuration")
@Route(value = "userconfig", layout = MainLayout.class)
@RolesAllowed({"ADMIN"})
public class ConfigurationGridView extends VerticalLayout {

    private final ProjectConnectionService projectConnectionService;
    private final UserService userService;
    private Crud<User> userCrud;
    private Grid<User> userGrid;
    private Dialog dialog;
    private Dialog resetPasswordDialog;

    public ConfigurationGridView(ProjectConnectionService projectConnectionService, UserService userService) {

        this.projectConnectionService = projectConnectionService;
        this.userService = userService;

        add(new H2("User Configuration"));

        createNewUserDialog();

        Button newUserButton = new Button("New User");
        newUserButton.addClickListener(e -> {
            dialog.open();
        });

        add(getUserConfigurationGrid(), newUserButton);
        setupDataProviderEvent();
    }

    private Component createNewUserDialog() {
        VerticalLayout content = new VerticalLayout();
        dialog = new Dialog();
        TextField name = new TextField("Name");
        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        PasswordField confirmPassword = new PasswordField("Confirm password");
        MultiSelectComboBox<Role> rolesComboBox = new MultiSelectComboBox<>("Roles");
        rolesComboBox.setItems(Arrays.asList(Role.values()));
        rolesComboBox.setItemLabelGenerator(Enum::toString);
        Checkbox is_ad = new Checkbox("Is AD");

        FormLayout formLayout = new FormLayout();
        formLayout.add(
                name, username,
                password, confirmPassword,
                rolesComboBox, is_ad
        );
        formLayout.setResponsiveSteps(
                // Use one column by default
                new FormLayout.ResponsiveStep("0", 1),
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("500px", 2)
        );
        // Stretch the username field over 2 columns
      //  formLayout.setColspan(username, 2);

        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> {
            dialog.close();
        });
        Button addButton = new Button("add");
        addButton.addClickListener(e -> {

            String nameValue = name.getValue();
            String usernameValue = username.getValue();
            String passwordValue = password.getValue();
            String bCryptPassword = BCrypt.hashpw(passwordValue, BCrypt.gensalt());
            String confirmPasswordValue = confirmPassword.getValue();
            int is_adValue = is_ad.getValue() ? 1 : 0;
            Set<Role> roles = rolesComboBox.getSelectedItems();

            if (!validateInputs(nameValue, usernameValue, passwordValue, confirmPasswordValue, roles)) {
                Notification.show("Please enter all fields", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (!validatePassword(passwordValue, confirmPasswordValue)) {
                return;
            }
            // Use the values as needed, for example, create a new user
            User newUser = new User();
            newUser.setName(nameValue);
            newUser.setUsername(usernameValue);
            newUser.setHashedPassword(bCryptPassword);
            newUser.setIs_ad(is_adValue);
            newUser.setRoles(roles);

            try {
                projectConnectionService.getJdbcDefaultConnection();
                userService.update(newUser);
                updateUserGrid();
                Notification.show("Created new user", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error during upload", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.getMessage();
            }
            dialog.close();


        });

        HorizontalLayout hl = new HorizontalLayout(closeButton, addButton);
        hl.setAlignItems(Alignment.BASELINE);
        content.add(formLayout, hl);
        dialog.add(content);
        return dialog;
    }

    private boolean validateInputs(String name, String username, String password, String confirmPassword, Set<Role> roles) {
        return !name.isEmpty() && !username.isEmpty() && !password.isEmpty() && !confirmPassword.isEmpty() && !roles.isEmpty();
    }
    private boolean validatePassword(String password, String confirmPassword) {

        if (!password.equals(confirmPassword)) {
            Notification.show("Passwords do not match", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
        return true;
    }

    private Component getUserConfigurationGrid() {
        VerticalLayout content = new VerticalLayout();
        userCrud = new Crud<>(User.class, createUserEditor());
        setupUserGrid();
        content.add(userCrud);

       // userCrud.setToolbarVisible(false);

        userGrid.addItemDoubleClickListener(event -> {
            User selectedEntity = event.getItem();
            userCrud.edit(selectedEntity, Crud.EditMode.EXISTING_ITEM);
            userCrud.getDeleteButton().getElement().getStyle().set("display", "none");
        });

        updateUserGrid();
        userCrud.setHeightFull();
        content.setHeightFull();
        return content;
    }
    private void setupDataProviderEvent() {
        GenericDataProvider userdataProvider = new GenericDataProvider(getGenericCommentsDataProviderAllItems());

        userCrud.addDeleteListener(
                deleteEvent -> {
                    //userdataProvider.delete(deleteEvent.getItem());
                    userService.delete(deleteEvent.getItem().getId());
                    updateUserGrid();
                });
        userCrud.addSaveListener(
                saveEvent -> {
                 //   userdataProvider.persist(saveEvent.getItem());
                    System.out.println(saveEvent.getItem().getName() +"..........saveeeeee");
                    User user = saveEvent.getItem();
                    userService.update(user);
                    updateUserGrid();
                });
    }

    private List<User> getGenericCommentsDataProviderAllItems() {
        DataProvider<User, Void> existDataProvider = (DataProvider<User, Void>) userGrid.getDataProvider();
        List<User> listOfGenericComments = existDataProvider.fetch(new Query<>()).collect(Collectors.toList());
        return listOfGenericComments;
    }

    private void updateUserGrid() {
        List<User> listOfUser = userService.getAllUsers();
        GenericDataProvider dataProvider = new GenericDataProvider(listOfUser);
        userGrid.setDataProvider(dataProvider);
    }

    private void setupUserGrid() {

        String ID = "id";
        String VERSION = "version";
        String USERNAME = "username";
        String NAME = "name";
        String PASSWORD = "hashedPassword";
        String ROLES = "roles";
        String PROFILEPICTURE = "profilePicture";
        String IS_AD = "is_ad";
        String RESET_PASSWORD = "resetPassword";

        String EDIT_COLUMN = "vaadin-crud-edit-column";

        userGrid = userCrud.getGrid();
     //   userGrid.setColumns(USERNAME, NAME, ROLES, IS_AD);

        userGrid.getColumnByKey(ID).setHeader("Id").setWidth("80px").setFlexGrow(0).setResizable(true);
        userGrid.getColumnByKey(VERSION).setHeader("Version").setWidth("200px").setFlexGrow(0).setResizable(true);
        userGrid.getColumnByKey(USERNAME).setHeader("Username").setWidth("80px").setFlexGrow(0).setResizable(true);
        userGrid.getColumnByKey(NAME).setHeader("Name").setWidth("200px").setFlexGrow(0).setResizable(true);
        userGrid.getColumnByKey(PASSWORD).setHeader("Password").setWidth("150px").setFlexGrow(0).setResizable(true);
        userGrid.getColumnByKey(ROLES).setHeader("roles").setWidth("80px").setFlexGrow(0).setResizable(true);
        userGrid.getColumnByKey(PROFILEPICTURE).setHeader("profilePicture").setWidth("80px").setFlexGrow(0).setResizable(true);
        userGrid.getColumnByKey(IS_AD).setHeader("is_ad").setWidth("80px").setFlexGrow(0).setResizable(true);

        userGrid.addComponentColumn(user -> {
            Button resetBtn = new Button("Reset Password");
            resetBtn.addClickListener(event -> {
                resetPasswordDialog(user);
                resetPasswordDialog.open();
            });
            return resetBtn;
        }).setKey(RESET_PASSWORD).setFlexGrow(0).setWidth("140px");

            userGrid.removeColumn(userGrid.getColumnByKey(ID));
            userGrid.removeColumn(userGrid.getColumnByKey(VERSION));
            userGrid.removeColumn(userGrid.getColumnByKey(PASSWORD));
            userGrid.removeColumn(userGrid.getColumnByKey(PROFILEPICTURE));
            userGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        // Reorder the columns (alphabetical by default)
        userGrid.setColumnOrder( userGrid.getColumnByKey(USERNAME)
                , userGrid.getColumnByKey(NAME)
                //   , userGrid.getColumnByKey(PASSWORD)
                , userGrid.getColumnByKey(ROLES)
                //    , userGrid.getColumnByKey(PROFILEPICTURE)
                , userGrid.getColumnByKey(IS_AD)
                , userGrid.getColumnByKey(EDIT_COLUMN)
        ,userGrid.getColumnByKey(RESET_PASSWORD));

        userCrud.setToolbarVisible(false);
    }

    private Dialog resetPasswordDialog(User user) {
        VerticalLayout content = new VerticalLayout();
        resetPasswordDialog = new Dialog();
        PasswordField password = new PasswordField("Password");
        PasswordField confirmPassword = new PasswordField("Confirm password");

        FormLayout formLayout = new FormLayout();
        formLayout.add(password, confirmPassword);
        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> {
            resetPasswordDialog.close();
        });
        Button addButton = new Button("add");
        addButton.addClickListener(e -> {
            String passwordValue = password.getValue();
            String confirmPasswordValue = confirmPassword.getValue();
            String bCryptPassword = BCrypt.hashpw(passwordValue, BCrypt.gensalt());
            if (validatePassword(passwordValue, confirmPasswordValue)) {
                user.setHashedPassword(bCryptPassword);
                try {
                    userService.update(user);
                    Notification.show("Password reset successfuly", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    resetPasswordDialog.close();
                } catch (Exception ex) {
                    ex.getMessage();
                    Notification.show("Error: Password is not reset", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

            }
        });

        HorizontalLayout hl = new HorizontalLayout(closeButton, addButton);
        hl.setAlignItems(Alignment.BASELINE);
        content.add(formLayout, hl);
        resetPasswordDialog.add(content);

        return resetPasswordDialog;
    }

    private CrudEditor<User> createUserEditor() {

        TextField username = new TextField("Username");
        TextField name = new TextField("Name");
        PasswordField hashedPassword = new PasswordField("Password");
        hashedPassword.setValue("");

        MultiSelectComboBox<Role> rolesComboBox = new MultiSelectComboBox<>("Roles");
        rolesComboBox.setItems(Arrays.asList(Role.values()));
        rolesComboBox.setItemLabelGenerator(Enum::toString);
        Checkbox is_ad = new Checkbox("Is AD");

        // Create a FormLayout to arrange the form fields
        FormLayout editForm = new FormLayout(
                username, name, rolesComboBox, is_ad
        );

        // Set the size of form fields as needed
        editForm.setWidth("500px");

        // Create a Binder to bind form fields with User entity
        Binder<User> binder = new Binder<>(User.class);

        // Bind each form field with its corresponding attribute in the User entity
        binder.forField(username).bind(User::getUsername, User::setUsername);
        binder.forField(name).bind(User::getName, User::setName);
        binder.forField(rolesComboBox)
                .bind(User::getRoles, User::setRoles);
        binder.forField(is_ad)
                .withConverter(new IntegerToBooleanConverter())
                .bind(User::getIs_ad, User::setIs_ad);
//        binder.forField(profilePicture)
//                .withConverter(new ByteArrayToUrlConverter())
//                .bind(User::getProfilePicture, User::setProfilePicture);

        return new BinderCrudEditor<>(binder, editForm);
    }

    public class BCryptConverter implements Converter<String, String> {

        @Override
        public Result<String> convertToModel(String presentation, ValueContext context) {
            // You want to store the new BCrypt hashed password in the database
            String hashedPassword = BCrypt.hashpw(presentation, BCrypt.gensalt());
         //   hashedPassword = "abc";
            return Result.ok(hashedPassword);
        }

        @Override
        public String convertToPresentation(String model, ValueContext context) {
            // Display the plain text password in the UI
         //   model = "xyz";
            return model;
        }
    }

    public class IntegerToBooleanConverter implements Converter<Boolean, Integer> {

        @Override
        public Result<Integer> convertToModel(Boolean value, ValueContext context) {
            return Result.ok(value != null && value ? 1 : 0);
        }

        @Override
        public Boolean convertToPresentation(Integer value, ValueContext context) {
            return value != null && value != 0;
        }
    }
    public class ByteArrayToUrlConverter implements Converter<String, byte[]> {
        @Override
        public Result<byte[]> convertToModel(String value, ValueContext context) {
            // You might want to implement this based on your requirements
            return Result.ok(new byte[0]);
        }

        @Override
        public String convertToPresentation(byte[] value, ValueContext context) {
            // You might want to implement this based on your requirements
            return ""; // Return the URL or representation of the byte array
        }
    }

}