package de.dbuss.tefcontrol.data.modules.sqlexecution.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SqlDefinition {

    private Long id;

    private Long pid;

    private String sql;

    private String beschreibung;

    private String name;

    private String accessRoles;

}
