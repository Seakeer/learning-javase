package me.seakeer.learning.javase.other.beans;

/**
 * MyBean;
 *
 * @author Seakeer;
 * @date 2024/12/25;
 */
public class MyBean {

    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "MyBean{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

}
