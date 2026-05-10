package ws.integration.model;

public class ResponseLine {
    private HeaderData headerData;
    private ResponseRecord responseRecord;
    private ResponseStatus responseStatus;

    public HeaderData getHeaderData() { return headerData; }
    public void setHeaderData(HeaderData headerData) { this.headerData = headerData; }

    public ResponseRecord getResponseRecord() { return responseRecord; }
    public void setResponseRecord(ResponseRecord responseRecord) { this.responseRecord = responseRecord; }

    public ResponseStatus getResponseStatus() { return responseStatus; }
    public void setResponseStatus(ResponseStatus responseStatus) { this.responseStatus = responseStatus; }
}
