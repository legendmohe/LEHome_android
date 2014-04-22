package de.greenrobot.lehome;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table SHORTCUT.
 */
public class Shortcut {

    private Long id;
    private String content;
    private Integer invoke_count;
    private Double weight;

    public Shortcut() {
    }

    public Shortcut(Long id) {
        this.id = id;
    }

    public Shortcut(Long id, String content, Integer invoke_count, Double weight) {
        this.id = id;
        this.content = content;
        this.invoke_count = invoke_count;
        this.weight = weight;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getInvoke_count() {
        return invoke_count;
    }

    public void setInvoke_count(Integer invoke_count) {
        this.invoke_count = invoke_count;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

}
