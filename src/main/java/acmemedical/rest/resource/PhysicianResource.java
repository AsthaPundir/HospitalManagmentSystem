package acmemedical.rest.resource;

import static acmemedical.utility.MyConstants.ADMIN_ROLE;
import static acmemedical.utility.MyConstants.PHYSICIAN_RESOURCE_NAME;
import static acmemedical.utility.MyConstants.RESOURCE_PATH_ID_ELEMENT;
import static acmemedical.utility.MyConstants.RESOURCE_PATH_ID_PATH;
import static acmemedical.utility.MyConstants.USER_ROLE;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.soteria.WrappingCallerPrincipal;

import acmemedical.ejb.ACMEMedicalService;
import acmemedical.entity.Physician;
import acmemedical.entity.SecurityUser;

@Path(PHYSICIAN_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PhysicianResource {

    private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMEMedicalService service;

    @Inject
    protected SecurityContext sc;

    @GET
    @RolesAllowed({ADMIN_ROLE})
    public Response getPhysicians() {
        LOG.debug("Attempting to retrieve all physicians...");
        if (!sc.isCallerInRole(ADMIN_ROLE)) {
            throw new ForbiddenException("Access denied: Only Admins can retrieve all physicians.");
        }
        List<Physician> physicians = service.getAllPhysicians();
        return Response.ok(physicians).build();
    }

    @GET
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response getPhysicianById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("Attempting to retrieve physician with ID: {}", id);
        if (sc.isCallerInRole(ADMIN_ROLE)) {
            Physician physician = service.getPhysicianById(id);
            return Response.status(physician == null ? Status.NOT_FOUND : Status.OK).entity(physician).build();
        } else if (sc.isCallerInRole(USER_ROLE)) {
            WrappingCallerPrincipal wCallerPrincipal = (WrappingCallerPrincipal) sc.getCallerPrincipal();
            SecurityUser sUser = (SecurityUser) wCallerPrincipal.getWrapped();
            Physician physician = sUser.getPhysician();
            if (physician != null && physician.getId() == id) {
                return Response.ok(physician).build();
            }
            throw new ForbiddenException("Access denied: User cannot access this physician's details.");
        }
        return Response.status(Status.BAD_REQUEST).build();
    }
    @POST
    @RolesAllowed({ADMIN_ROLE})
    public Response addPhysician(Physician newPhysician) {
        LOG.debug("Adding a new physician: {}", newPhysician);
        Physician createdPhysician = service.persistPhysician(newPhysician);
        return Response.status(Status.CREATED).entity(createdPhysician).build();
    }

    @PUT
    @RolesAllowed({ADMIN_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response updatePhysician(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id, Physician updatedPhysician) {
        LOG.debug("Updating physician with ID: {}", id);
        Physician physician = service.updatePhysicianById(id, updatedPhysician);
        return Response.status(physician == null ? Status.NOT_FOUND : Status.OK).entity(physician).build();
    }

    @DELETE
    @RolesAllowed({ADMIN_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response deletePhysician(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("Deleting physician with ID: {}", id);
        try {
            service.deletePhysicianById(id);
            return Response.status(Status.NO_CONTENT).build(); // No content on successful delete
        } catch (Exception e) {
            LOG.error("Error deleting physician with ID: {}", id, e);
            return Response.status(Status.NOT_FOUND).entity("Physician not found").build();
        }
    }
}
