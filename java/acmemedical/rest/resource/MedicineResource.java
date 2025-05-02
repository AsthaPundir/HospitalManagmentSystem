

package acmemedical.rest.resource;
import static acmemedical.utility.MyConstants.ADMIN_ROLE;
import static acmemedical.utility.MyConstants.PHYSICIAN_PATIENT_MEDICINE_RESOURCE_PATH;
import static acmemedical.utility.MyConstants.PHYSICIAN_RESOURCE_NAME;
import static acmemedical.utility.MyConstants.RESOURCE_PATH_ID_ELEMENT;
import static acmemedical.utility.MyConstants.RESOURCE_PATH_ID_PATH;
import static acmemedical.utility.MyConstants.USER_ROLE;


import static acmemedical.utility.MyConstants.*;


import java.util.List;

import acmemedical.ejb.ACMEMedicalService;
import acmemedical.entity.MedicalSchool;
import acmemedical.entity.Patient;
import acmemedical.entity.SecurityUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;

import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import acmemedical.entity.Physician;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.soteria.WrappingCallerPrincipal;

import acmemedical.ejb.ACMEMedicalService;
import acmemedical.entity.Medicine;



@Path(MEDICINE_SUBRESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MedicineResource {
	private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMEMedicalService service;

    @Inject
    protected SecurityContext sc;

    @GET
    @RolesAllowed({ADMIN_ROLE})
    public Response getMedicines() {
        LOG.debug("Retrieving medicine...");
        List<Medicine> medicines = service.getAllMedicines();
        Response response = Response.ok(medicines).build();
        return response;
    }

//    @GET
//    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
//    @Path(RESOURCE_PATH_ID_PATH)
//    public Response getMedicineById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
//        LOG.debug("Retrieving specific medicine with id = {}", id);
//        Medicine medicine = service.getMedicineById(id);
//        if (medicine == null)
//            return Response.status(Status.NOT_FOUND).build();
//        return Response.ok(medicine).build();
//    }
    @GET
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response getMedicineById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("Attempting to retrieve medicine with ID: {}", id);

        if (sc.isCallerInRole(ADMIN_ROLE)) {
            Medicine medicine = service.getMedicineById(id);
            return Response.status(medicine == null ? Status.NOT_FOUND : Status.OK).entity(medicine).build();
        } else if (sc.isCallerInRole(USER_ROLE)) {
            LOG.debug("User role is authorized but restricted to certain data. Accessing medicine with ID: {}", id);
            // If there are no further restrictions for USER_ROLE, treat it the same as ADMIN_ROLE.
            Medicine medicine = service.getMedicineById(id);
            return Response.status(medicine == null ? Status.NOT_FOUND : Status.OK).entity(medicine).build();
        }

        // Fallback for unhandled cases
        return Response.status(Status.BAD_REQUEST).build();
    }





    @POST
    @RolesAllowed({ADMIN_ROLE})
    public Response addMedicine(Medicine newMedicine) {
        LOG.debug("Adding a new medicine = {}", newMedicine);
        Response response = null;
        service.persistMedicine(newMedicine);
        response = Response.ok(newMedicine).build();
        return response;
    }

    @DELETE
    @RolesAllowed({ADMIN_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response deleteMedicine(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("Deleting a medicine with id = {}", id);
        Response response = null;
        service.deleteMedicineById(id);
        response = Response.ok().build();
        return response;
    }
}