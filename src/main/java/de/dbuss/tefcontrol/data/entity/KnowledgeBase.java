package de.dbuss.tefcontrol.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(schema = "dbo", name = "KnowledgeBase")
public class KnowledgeBase {
    @Id
    private Long id;

    @NotEmpty
    private String RichText;

    public String getRichText() {
        return RichText;
    }

    public void setRichText(String richText) {
        RichText = richText;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }



}
