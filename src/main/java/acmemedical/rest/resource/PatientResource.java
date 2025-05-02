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
import acmemedical.entity.Medicine;
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

@Path(PATIENT_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PatientResource {

		private static final Logger LOG = LogManager.getLogger();

	    @EJB
	    protected ACMEMedicalService service;

	    @Inject
	    protected SecurityContext sc;

	    @GET
	    @RolesAllowed({ADMIN_ROLE})
	    public Response getPatients() {
	        LOG.debug("Retrieving patient...");
	        List<Patient> patients = service.getAllPatients();
	        Response response = Response.ok(patients).build();
	        return response;
	    }

	    @GET
	    @RolesAllowed({ADMIN_ROLE, USER_ROLE})
	    @Path(RESOURCE_PATH_ID_PATH)
	    public Response getPatientById(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
	        LOG.debug("Retrieving specific patient with id = {}", id);
	        Patient patient = service.getPatientById(id);
	        if (patient == null)
	            return Response.status(Status.NOT_FOUND).build();
	        return Response.ok(patient).build();
	    }

	    @POST
	    @RolesAllowed({ADMIN_ROLE})
	    public Response addPatient(Patient newPatient) {
	        LOG.debug("Adding a new patient = {}", newPatient);
	        Response response = null;
	        service.persistPatient(newPatient);
	        response = Response.ok(newPatient).build();
	        return response;
	    }

	    @DELETE
	    @RolesAllowed({ADMIN_ROLE})
	    @Path(RESOURCE_PATH_ID_PATH)
	    public Response deletePatient(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
	        LOG.debug("Deleting a patient with id = {}", id);
	        Response response = null;
	        service.deletePatientById(id);
	        response = Response.ok().build();
	        return response;
	    }
	    
	    @PUT
	    @RolesAllowed({ADMIN_ROLE})
	    @Path(RESOURCE_PATH_ID_PATH)
	    public Response updatePatient(@PathParam(RESOURCE_PATH_ID_ELEMENT) int id, Patient updatedPatient) {
	        LOG.debug("Updating patient with ID = {}", id);
	        Patient existingPatient = service.updatePatientById(id, updatedPatient);
	        if (existingPatient == null) {
	            return Response.status(Status.NOT_FOUND).entity("Patient not found").build();
	        }
	        return Response.ok(existingPatient).build();
	    }

	}
