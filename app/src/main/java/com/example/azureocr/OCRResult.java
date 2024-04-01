package com.example.azureocr;

import com.google.gson.annotations.SerializedName;

public class OCRResult {
    public ReadResult readResult;
    @SerializedName("modelVersion")
    public String modelVersion;

    public ReadResult getReadResult() {
        return readResult;
    }

    public void setReadResult(ReadResult readResult) {
        this.readResult = readResult;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }
}
