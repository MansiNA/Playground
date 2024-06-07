package de.dbuss.tefcontrol.data.modules.sqlexecution.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Base64;

@Getter
@Setter
public class Configuration {

    private Long id;

    private String name;

    private String userName;

    private String password;

    private String db_Url;

    // Method to encode a password to Base64
    // Method to encode a password to URL-safe Base64
    public static String encodePassword(String plainTextPassword) {
        return Base64.getUrlEncoder().encodeToString(plainTextPassword.getBytes());
    }

    // Method to decode a password from URL-safe Base64
    public static String decodePassword(String encodedPassword) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedPassword);
        return new String(decodedBytes);
    }
}
