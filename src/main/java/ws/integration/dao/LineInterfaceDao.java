package ws.integration.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import ws.integration.model.HeaderData;
import ws.integration.model.RequestLine;
import ws.integration.model.ResponseLine;
import ws.integration.model.ResponseRecord;
import ws.integration.model.ResponseStatus;
import ws.integration.util.CallWSUtils;
import ws.integration.util.ConnectionDB;
import ws.integration.util.DatabaseUtils;

public class LineInterfaceDao {

    // ── helpers ──────────────────────────────────────────────────────────────

    private static HeaderData buildHeader(RequestLine req) {
        HeaderData h = new HeaderData();
        h.setMessageId(req.getHeaderData().getMessageId());
        h.setSentDateTime(req.getHeaderData().getSentDateTime());
        h.setResponseDateTime(req.getHeaderData().getResponseDateTime());
        return h;
    }

    private static ResponseStatus successStatus(String message) {
        ResponseStatus s = new ResponseStatus();
        s.setStatusCode("S");
        s.setErrorCode("200");
        s.setErrorMessage(message);
        return s;
    }

    private static ResponseStatus errorStatus(String message) {
        ResponseStatus s = new ResponseStatus();
        s.setStatusCode("E");
        s.setErrorCode("500");
        s.setErrorMessage(message);
        return s;
    }

    // ── Line-01: check customer by name + date of birth ──────────────────────

    public static ResponseLine checkCustomerName(RequestLine req) throws SQLException {
        ResponseLine result = new ResponseLine();
        result.setHeaderData(buildHeader(req));

        String firstName = req.getRequestRecord().getFirstName();
        String lastName  = req.getRequestRecord().getLastName();
        String dob       = req.getRequestRecord().getDob();

        if (isEmpty(firstName) || isEmpty(lastName) || isEmpty(dob)) {
            result.setResponseStatus(errorStatus("Parameter ที่ส่งมาไม่ถูกต้อง"));
            return result;
        }

        // Convert Buddhist-era date (dd/MM/yyyy BE) to ISO date (yyyy-MM-dd CE)
        String birthDay   = dob.substring(0, 2);
        String birthMonth = dob.substring(3, 5);
        int    birthYear  = Integer.parseInt(dob.substring(6, 10)) - 543;
        String birthDate  = birthYear + "-" + birthMonth + "-" + birthDay;

        String sql = "SELECT person.id, party.govt_id"
                   + " FROM person"
                   + " INNER JOIN party ON person.id = party.id"
                   + " WHERE person.fname = ? AND person.lname = ?"
                   + " AND person.birth_date BETWEEN ? AND ?";

        Connection conn = ConnectionDB.getDbCustomer();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, birthDate);
            pstmt.setString(4, birthDate);

