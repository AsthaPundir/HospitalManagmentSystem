
package acmemedical.rest.resource;

import static acmemedical.utility.MyConstants.ADMIN_ROLE;
import static acmemedical.utility.MyConstants.USER_ROLE;
import static acmemedical.utility.MyConstants.MEDICAL_CERTIFICATE_RESOURCE_NAME;
import static acmemedical.utility.MyConstants.RESOURCE_PATH_ID_ELEMENT;
import static acmemedical.utility.MyConstants.RESOURCE_PATH_ID_PATH;
import static acmemedical.utility.MyConstants.*;

import java.util.List;





import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.soteria.WrappingCallerPrincipal;

import acmemedical.ejb.ACMEMedicalService;
import acmemedical.entity.MedicalCertificate;
import acmemedical.entity.SecurityUser;
import acmemedical.entity.Physician;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;


@Path(MEDICAL_CERTIFICATE_RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MedicalCertificateResource {
private static final Logger LOG = LogManager.getLogger();
	
	@EJB
	protected ACMEMedicalService service;
	
	 @Inject
	protected SecurityContext sc;

	 @GET
	 @RolesAllowed({ADMIN_ROLE})
	 public Response getMedicalCertificates() {
		 LOG.debug("retrieving all certificates ...");
		 List<MedicalCertificate> medicalCertificates = service.getMedicalCertificates();
		 Response response = Response.ok(medicalCertificates).build();
		 
		 return response;
	 }
	 
	 @GET
	 @Path(RESOURCE_PATH_ID_PATH)
	 @RolesAllowed({ADMIN_ROLE, USER_ROLE})
	 public Response getMedicalCertificateById (@PathParam(RESOURCE_PATH_ID_ELEMENT) int id) {
		 
		 LOG.debug("try to retrieve specific medicalCertificate " + id);
	        Response response = null;
	        Physician physician = null;
	        
	       	MedicalCertificate medicalCertificate = service.getMedicalCertificateById(id);

	        if (sc.isCallerInRole(ADMIN_ROLE)) {
			 response = Response.status(medicalCertificate == null ? Status.NOT_FOUND : Status.OK).entity(medicalCertificate).build();
	        } else if (sc.isCallerInRole(USER_ROLE)) {
	            WrappingCallerPrincipal wCallerPrincipal = (WrappingCallerPrincipal) sc.getCallerPrincipal();
	            SecurityUser sUser = (SecurityUser) wCallerPrincipal.getWrapped();
	            physician = sUser.getPhysician();
	            if (physician != null && physician.getId() == medicalCertificate.getOwner().getId()) {
	            	 response = Response.status(medicalCertificate == null ? Status.NOT_FOUND : Status.OK).entity(medicalCertificate).build();
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
	 @Path(REST_APPLICATION_PATH + "/physician/{" + PHYSICIAN_ID_RESOURCE_NAME + "}/medicalTraining/{" + TRAINING_ID_RESOURCE_NAME + "}/")
	 public Response addMedicalCertificate(@PathParam("physicianId") int physicianId, @PathParam("trainingId") int trainingID) {
	     LOG.debug("Inside addMedicalCertificate");
	     Response response = null;
	     MedicalCertificate newMedicalCertificate = service.persistMedicalCertificateForPhysician(physicianId, trainingID);
	     if (newMedicalCertificate != null) {
	         response = Response.ok(newMedicalCertificate).build();
	     } else {
	         response = Response.status(Status.NO_CONTENT).build();
	     }
	     return response;
	 }
	 
	 @DELETE
	 @RolesAllowed({ADMIN_ROLE})
	 @Path(RESOURCE_PATH_ID_PATH)
	 public Response deleteMedicalCertificate (@PathParam(RESOURCE_PATH_ID_ELEMENT) int mId) {
		 LOG.debug("Deleting medicalCertificate with id = {}", mId);
		 MedicalCertificate m =service.deleteMedicalCertificateById(mId);
		 Response response = Response.ok(m).build();
		 return response;
	 }
}
