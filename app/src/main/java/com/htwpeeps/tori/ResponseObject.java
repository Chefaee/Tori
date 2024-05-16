package com.htwpeeps.tori;

public class ReponseObject {
    private int fieldIndex;
    private int responseCode;

    public ReponseObject(int fieldIndex, int responseCode) {
        this.fieldIndex = fieldIndex;
        this.responseCode = responseCode;
    }

    public int getFieldIndex() {
        return fieldIndex;
    }

    public void setFieldIndex(int fieldIndex) {
        this.fieldIndex = fieldIndex;
    }
}