            int partyId = 0;
            ResponseRecord record = new ResponseRecord();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    partyId = rs.getInt("id");
                    record.setPartyId(partyId);
                    record.setGovtId(rs.getString("govt_id"));
                }
            }
            result.setResponseRecord(record);

            if (partyId > 0) {
                result.setResponseStatus(successStatus("ดำเนินการเรียบร้อย"));
            } else {
                result.setResponseStatus(successStatus("ไม่พบข้อมูลที่ต้องการค้นหา"));
            }
        } catch (SQLException e) {
            result.setResponseStatus(errorStatus("เกิดข้อผิดพลาด: " + e.getMessage()));
        } finally {
            DatabaseUtils.close(conn);
        }
        return result;
    }

    // ── Line-02: insert LINE profile ─────────────────────────────────────────

    public static ResponseLine insertCustomerDetail(RequestLine req) throws SQLException {
        ResponseLine result = new ResponseLine();
        result.setHeaderData(buildHeader(req));

        String govtId      = req.getRequestRecord().getGovtId();
        String mobilePhone = req.getRequestRecord().getMobilePhone();
        String email       = req.getRequestRecord().getEmail();
        String lineId      = req.getRequestRecord().getLineId();

        if (isEmpty(govtId) || isEmpty(mobilePhone) || isEmpty(email) || isEmpty(lineId)) {
            result.setResponseStatus(errorStatus("Parameter ที่ส่งมาไม่ถูกต้อง"));
            return result;
        }

        Connection conn = ConnectionDB.getDbCustomer();
        try {
            conn.setAutoCommit(false);

            int partyId = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM party WHERE id = ?")) {
                ps.setString(1, govtId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) partyId = rs.getInt("id");
                }
            }

            if (partyId == 0) {
                result.setResponseStatus(errorStatus("ไม่สามารถบันทึกข้อมูลได้: ไม่พบ party"));
                return result;
            }

            // Insert into line_log only when no existing entry
            int logCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(id) AS cnt FROM line_log WHERE id = ?")) {
                ps.setInt(1, partyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) logCount = rs.getInt("cnt");
                }
            }

            if (logCount == 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO line_log (id, govt, fname, lname, phone_number, email, line_id, create_time)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, now())")) {
                    ps.setInt(1, partyId);
                    ps.setString(2, govtId);
                    ps.setString(3, req.getRequestRecord().getFnameTh());
                    ps.setString(4, req.getRequestRecord().getLnameTh());
                    ps.setString(5, mobilePhone);
                    ps.setString(6, email);
                    ps.setString(7, lineId);
                    ps.executeUpdate();
                }
                conn.commit();
            }

            // Determine next sequence for socialmedia
            int sequence = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(sequence) AS cnt FROM socialmedia WHERE id = ? AND socialmedia = 1")) {
                ps.setInt(1, partyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) sequence = rs.getInt("cnt");
                }
            }
            sequence = (sequence > 0) ? sequence + 1 : 1;

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO socialmedia"
                    + " (id, sequence, socialmedia, account, create_user_code, create_time,"
                    + "  update_user_code, last_update, system_id, system_key)"
                    + " VALUES (?, ?, 1, ?, '', now(), '', now(), 65, '')")) {
                ps.setInt(1, partyId);
                ps.setInt(2, sequence);
                ps.setString(3, lineId);
                ps.executeUpdate();
            }
            conn.commit();

            result.setResponseStatus(successStatus("ดำเนินการเรียบร้อย"));
        } catch (SQLException e) {
            rollbackQuietly(conn);
            result.setResponseStatus(errorStatus("เกิดข้อผิดพลาด: " + e.getMessage()));
        } finally {
            DatabaseUtils.close(conn);
        }
        return result;
    }

    // ── Line-03: update government ID ────────────────────────────────────────

    public static ResponseLine updateCustomerGovId(RequestLine req) throws SQLException {
        ResponseLine result = new ResponseLine();
        result.setHeaderData(buildHeader(req));

        String govtId = req.getRequestRecord().getGovtId();
        if (isEmpty(govtId)) {
            result.setResponseStatus(errorStatus("Parameter ที่ส่งมาไม่ถูกต้อง"));
            return result;
        }

        Connection conn = ConnectionDB.getDbCustomer();
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO line_update (id, fname, lname, policy_no, create_time)"
                    + " VALUES (?, ?, ?, ?, now())")) {
                ps.setString(1, govtId);
                ps.setString(2, req.getRequestRecord().getFnameTh());
                ps.setString(3, req.getRequestRecord().getLnameTh());
                ps.setString(4, req.getRequestRecord().getPolicyNo());
                int count = ps.executeUpdate();
                if (count > 0) {
                    conn.commit();
                    result.setResponseStatus(successStatus("ดำเนินการเรียบร้อย"));
                }
            }
        } catch (SQLException e) {
            rollbackQuietly(conn);
            if (e.getErrorCode() == 0) {
                result.setResponseStatus(errorStatus("ไม่สามารถบันทึกข้อมูลซ้ำได้"));
            } else {
                result.setResponseStatus(errorStatus("Error code = " + e.getErrorCode()));
            }
        } finally {
            DatabaseUtils.close(conn);
        }
        return result;
    }

    // ── Line-05: verify customer via CUS-20 or fallback DB ───────────────────

    public static ResponseLine checkCustomerGovId(RequestLine req) throws SQLException, IOException {
        ResponseLine result = new ResponseLine();
        result.setHeaderData(buildHeader(req));

        String citizenId = req.getRequestRecord().getCitizenId();
        if (isEmpty(citizenId)) {
            result.setResponseStatus(errorStatus("Parameter ที่ส่งมาไม่ถูกต้อง"));
            return result;
        }

        try {
            String wsInput    = new Gson().toJson(req);
            String cus20Data  = new CallWSUtils().callCus20(wsInput);
            JSONArray arrBody = new JSONObject(cus20Data).getJSONArray("responseRecord");

            ResponseRecord record = new ResponseRecord();
            if (arrBody.length() > 0) {
                record.setStatusCode("Y");
                result.setResponseRecord(record);
                result.setResponseStatus(successStatus("ดำเนินการเรียบร้อย"));
            } else {
                // CUS-20 returned nothing – fall back to local line_update table
                boolean found = false;
                Connection conn = ConnectionDB.getDbCustomer();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM line_update WHERE id = ?")) {
                    ps.setString(1, citizenId);
                    try (ResultSet rs = ps.executeQuery()) {
                        found = rs.next();
                    }
                } finally {
                    DatabaseUtils.close(conn);
                }
                record.setStatusCode(found ? "Y" : "N");
                result.setResponseRecord(record);
                result.setResponseStatus(successStatus("ดำเนินการเรียบร้อย"));
            }
        } catch (Exception e) {
            result.setResponseStatus(errorStatus("เกิดข้อผิดพลาด: " + e.getMessage()));
        }
        return result;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException ignored) { }
        }
    }
}
