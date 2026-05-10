package ws.integration.endpoint;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

import ws.integration.dao.LineInterfaceDao;
import ws.integration.model.ErrorResponse;
import ws.integration.model.RequestLine;
import ws.integration.model.ResponseLine;

@Path("LineInterface")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LineInterfaceService {

    private static final String CORS_ORIGIN  = "*";
    private static final String CORS_METHODS = "POST, GET, PUT, UPDATE, OPTIONS";
    private static final String CORS_HEADERS = "Content-Type, Accept, X-Requested-With";

    @POST
    @Path("checkCustomerName")
    public Response checkCustomerName(RequestLine request) {
        try {
            ResponseLine data = LineInterfaceDao.checkCustomerName(request);
            return ok(data);
        } catch (Exception e) {
            return ok(new ErrorResponse(e.getMessage(), -1));
        }
    }

    @POST
    @Path("insertCustomerDetail")
    public Response insertCustomerDetail(RequestLine request) {
        try {
            ResponseLine data = LineInterfaceDao.insertCustomerDetail(request);
            return ok(data);
        } catch (Exception e) {
            return ok(new ErrorResponse(e.getMessage(), -1));
        }
    }

    @POST
    @Path("updateCustomerGovId")
    public Response updateCustomerGovId(RequestLine request) {
        try {
            ResponseLine data = LineInterfaceDao.updateCustomerGovId(request);
            return ok(data);
        } catch (Exception e) {
            return ok(new ErrorResponse(e.getMessage(), -1));
        }
    }

    @POST
    @Path("checkCustomerGovId")
    public Response checkCustomerGovId(RequestLine request) {
        try {
            ResponseLine data = LineInterfaceDao.checkCustomerGovId(request);
            return ok(data);
        } catch (Exception e) {
            return ok(new ErrorResponse(e.getMessage(), -1));
        }
    }

    private static Response ok(Object body) {
        return Response.ok(new Gson().toJson(body))
                .header("Access-Control-Allow-Origin",  CORS_ORIGIN)
                .header("Access-Control-Allow-Methods", CORS_METHODS)
                .header("Access-Control-Allow-Headers", CORS_HEADERS)
                .build();
    }
}
