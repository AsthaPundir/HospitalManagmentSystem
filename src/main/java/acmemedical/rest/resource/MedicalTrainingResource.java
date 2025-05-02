package acmemedical.rest.resource;

import static acmemedical.utility.MyConstants.*;

import java.util.List;


import org.glassfish.soteria.WrappingCallerPrincipal;

import acmemedical.ejb.ACMEMedicalService;
import acmemedical.entity.MedicalTraining;
import acmemedical.entity.Physician;
import acmemedical.entity.SecurityUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import static acmemedical.utility.MyConstants.ADMIN_ROLE;
import static acmemedical.utility.MyConstants.USER_ROLE;
import jakarta.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path(MEDICAL_TRAINING_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MedicalTrainingResource {
	
	private static final Logger LOG = LogManager.getLogger();

    @EJB
    protected ACMEMedicalService service;

    @Inject
    protected SecurityContext sc;


    @GET
    @RolesAllowed({ADMIN_ROLE})
    public Response getMedicalTrainings() {
        LOG.debug("retrieving all trainings ...");
        List<MedicalTraining> physicians = service.getAllMedicalTraining();
        Response response = Response.ok(physicians).build();
        return response;
    }

    @GET
    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response getMedicalTrainingById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("try to retrieve specific training " + id);
        Response response = null;
        Physician physician = null;

        MedicalTraining medicalTraining = service.getMedicalTrainingById(id);

        if (sc.isCallerInRole(ADMIN_ROLE)) {

            response = Response.status(medicalTraining == null ? Status.NOT_FOUND : Status.OK).entity(medicalTraining).build();
        } else if (sc.isCallerInRole(USER_ROLE)) {
            WrappingCallerPrincipal wCallerPrincipal = (WrappingCallerPrincipal) sc.getCallerPrincipal();
            SecurityUser sUser = (SecurityUser) wCallerPrincipal.getWrapped();
            physician = sUser.getPhysician();
            if (physician != null && medicalTraining != null && physician.equals(medicalTraining.getCertificate().getOwner())) {
                response = Response.status(Status.OK).entity(physician).build();
            } else {
                throw new ForbiddenException("User trying to access resource it does not own (wrong userid)");
            }
        } else {
            response = Response.status(Status.BAD_REQUEST).build();
        }
        return response;
    }

    @POST
    @RolesAllowed({ADMIN_ROLE})
    public Response addMedicalTraining(MedicalTraining newMedicalTraining) {
        MedicalTraining newCMWithIdTimestamps = service.persistMedicalTraining(newMedicalTraining);
        return Response.ok(newCMWithIdTimestamps).build();
    }


    @DELETE
    @RolesAllowed({ADMIN_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response deleteMedicalTrainingById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
        LOG.debug("Deleting training with id = {}", id);
        service.deleteMedicalTrainingById(id);
        return Response.ok(id).build();
    }
    @PUT
    @RolesAllowed({ADMIN_ROLE})
    @Path(RESOURCE_PATH_ID_PATH)
    public Response updateMedicalTrainingById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id, MedicalTraining updatedMedicalTraining) {
        LOG.debug("Updating training with id = {}", id);
        MedicalTraining updatedTraining = service.updateMedicalTrainingById(id, updatedMedicalTraining);
        if (updatedTraining != null) {
            return Response.ok(updatedTraining).build(); // Return the updated record
        } else {
            return Response.status(Status.NOT_FOUND).build(); // Record not found
        }
    }

}