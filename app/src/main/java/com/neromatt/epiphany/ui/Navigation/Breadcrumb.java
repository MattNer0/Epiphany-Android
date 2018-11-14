package com.neromatt.epiphany.ui.Navigation;

public class Breadcrumb {

    public String name;
    public String value;
    public boolean current;

    public Breadcrumb(String name, String value) {
        this.name = name;
        this.value = value;
        this.current = false;
    }

    public Breadcrumb(String name, String value, boolean current) {
        this.name = name;
        this.value = value;
        this.current = current;
    }

    @Override
    public String toString() {
        return "Breadcrumb["+name+": "+value+"]";
    }
}
