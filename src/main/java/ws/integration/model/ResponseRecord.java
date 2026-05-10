package ws.integration.model;

public class ResponseRecord {
    private int partyId;
    private String govtId;
    private String statusCode;

    public int getPartyId() { return partyId; }
    public void setPartyId(int partyId) { this.partyId = partyId; }

    public String getGovtId() { return govtId; }
    public void setGovtId(String govtId) { this.govtId = govtId; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
}
