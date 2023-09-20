package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "dbo", name = "KnowledgeBase")
public class KnowledgeBase {
    @Id
    private Long id;

    @NotEmpty
    private String RichText;
}
