package com.htwpeeps.tori;

public class ResponseObject {
    public Integer fieldIndex;
    public Integer responseCode;

    public ResponseObject(Integer fieldIndex, Integer responseCode) {
        this.fieldIndex = fieldIndex;
        this.responseCode = responseCode;
    }
}
