package ws.integration.model;

public class RequestLine {
    private HeaderData headerData;
    private RequestRecord requestRecord;

    public HeaderData getHeaderData() { return headerData; }
    public void setHeaderData(HeaderData headerData) { this.headerData = headerData; }

    public RequestRecord getRequestRecord() { return requestRecord; }
    public void setRequestRecord(RequestRecord requestRecord) { this.requestRecord = requestRecord; }
}
