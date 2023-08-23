package de.dbuss.tefcontrol.data.entity;

public class Department {
    private int id;
    private String name;
    private String manager;
    private String url;
    private Department parent;

    public Department(int id, String name, Department parent, String manager, String url) {
        this.id = id;
        this.name = name;
        this.manager = manager;
        this.parent = parent;
        this.url=url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public Department getParent() {

        return parent;

    }

    public void setParent(Department parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return name;
    }
}
