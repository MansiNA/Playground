package de.dbuss.tefcontrol.data.entity;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Getter
@Setter
public class CltvAllProduct {
        private String allProducts;
        private String allProductGenNumber;
        private String allProductGen2;
        private Date verarb_datum;
}
