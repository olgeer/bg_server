package com.ucsmy.itil.bg.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * Created by Max on 2017/1/3.
 */
public class Column {
    private String name;
    private String type;
    private boolean maynull;
    private int size;
    private String comment;
    private boolean key;
    private String alias;
    private boolean autofill;
    private String showname;
    private String describes;
    private String defaultvalue;
    private String autoformula;
    private String dictionaryname;
    private String format;
    private String formcontrol;
    private int ui_sort_order;
    private String ui_sort_length;
    private int in_use;
    private String tempValue;

    public String getTempValue() {
        return tempValue;
    }

    public void setTempValue(String tempValue) {
        this.tempValue = tempValue;
    }


    public int getUi_sort_order() {
        return ui_sort_order;
    }

    public void setUi_sort_order(int ui_sort_order) {
        this.ui_sort_order = ui_sort_order;
    }

    public void setUi_sort_length(String ui_sort_length) {
        this.ui_sort_length = ui_sort_length;
    }

    public int getIn_use() {
        return in_use;
    }

    public void setIn_use(int in_use) {
        this.in_use = in_use;
    }

    public boolean isAutofill() {
        return autofill;
    }

    public void setAutofill(boolean autofill) {
        this.autofill = autofill;
    }

    public String getDictionaryname() {
        return dictionaryname;
    }

    public void setDictionaryname(String dictionaryname) {
        this.dictionaryname = dictionaryname;
    }

    public String getShowname() {
        return showname;
    }

    public void setShowname(String showname) {
        this.showname = showname;
    }

    public String getDescribes() {
        return describes;
    }

    public void setDescribes(String describes) {
        this.describes = describes;
    }

    public String getDefaultvalue() {
        return defaultvalue;
    }

    public void setDefaultvalue(String defaultvalue) {
        this.defaultvalue = defaultvalue;
    }

    public String getAutoformula() {
        return autoformula;
    }

    public void setAutoformula(String autoformula) {
        this.autoformula = autoformula;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormcontrol() {
        return formcontrol;
    }

    public void setFormcontrol(String formcontrol) {
        this.formcontrol = formcontrol;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isKey() {
        return key;
    }

    public void setKey(boolean key) {
        this.key = key;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAliasName() {
        String retString = this.name;
        if (this.alias.length() > 0) {
            retString = this.alias + "." + retString;
        }
        return retString;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isMaynull() {
        return maynull;
    }

    public void setMaynull(boolean maynull) {
        this.maynull = maynull;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getUi_sort_length() {
        return ui_sort_length;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this, SerializerFeature.WriteMapNullValue);
    }
}